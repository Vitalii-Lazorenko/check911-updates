package com.example.check_911.data

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SurveyReminderScheduler.scheduleAll(context)
        }
    }
}
