package io.daysleft.app.data.local

import androidx.room.ColumnInfo

data class LunarInfo(
    @ColumnInfo(defaultValue = "0")
    val isLunar: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isLunarWithoutYear: Boolean = false,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null
)
