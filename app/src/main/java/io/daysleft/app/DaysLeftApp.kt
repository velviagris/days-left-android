package io.daysleft.app

import android.app.Application
import io.daysleft.app.data.local.AppDatabase
import io.daysleft.app.data.repository.CountdownRepositoryImpl
import io.daysleft.app.util.AlarmScheduler
import io.daysleft.app.util.CalendarSyncManager
import kotlin.getValue

class DaysLeftApp : Application() {
    // 延迟初始化，确保只有在使用时才创建数据库实例
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CountdownRepositoryImpl(database.countdownEventDao) }

    // 初始化两大管理器
    val alarmScheduler by lazy { AlarmScheduler(this) }
    val calendarSyncManager by lazy { CalendarSyncManager(this) }
}