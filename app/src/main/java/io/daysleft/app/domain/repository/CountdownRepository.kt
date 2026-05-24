package io.daysleft.app.domain.repository

import io.daysleft.app.data.local.CountdownEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 倒数日数据仓库接口。
 * 遵循依赖反转原则，为 ViewModel 提供统一的异步数据访问 API。
 */
interface CountdownRepository {

    /**
     * 获取所有倒数日事件流。
     * Flow 会自动响应数据库的变更并向订阅者发射最新数据流。
     */
    fun getAllEvents(): Flow<List<CountdownEventEntity>>

    /**
     * 根据主键 ID 查询单一倒数日事件。
     * @param id 事件的唯一标识符
     * @return 如果找不到则返回 null
     */
    suspend fun getEventById(id: Long): CountdownEventEntity?

    /**
     * 插入新的倒数日事件。
     * @param event 要插入的实体对象
     * @return 插入成功后生成的主键 ID
     */
    suspend fun insertEvent(event: CountdownEventEntity): Long

    /**
     * 更新已有的倒数日事件。
     * @param event 包含最新数据的实体对象（需要包含原有 ID）
     */
    suspend fun updateEvent(event: CountdownEventEntity)

    /**
     * 删除指定的倒数日事件。
     * @param event 要删除的实体对象
     */
    suspend fun deleteEvent(event: CountdownEventEntity)

    suspend fun insertEvents(events: List<CountdownEventEntity>)
}