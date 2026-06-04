package io.daysleft.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "countdown_events",
    indices = [
        Index(value = ["targetDate"]),
        Index(value = ["calendarEventId"])
    ]
)
data class CountdownEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetDate: LocalDate,
    
    @Embedded
    val lunarInfo: LunarInfo = LunarInfo(),
    
    val repeatInterval: RepeatInterval?,
    
    val notifyDaysInAdvance: Int,
    
    @ColumnInfo(defaultValue = "9")
    val notifyTimeHour: Int = 9,
    
    @ColumnInfo(defaultValue = "0")
    val notifyTimeMinute: Int = 0,
    
    val syncToSystemCalendar: Boolean,
    
    @ColumnInfo(defaultValue = "0")
    val useCalendarNotification: Boolean = false,
    
    val calendarEventId: Long?,

    @ColumnInfo(defaultValue = "0")
    val isWithoutYear: Boolean = false
) {
    val isRepeatEnabled: Boolean
        @Ignore get() = repeatInterval != null

    val isLunar: Boolean
        @Ignore get() = lunarInfo.isLunar

    val isLunarWithoutYear: Boolean
        @Ignore get() = lunarInfo.isLunarWithoutYear

    val lunarMonth: Int?
        @Ignore get() = lunarInfo.lunarMonth

    val lunarDay: Int?
        @Ignore get() = lunarInfo.lunarDay
}