package io.materialdaysleft.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import io.materialdaysleft.app.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "countdown_reminders"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DAYS_LEFT = "extra_days_left"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "您的倒数日"
        val daysLeft = intent.getIntExtra(EXTRA_DAYS_LEFT, 0)

        if (eventId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "倒数日提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于发送即将到来的倒数日通知"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 构建通知文案
        val contentText = if (daysLeft == 0) {
            "就是今天！"
        } else {
            "距离目标还有 $daysLeft 天"
        }

        // 构建并发送通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 替换为您的应用图标
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // 使用 eventId 作为通知的 unique ID，防止被相互覆盖
        notificationManager.notify(eventId.toInt(), notification)
    }
}