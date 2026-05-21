package io.materialdaysleft.app.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import io.materialdaysleft.app.data.local.CountdownEventEntity
import java.time.ZoneId
import java.util.TimeZone

class CalendarSyncManager(private val context: Context) {

    /**
     * 将倒数日事件同步到系统日历 (全天事件)
     * @return 返回系统日历中生成的 Event ID，如果失败或无权限则返回 null
     */
    fun syncToCalendar(event: CountdownEventEntity): Long? {
        // 检查是否有写入日历的权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val calId = getPrimaryCalendarId() ?: return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, "倒数日: ${event.title}")
            put(CalendarContract.Events.DESCRIPTION, "由 Material DaysLeft 自动生成")
            put(CalendarContract.Events.CALENDAR_ID, calId)

            // 恢复为全天事件
            val startMillis = event.targetDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            val endMillis = startMillis + 24 * 60 * 60 * 1000
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            put(CalendarContract.Events.ALL_DAY, 1)
        }

        // 插入事件并获取 URI
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = uri?.lastPathSegment?.toLongOrNull()

        // 如果开启了日历通知，则添加系统默认的提醒
        if (eventId != null && event.useCalendarNotification) {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.MINUTES, 0) // 准时（对于全天事件通常是当天 0 点或系统设定值）
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
        }

        return eventId
    }

    /**
     * 从系统日历中删除事件
     */
    fun deleteFromCalendar(calendarEventId: Long) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
        context.contentResolver.delete(deleteUri, null, null)
    }

    /**
     * 获取系统默认/主日历的 ID
     */
    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        // 优先查找标记为 PRIMARY 的日历账户
        val selection = "${CalendarContract.Calendars.IS_PRIMARY} = 1"

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }

        // 如果找不到主日历，就退而求其次拿第一个可用的日历
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }
}