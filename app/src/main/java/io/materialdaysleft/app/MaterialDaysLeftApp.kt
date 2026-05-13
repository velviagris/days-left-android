package io.materialdaysleft.app

import android.app.Application
import io.materialdaysleft.app.data.local.AppDatabase
import io.materialdaysleft.app.data.repository.CountdownRepositoryImpl
import io.materialdaysleft.app.util.AlarmScheduler
import io.materialdaysleft.app.util.CalendarSyncManager
import kotlin.getValue

class MaterialDaysLeftApp : Application() {
    // 延迟初始化，确保只有在使用时才创建数据库实例
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CountdownRepositoryImpl(database.countdownEventDao) }

    // 初始化两大管理器
    val alarmScheduler by lazy { AlarmScheduler(this) }
    val calendarSyncManager by lazy { CalendarSyncManager(this) }
}