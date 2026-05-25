package com.example.check_911.data


import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit


object SurveyReminderScheduler {

    private fun delayUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (target.isBefore(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    fun scheduleMorning(context: Context) {
        val delay = delayUntil(10, 0)
        Log.d("WorkManagerDebug", "⏰ Scheduling MorningSurveysWorker, delay=${delay/1000/60} min")
        val req = PeriodicWorkRequestBuilder<MorningSurveysWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(10, 0), TimeUnit.MILLISECONDS)
            .addTag("morning_surveys")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "morning_surveys",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun scheduleEvening(context: Context) {
        val req = PeriodicWorkRequestBuilder<EveningStatusWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(19, 0), TimeUnit.MILLISECONDS)
            .addTag("evening_status")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "evening_status",
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun scheduleFollowUpOnce(context: Context, attempt: Int) {
        // одноразове нагадування через 30 хв, з лімітом спроб
        val data = workDataOf("attempt" to attempt)
        val req = OneTimeWorkRequestBuilder<FollowUpStatusWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(data)
            .addTag("followup_status")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "followup_status",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    fun cancelFollowUp(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("followup_status")
    }

    fun scheduleAll(context: Context) {
        scheduleMorning(context)
        scheduleEvening(context)
    }
}
