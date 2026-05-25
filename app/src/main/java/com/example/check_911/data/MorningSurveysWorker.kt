package com.example.check_911.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.example.check_911.App
import com.example.check_911.MainActivity
import java.time.DayOfWeek
import java.time.LocalDate

class MorningSurveysWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val app = ctx.applicationContext as App
    private val helper = NotificationHelper(ctx)

    override suspend fun doWork(): Result {
        Log.d("WorkManagerDebug", "🌅 MorningSurveysWorker запущен")

        val visible = app.database.surveyDao().getVisibleSurveys()
        if (visible.isEmpty()) return Result.success()
        Log.d("WorkManagerDebug", "📋 Visible surveys: ${visible.size}")

        val today = LocalDate.now()
        val hasDaily = visible.any { it.periodDescription.contains("щод", true) || it.periodDescription.contains("ежеднев", true) }
        val isMonday = today.dayOfWeek == DayOfWeek.MONDAY
        val isFirstDay = today.dayOfMonth == 1

        val hasWeekly = visible.any { it.periodDescription.contains("щотиж", true) || it.periodDescription.contains("еженедел", true) }
        val hasMonthly = visible.any { it.periodDescription.contains("щомісяч", true) || it.periodDescription.contains("ежемесяч", true) }
        Log.d("WorkManagerDebug", "hasDaily=$hasDaily, hasWeekly=$hasWeekly, hasMonthly=$hasMonthly")

        // Щоденні
        if (hasDaily) {
            helper.notify(
                NotificationHelper.CH_REMINDERS,
                NotificationHelper.ID_MORNING,
                "Нові опитування",
                "Доступні щоденні опитування. Пройдіть їх сьогодні.",
                openMainPendingIntent()
            )
            Log.d("WorkManagerDebug", "✅ Отправлено уведомление: daily")
        }
        // Щотижневі по понеділках
        if (hasWeekly && isMonday) {
            helper.notify(
                NotificationHelper.CH_REMINDERS,
                NotificationHelper.ID_WEEKLY,
                "Щотижневі опитування",
                "Доступні щотижневі опитування.",
                openMainPendingIntent()
            )
            Log.d("WorkManagerDebug", "✅ Отправлено уведомление: weekly")
        }

        // Щомісячні 1-го числа
        if (hasMonthly && isFirstDay) {
            helper.notify(
                NotificationHelper.CH_REMINDERS,
                NotificationHelper.ID_MONTHLY,
                "Щомісячні опитування",
                "Доступні щомісячні опитування.",
                openMainPendingIntent()
            )
            Log.d("WorkManagerDebug", "✅ Отправлено уведомление: monthly")
        }

        return Result.success()
    }

    private fun openMainPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }
}
