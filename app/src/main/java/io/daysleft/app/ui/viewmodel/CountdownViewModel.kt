package io.daysleft.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.domain.repository.CountdownRepository
import io.daysleft.app.util.AlarmScheduler
import io.daysleft.app.util.CalendarSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CountdownViewModel(
    private val repository: CountdownRepository, // 注意：这里是 private，对外隐藏数据源细节
    private val alarmScheduler: AlarmScheduler,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModel() {

    // 获取所有事件的状态流，供 HomeScreen 使用
    val allEvents: StateFlow<List<CountdownEventEntity>> = repository.getAllEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 当没有观察者时延迟 5s 停止采集，节省资源
            initialValue = emptyList()
        )

    /**
     * 根据 ID 获取单个倒数日事件。
     * 这是一个挂起函数 (suspend function)，供 UI 层 (如 EditScreen 的 LaunchedEffect) 在协程中调用。
     * 使用 withContext(Dispatchers.IO) 确保数据库查询在后台线程执行，不阻塞主线程。
     */
    suspend fun getEventById(id: Long): CountdownEventEntity? {
        return withContext(Dispatchers.IO) {
            repository.getEventById(id)
        }
    }

    /**
     * 插入新的倒数日事件
     */
    fun insertEvent(event: CountdownEventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 如果需要同步日历，执行同步并获取返回的日历ID
            val finalEvent = if (event.syncToSystemCalendar) {
                val calId = calendarSyncManager.syncToCalendar(event)
                event.copy(calendarEventId = calId)
            } else event

            // 2. 插入数据库获取主键
            val newId = repository.insertEvent(finalEvent)

            // 3. 设定系统精准闹钟
            alarmScheduler.scheduleAlarm(finalEvent.copy(id = newId))
        }
    }

    /**
     * 更新已有的倒数日事件
     */
    fun updateEvent(event: CountdownEventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 处理日历逻辑：先查出旧数据看看之前的状态
            val oldEvent = repository.getEventById(event.id)

            // 简单粗暴且安全的日历更新策略：先删旧的，再建新的
            oldEvent?.calendarEventId?.let { calendarSyncManager.deleteFromCalendar(it) }

            val finalEvent = if (event.syncToSystemCalendar) {
                val calId = calendarSyncManager.syncToCalendar(event)
                event.copy(calendarEventId = calId)
            } else {
                event.copy(calendarEventId = null)
            }

            // 更新数据库并重设闹钟
            repository.updateEvent(finalEvent)
            alarmScheduler.scheduleAlarm(finalEvent)
        }
    }

    /**
     * 删除指定的倒数日事件
     */
    fun deleteEvent(event: CountdownEventEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. 从日历中删除
            event.calendarEventId?.let { calendarSyncManager.deleteFromCalendar(it) }
            // 2. 取消闹钟
            alarmScheduler.cancelAlarm(event)
            // 3. 从数据库删除
            repository.deleteEvent(event)
        }
    }

    /**
     * 批量插入倒数日事件（用于数据恢复）
     */
    fun insertMultipleEvents(events: List<CountdownEventEntity>, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertEvents(events)
            // 遍历恢复所有闹钟
            events.forEach { alarmScheduler.scheduleAlarm(it) }
            withContext(Dispatchers.Main) { onComplete() }
        }
    }
}

// ViewModel Factory 保持不变
class CountdownViewModelFactory(
    private val repository: CountdownRepository,
    private val alarmScheduler: AlarmScheduler,
    private val calendarSyncManager: CalendarSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CountdownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CountdownViewModel(repository, alarmScheduler, calendarSyncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}