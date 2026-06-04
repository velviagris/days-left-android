package io.daysleft.app.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import io.daysleft.app.R
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.data.local.RepeatInterval
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class CalendarSyncManager(private val context: Context) {

    /**
     * 将倒数日事件同步到系统日历 (全天事件)
     * @return 返回系统日历中生成的 Event ID，如果失败或无权限则返回 null
     */
    fun syncToCalendar(event: CountdownEventEntity, forceNextOccurrence: Boolean = false): Long? {
        // 检查是否有写入日历的权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val calId = getPrimaryCalendarId() ?: return null

        val baseDate = if (event.isLunar) {
            if (forceNextOccurrence) {
                DateUtils.calculateNextOccurrence(event, LocalDate.now().plusDays(1))
            } else {
                DateUtils.calculateNextOccurrence(event)
            }
        } else {
            event.targetDate
        }

        val startMillis = baseDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_prefix, event.title))
            put(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_event_desc))
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            put(CalendarContract.Events.ALL_DAY, 1)

            // 如果是公历重复事件，写入 RRULE，且必须用 DURATION 代替 DTEND
            val isRecurringSolar = event.isRepeatEnabled && !event.isLunar
            if (isRecurringSolar) {
                val rrule = when (event.repeatInterval) {
                    RepeatInterval.DAILY -> "FREQ=DAILY"
                    RepeatInterval.WEEKLY -> "FREQ=WEEKLY"
                    RepeatInterval.MONTHLY -> "FREQ=MONTHLY"
                    RepeatInterval.YEARLY -> "FREQ=YEARLY"
                    null -> null
                }
                rrule?.let {
                    put(CalendarContract.Events.RRULE, it)
                    put(CalendarContract.Events.DURATION, "P1D") // 全天事件持续时间为 1 天 (P1D)
                }
            } else {
                // 非重复事件或农历事件（农历目前不支持系统自带的 RRULE），使用具体的 DTEND
                val endMillis = startMillis + 24 * 60 * 60 * 1000
                put(CalendarContract.Events.DTEND, endMillis)
            }
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