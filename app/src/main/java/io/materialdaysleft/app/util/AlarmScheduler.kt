package io.materialdaysleft.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.materialdaysleft.app.data.local.CountdownEventEntity
import io.materialdaysleft.app.receiver.NotificationReceiver
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为倒数日设定精确的系统唤醒提醒
     */
    fun scheduleAlarm(event: CountdownEventEntity) {
        cancelAlarm(event) // 设定前先清除旧的

        val targetDate = DateUtils.calculateNextOccurrence(event)
        val notifyDate = targetDate.minusDays(event.notifyDaysInAdvance.toLong())

        var triggerTime = notifyDate.atTime(event.notifyTimeHour, event.notifyTimeMinute)
            .atZone(ZoneId.systemDefault())

        // 【关键修复】：如果算出的触发时间已经过去（例如今天9点已过），且是重复事件，则顺延到下一个周期
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
            putExtra(NotificationReceiver.EXTRA_DAYS_LEFT, event.notifyDaysInAdvance)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
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
     * 取消已存在的闹钟
     */
    fun cancelAlarm(event: CountdownEventEntity) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}