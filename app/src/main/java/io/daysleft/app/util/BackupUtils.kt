package io.daysleft.app.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.data.local.LunarInfo
import io.daysleft.app.data.local.RepeatInterval
import java.time.LocalDate

object BackupUtils {
    /**
     * 配置了 LocalDate 解析器与向后兼容 CountdownEventEntity 的 Gson 实例
     */
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) }
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            JsonDeserializer { json, _, _ -> LocalDate.parse(json.asString) }
        )
        .registerTypeAdapter(
            CountdownEventEntity::class.java,
            JsonDeserializer { json, _, context ->
                val obj = json.asJsonObject
                
                val id = obj.get("id")?.asLong ?: 0L
                val title = obj.get("title")?.asString ?: ""
                val targetDate = context.deserialize<LocalDate>(obj.get("targetDate"), LocalDate::class.java)
                
                val repeatIntervalStr = obj.get("repeatInterval")?.asString
                val repeatInterval = repeatIntervalStr?.let {
                    try {
                        RepeatInterval.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val lunarInfo = if (obj.has("lunarInfo")) {
                    context.deserialize<LunarInfo>(obj.get("lunarInfo"), LunarInfo::class.java)
                } else {
                    val isLunar = obj.get("isLunar")?.asBoolean ?: false
                    val isLunarWithoutYear = obj.get("isLunarWithoutYear")?.asBoolean ?: false
                    val lunarMonth = obj.get("lunarMonth")?.asInt
                    val lunarDay = obj.get("lunarDay")?.asInt
                    LunarInfo(isLunar, isLunarWithoutYear, lunarMonth, lunarDay)
                }
                
                val notifyDaysInAdvance = obj.get("notifyDaysInAdvance")?.asInt ?: 0
                val notifyTimeHour = obj.get("notifyTimeHour")?.asInt ?: 9
                val notifyTimeMinute = obj.get("notifyTimeMinute")?.asInt ?: 0
                val syncToSystemCalendar = obj.get("syncToSystemCalendar")?.asBoolean ?: false
                val useCalendarNotification = obj.get("useCalendarNotification")?.asBoolean ?: false
                val calendarEventId = obj.get("calendarEventId")?.asLong
                val isWithoutYear = obj.get("isWithoutYear")?.asBoolean ?: lunarInfo.isLunarWithoutYear
                
                CountdownEventEntity(
                    id = id,
                    title = title,
                    targetDate = targetDate,
                    lunarInfo = lunarInfo,
                    repeatInterval = repeatInterval,
                    notifyDaysInAdvance = notifyDaysInAdvance,
                    notifyTimeHour = notifyTimeHour,
                    notifyTimeMinute = notifyTimeMinute,
                    syncToSystemCalendar = syncToSystemCalendar,
                    useCalendarNotification = useCalendarNotification,
                    calendarEventId = calendarEventId,
                    isWithoutYear = isWithoutYear
                )
            }
        )
        .create()
}