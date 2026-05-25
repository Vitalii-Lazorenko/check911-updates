package com.example.check_911.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.example.check_911.App
import com.example.check_911.MainActivity

class EveningStatusWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val app = ctx.applicationContext as App
    private val helper = NotificationHelper(ctx)

    override suspend fun doWork(): Result {
        val drafts = app.database.resultsSurveyDao().countDraft()
        val ready = app.database.resultsSurveyDao().countReady()

        var needFollowUp = false

        if (drafts > 0) {
            helper.notify(
                NotificationHelper.CH_STATUS,
                NotificationHelper.ID_INCOMPLETE,
                "Незавершені опитування",
                "У вас є незавершені опитування. Завершіть їх, будь ласка.",
                openIncompletePending()
            )
            needFollowUp = true
        }

        if (ready > 0) {
            helper.notify(
                NotificationHelper.CH_STATUS,
                NotificationHelper.ID_READY_TO_SEND,
                "Готові до відправлення",
                "Є завершені опитування, але вони ще не відправлені.",
                openReadyPending()
            )
            needFollowUp = true
        }

        if (needFollowUp) {
            // перший фоллоу-ап через 30 хв, спроба №1
            SurveyReminderScheduler.scheduleFollowUpOnce(applicationContext, attempt = 1)
        } else {
            // все чисто — скасуємо можливий попередній follow-up
            SurveyReminderScheduler.cancelFollowUp(applicationContext)
        }

        return Result.success()
    }

    private fun openIncompletePending(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra("openTab", "incomplete") // за бажанням: навігація на конкретний екран/фільтр
        return PendingIntent.getActivity(
            applicationContext, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }

    private fun openReadyPending(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra("openTab", "ready")
        return PendingIntent.getActivity(
            applicationContext, 102, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )
    }
}
