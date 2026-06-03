package io.daysleft.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromRepeatInterval(value: String?): RepeatInterval? {
        return value?.let {
            try {
                RepeatInterval.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun repeatIntervalToString(interval: RepeatInterval?): String? {
        return interval?.name
    }
}