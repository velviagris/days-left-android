package io.daysleft.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 核心数据库类，采用单例模式管理数据库连接。
 */
@Database(
    entities = [CountdownEventEntity::class],
    version = 6,
    exportSchema = false // 在生产环境中，建议开启 schema 导出以便追踪数据库版本变更
)
@TypeConverters(Converters::class) // 注册我们之前编写的类型转换器
abstract class AppDatabase : RoomDatabase() {

    // 暴露数据访问对象 (DAO)
    abstract val countdownEventDao: CountdownEventDao

    companion object {
        // @Volatile 保证多线程环境下的内存可见性，防止指令重排
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库单例。
         * 采用双重检查锁定 (Double-Checked Locking) 确保线程安全。
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daysleft_db"
                )
                    // 在开发初期，当 schema 发生改变时，采取破坏性迁移（清空数据）
                    // 生产环境需替换为 addMigrations()
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}