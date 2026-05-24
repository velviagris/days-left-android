package io.daysleft.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.receiver.NotificationReceiver
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为倒数日设定精确的系统唤醒提醒
     * 支持同时设定“提前提醒”和“当日提醒”
     */
    fun scheduleAlarm(event: CountdownEventEntity) {
        cancelAlarm(event) // 设定前先清除旧的

        val targetDate = DateUtils.calculateNextOccurrence(event)

        // 1. 设定“提前提醒”闹钟 (使用原始 ID 作为 requestCode)
        if (event.notifyDaysInAdvance > 0) {
            val notifyDate = targetDate.minusDays(event.notifyDaysInAdvance.toLong())
            scheduleSingleAlarm(event, notifyDate, event.notifyDaysInAdvance, event.id.toInt())
        }

        // 2. 设定“当日提醒”闹钟 (使用 ID + 100000 作为 requestCode 以示区分)
        scheduleSingleAlarm(event, targetDate, 0, event.id.toInt() + 100000)
    }

    private fun scheduleSingleAlarm(event: CountdownEventEntity, notifyDate: LocalDate, daysLeft: Int, requestCode: Int) {
        var triggerTime = notifyDate.atTime(event.notifyTimeHour, event.notifyTimeMinute)
            .atZone(ZoneId.systemDefault())

        // 如果算出的触发时间已经过去，且是重复事件，则顺延到下一个周期
        if (triggerTime.isBefore(ZonedDateTime.now())) {
            if (event.isRepeatEnabled) {
                when (event.repeatInterval) {
                    "DAILY" -> triggerTime = triggerTime.plusDays(1)
                    "WEEKLY" -> triggerTime = triggerTime.plusWeeks(1)
                    "MONTHLY" -> triggerTime = triggerTime.plusMonths(1)
                    "YEARLY" -> triggerTime = triggerTime.plusYears(1)
                }
            } else {
                return // 非重复事件，过去就直接抛弃
            }
        }

        val triggerTimeMillis = triggerTime.toInstant().toEpochMilli()

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(NotificationReceiver.EXTRA_TITLE, event.title)
            putExtra(NotificationReceiver.EXTRA_DAYS_LEFT, daysLeft)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * 取消已存在的闹钟（包括提前提醒和当日提醒）
     */
    fun cancelAlarm(event: CountdownEventEntity) {
        val intent = Intent(context, NotificationReceiver::class.java)
        
        // 取消提前提醒
        val pendingIntentAdvance = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentAdvance)

        // 取消当日提醒
        val pendingIntentToday = PendingIntent.getBroadcast(
            context,
            event.id.toInt() + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntentToday)
    }
}
