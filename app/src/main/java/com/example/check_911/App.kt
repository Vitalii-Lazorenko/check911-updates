package com.example.check_911

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.check_911.data.NotificationHelper
import com.example.check_911.data.SurveyReminderScheduler
//import com.example.check_911.data.SurveyReminderScheduler
import com.example.check_911.data.UpdateChecker
import com.example.check_911.data.db.MainDb
import com.example.check_911.data.utils.AppLogger
import com.example.check_911.data.utils.TasksSyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit

class App : Application() {

    lateinit var database: MainDb
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            MainDb::class.java,
            "survey_db"
        ).fallbackToDestructiveMigration().build()

        instance = this

        // Запуск Koin
        startKoin {
            androidContext(this@App)
            modules(listOf(networkModule, dbModule, repositoryModule, viewModelModule))
        }

        NotificationHelper(this).ensureChannels()
        SurveyReminderScheduler.scheduleAll(this)

        scheduleTasksSync()



//        // При завершении загрузки APK (для установки обновлений)
//        val receiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
//                    Log.d("UpdateManager", "Завантаження завершено")
//                    Toast.makeText(context, "Оновлення готове до встановлення", Toast.LENGTH_LONG).show()
//                    context?.let { UpdateChecker.installSavedApk(it) }
//                }
//            }
//        }
//
//        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val logText = buildString {
                append("=== CRASH DETECTED ===\n")
                append("Thread: ${thread.name}\n")
                append("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n")
                append("Message: ${throwable.message}\n")
                append("Stacktrace:\n")
                append(Log.getStackTraceString(throwable))
            }

            // Пишем в AppLogger
            AppLogger.log("CRASH", logText, applicationContext)

//            // Можно ещё отправить в Telegram бота
//            try {
//                // sendLogToTelegram(logText) // твоя реализация
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//            // Завершаем приложение (чтобы поведение было как при обычном краше)
//            android.os.Process.killProcess(android.os.Process.myPid())
//            exitProcess(1)


        }
    }

    companion object {
        private lateinit var instance: App
        fun getAppContext(): Context = instance.applicationContext
    }

    private fun scheduleTasksSync() {

        val workRequest = PeriodicWorkRequestBuilder<TasksSyncWorker>(
//            1, TimeUnit.HOURS // 🔥 каждые 1 час
            30, TimeUnit.MINUTES // 🔥 каждые 15 minuts
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // только при интернете
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "tasks_sync",
            ExistingPeriodicWorkPolicy.UPDATE, // обновлять если уже есть
            workRequest
        )
    }

}
