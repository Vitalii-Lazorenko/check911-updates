package com.example.check_911.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters



class TestNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WorkManagerDebug", "⚡ TestNotificationWorker запущен")

        // Создаём канал (для Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "survey_channel",
                "Survey Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Проверка разрешения (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                Log.w("WorkManagerDebug", "⚠️ Немає дозволу POST_NOTIFICATIONS, повідомлення не буде показане")
                return Result.success()
            }
        }

        // Само уведомление
        val notification = NotificationCompat.Builder(applicationContext, "survey_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Тест WorkManager")
            .setContentText("Це повідомлення від WorkManager")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(1234, notification)

        return Result.success()
    }

}
