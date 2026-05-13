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

        // 计算目标提醒日期：目标日期 - 提前通知的天数
        val targetDate = DateUtils.calculateNextOccurrence(event)
        val notifyDate = targetDate.minusDays(event.notifyDaysInAdvance.toLong())

        // 结合用户设定的具体时间（小时和分钟）
        val triggerTime = notifyDate.atTime(event.notifyTimeHour, event.notifyTimeMinute)
            .atZone(ZoneId.systemDefault())

        // 如果计算出的精确提醒时间已经过去，就不再设闹钟了
        if (triggerTime.isBefore(ZonedDateTime.now())) return

        val triggerTimeMillis = triggerTime.toInstant().toEpochMilli()

        // 准备传递给 Receiver 的数据
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(NotificationReceiver.EXTRA_TITLE, event.title)
            putExtra(NotificationReceiver.EXTRA_DAYS_LEFT, event.notifyDaysInAdvance)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(), // 使用 event.id 作为 RequestCode，确保可以唯一标识和取消
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 使用精准闹钟，允许在打盹模式(Doze)下唤醒
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Android 14+ 如果用户手动在系统设置剥夺了闹钟权限会抛出异常，这里安全捕获
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