package com.example.check_911.data.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.check_911.ApiServiceData
import com.example.check_911.App
import com.example.check_911.NetWorkProvider
import com.example.check_911.data.db.repository.InstructionTaskRepository
import com.example.check_911.data.db.repository.TaskRepository

class TasksSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val database = (context.applicationContext as App).database

    private val api = NetWorkProvider.provideApiService(
        NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL),
        ApiServiceData::class.java
    )

    private val repository = TaskRepository(api, database.taskDao(), applicationContext)
    private val instructionTaskRepository = InstructionTaskRepository(api, database.instructionTaskDao(), applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("auth_token", null)

            if (token.isNullOrEmpty()) {
                return Result.failure()
            }

            repository.getAndSaveTasks(token)
            instructionTaskRepository.getAndSaveTasks(token)

            Result.success()

        } catch (e: Exception) {
            Result.retry() // 🔥 попробует снова позже
        }
    }
}
