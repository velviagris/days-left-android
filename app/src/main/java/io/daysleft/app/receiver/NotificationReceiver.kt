package io.daysleft.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.daysleft.app.R
import io.daysleft.app.data.local.AppDatabase
import io.daysleft.app.util.AlarmScheduler
import io.daysleft.app.util.CalendarSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "countdown_reminders"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DAYS_LEFT = "extra_days_left"

        /**
         * 发送一个测试通知
         */
        fun sendTestNotification(context: Context) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.test_notification_title))
                .setContentText(context.getString(R.string.test_notification_content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(999, notification)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.notification_default_title)
        val daysLeft = intent.getIntExtra(EXTRA_DAYS_LEFT, 0)

        if (eventId == -1L) return

        // 1. 发送通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = if (daysLeft == 0) context.getString(R.string.notification_today) 
                          else context.getString(R.string.notification_days_left, daysLeft)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(eventId.toInt(), notification)

        // 2. 【修复核心】：闭环机制，安排下一次提醒
        // 使用 goAsync() 告诉系统该广播还需要在后台做些耗时操作（查询数据库）
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val event = db.countdownEventDao.getEventById(eventId)

                // 如果事件存在且开启了重复，立刻为它安排下一个周期的闹钟
                if (event != null && event.isRepeatEnabled) {
                    val alarmScheduler = AlarmScheduler(context)
                    alarmScheduler.scheduleAlarm(event)

                    // 如果是农历重复事件，且开启了系统日历同步，并且是“当日提醒”（daysLeft == 0），
                    // 则自动更新系统日历至下一个发生的日期，并将新的 calendarEventId 写入数据库。
                    if (event.syncToSystemCalendar && event.isLunar && daysLeft == 0) {
                        val calendarSyncManager = CalendarSyncManager(context)
                        // 1. 从系统日历中删除旧事件
                        event.calendarEventId?.let { calendarSyncManager.deleteFromCalendar(it) }
                        // 2. 创建下一年发生的新事件并写入
                        val newCalId = calendarSyncManager.syncToCalendar(event, forceNextOccurrence = true)
                        // 3. 更新数据库中对应的 calendarEventId
                        db.countdownEventDao.updateEvent(event.copy(calendarEventId = newCalId))
                    }
                }
            } finally {
                // 必须调用 finish 释放系统资源
                pendingResult.finish()
            }
        }
    }
}