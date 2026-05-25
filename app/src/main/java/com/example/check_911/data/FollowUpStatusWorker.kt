package com.example.check_911.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.example.check_911.App
import com.example.check_911.MainActivity

class FollowUpStatusWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val app = ctx.applicationContext as App
    private val helper = NotificationHelper(ctx)

    override suspend fun doWork(): Result {
        val attempt = inputData.getInt("attempt", 1)
        val drafts = app.database.resultsSurveyDao().countDraft()
        val ready = app.database.resultsSurveyDao().countReady()

        val stillNeeds = (drafts > 0) || (ready > 0)

        if (stillNeeds) {
            if (drafts > 0) {
                helper.notify(
                    NotificationHelper.CH_STATUS,
                    NotificationHelper.ID_INCOMPLETE,
                    "Нагадування",
                    "Незавершені опитування ще очікують на вас.",
                    openIncompletePending()
                )
            }
            if (ready > 0) {
                helper.notify(
                    NotificationHelper.CH_STATUS,
                    NotificationHelper.ID_READY_TO_SEND,
                    "Нагадування",
                    "Є опитування, готові до відправлення.",
                    openReadyPending()
                )
            }

            if (attempt < 4) { // максимум 4 повтори
                SurveyReminderScheduler.scheduleFollowUpOnce(applicationContext, attempt + 1)
            } else {
                // ліміт вичерпано — зупиняємося
                SurveyReminderScheduler.cancelFollowUp(applicationContext)
            }
        } else {
            // все закрито / відправлено — скасовуємо повтори
            SurveyReminderScheduler.cancelFollowUp(applicationContext)
        }

        return Result.success()
    }

    private fun openIncompletePending(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).putExtra("openTab", "incomplete")
        return PendingIntent.getActivity(
            applicationContext, 201, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }

    private fun openReadyPending(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).putExtra("openTab", "ready")
        return PendingIntent.getActivity(
            applicationContext, 202, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }
}
