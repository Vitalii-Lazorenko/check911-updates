package com.example.check_911.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.check_911.R


class NotificationHelper(private val ctx: Context) {

    companion object {
        const val CH_REMINDERS = "survey_reminders"
        const val CH_STATUS = "survey_status"

        const val ID_MORNING = 1001
        const val ID_WEEKLY = 1002
        const val ID_MONTHLY = 1003
        const val ID_INCOMPLETE = 2001
        const val ID_READY_TO_SEND = 2002
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val rem = NotificationChannel(CH_REMINDERS, "Нагадування про опитування", NotificationManager.IMPORTANCE_DEFAULT)
            val stat = NotificationChannel(CH_STATUS, "Статус опитувань", NotificationManager.IMPORTANCE_HIGH)
            mgr.createNotificationChannel(rem)
            mgr.createNotificationChannel(stat)
        }
    }

    fun notify(
        channelId: String,
        notifId: Int,
        title: String,
        text: String,
        pendingIntent: PendingIntent? = null
    ) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val b = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable._start)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        pendingIntent?.let { b.setContentIntent(it) }
        mgr.notify(notifId, b.build())
    }
}
