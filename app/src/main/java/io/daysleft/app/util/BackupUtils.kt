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
                
                val isObfuscated = obj.has("b") && !obj.has("title")
                
                val id = if (isObfuscated) {
                    obj.get("a")?.asLong ?: 0L
                } else {
                    obj.get("id")?.asLong ?: 0L
                }
                
                val title = if (isObfuscated) {
                    obj.get("b")?.asString ?: ""
                } else {
                    obj.get("title")?.asString ?: ""
                }
                
                val targetDateElement = if (isObfuscated) obj.get("c") else obj.get("targetDate")
                val targetDate = context.deserialize<LocalDate>(targetDateElement, LocalDate::class.java)
                
                val repeatIntervalStr = if (isObfuscated) {
                    obj.get("e")?.asString
                } else {
                    obj.get("repeatInterval")?.asString
                }
                val repeatInterval = repeatIntervalStr?.let {
                    try {
                        RepeatInterval.valueOf(it)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                val lunarInfo = if (isObfuscated) {
                    val lunarObj = obj.getAsJsonObject("d")
                    if (lunarObj != null) {
                        val isLunar = lunarObj.get("a")?.asBoolean ?: false
                        val isLunarWithoutYear = lunarObj.get("b")?.asBoolean ?: false
                        val lunarMonth = lunarObj.get("c")?.asInt
                        val lunarDay = lunarObj.get("d")?.asInt
                        LunarInfo(isLunar, isLunarWithoutYear, lunarMonth, lunarDay)
                    } else {
                        LunarInfo()
                    }
                } else {
                    if (obj.has("lunarInfo")) {
                        context.deserialize<LunarInfo>(obj.get("lunarInfo"), LunarInfo::class.java)
                    } else {
                        val isLunar = obj.get("isLunar")?.asBoolean ?: false
                        val isLunarWithoutYear = obj.get("isLunarWithoutYear")?.asBoolean ?: false
                        val lunarMonth = obj.get("lunarMonth")?.asInt
                        val lunarDay = obj.get("lunarDay")?.asInt
                        LunarInfo(isLunar, isLunarWithoutYear, lunarMonth, lunarDay)
                    }
                }
                
                val notifyDaysInAdvance = if (isObfuscated) {
                    obj.get("f")?.asInt ?: 0
                } else {
                    obj.get("notifyDaysInAdvance")?.asInt ?: 0
                }
                
                val notifyTimeHour = if (isObfuscated) {
                    obj.get("g")?.asInt ?: 9
                } else {
                    obj.get("notifyTimeHour")?.asInt ?: 9
                }
                
                val notifyTimeMinute = if (isObfuscated) {
                    obj.get("h")?.asInt ?: 0
                } else {
                    obj.get("notifyTimeMinute")?.asInt ?: 0
                }
                
                val syncToSystemCalendar = if (isObfuscated) {
                    obj.get("i")?.asBoolean ?: false
                } else {
                    obj.get("syncToSystemCalendar")?.asBoolean ?: false
                }
                
                val useCalendarNotification = if (isObfuscated) {
                    obj.get("j")?.asBoolean ?: false
                } else {
                    obj.get("useCalendarNotification")?.asBoolean ?: false
                }
                
                val calendarEventId = if (isObfuscated) {
                    obj.get("k")?.asLong
                } else {
                    obj.get("calendarEventId")?.asLong
                }
                
                val isWithoutYear = if (isObfuscated) {
                    obj.get("l")?.asBoolean ?: lunarInfo.isLunarWithoutYear
                } else {
                    obj.get("isWithoutYear")?.asBoolean ?: lunarInfo.isLunarWithoutYear
                }
                
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