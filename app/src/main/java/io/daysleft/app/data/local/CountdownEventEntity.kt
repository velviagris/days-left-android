package io.daysleft.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "countdown_events")
data class CountdownEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetDate: LocalDate,
    val isLunar: Boolean,
    val isLunarWithoutYear: Boolean = false,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val isRepeatEnabled: Boolean,
    val repeatInterval: String?,
    val notifyDaysInAdvance: Int,
    val notifyTimeHour: Int = 9,       // 通知时间（小时），默认上午 9 点
    val notifyTimeMinute: Int = 0,     // 通知时间（分钟），默认 0 分
    val syncToSystemCalendar: Boolean,
    val useCalendarNotification: Boolean = false, // 新增：是否使用系统日历通知
    val calendarEventId: Long?
)