package com.example.check_911.data.db.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Transaction
import com.example.check_911.ApiServiceData
import com.example.check_911.TasksActivity
import com.example.check_911.data.NotificationHelper
import com.example.check_911.data.db.dao.TaskDao
import com.example.check_911.data.db.entity.TaskEntity
import com.example.check_911.data.networking.networking.models.TaskDto
import com.example.check_911.data.utils.AppLogger

class TaskRepository(
    private val api: ApiServiceData,
    private val dao: TaskDao,
    private val appContext: Context
) {

    suspend fun getAndSaveTasks(token: String) {
        try {
            AppLogger.log("TASKS_API", "📡 Запрос задач...", null)
            val response = api.getTasks(token)

            if (response.isSuccessful) {
//                val tasks = response.body()
//                    ?: emptyList()
//                    ?: throw Exception("Сервер повернув порожню відповідь")
                val tasks = response.body().orEmpty()
                AppLogger.log(
                    "TASKS_API",
                    "✅ Успешно. Получено задач: ${tasks.size}",
                    null
                )
//                val oldCount = dao.getTasksCount()
                val oldIds = dao.getAllTaskIds()



                val newIds = tasks.map { it.id }

// 👉 находим реально новые задачи
                val newTasks = newIds.filter { it !in oldIds }

                saveTasksToDatabase(tasks)

                // 👉 уведомление если появились новые
//                if (newCount > oldCount) {
//                    notifyAboutNewTasks(newCount - oldCount)
//                }
                if (newTasks.isNotEmpty()) {
                    notifyAboutNewTasks(newTasks.size)
                }
            } else {
                AppLogger.log(
                    "TASKS_API",
                    "❌ Ошибка ответа: ${response.code()} ${response.errorBody()?.string()}",
                    null
                )
                throw Exception("${response.errorBody()?.string()}")
            }

        } catch (e: Exception) {
            AppLogger.log(
                "CRASH",
                "❌ getTasks error: ${e.message}",
                null
            )
            throw Exception("${e.message}", e)
        }
    }

    private fun notifyAboutNewTasks(newCount: Int) {
        try {
            val helper = NotificationHelper(appContext)

            helper.ensureChannels()

            val intent = Intent(appContext, TasksActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            helper.notify(
                NotificationHelper.CH_STATUS,
                NotificationHelper.ID_READY_TO_SEND,
                "Нові завдання",
                "Отримано нових задач: $newCount",
                        pendingIntent
            )

            AppLogger.log("TASKS_NOTIFY", "📢 Нові задачі: $newCount", null)

        } catch (e: Exception) {
            AppLogger.log("CRASH", "Notify error: ${e.message}", null)
        }
    }

    @Transaction
    suspend fun saveTasksToDatabase(tasks: List<TaskDto>) {

        dao.clearTasks() // очищаем старые

        val entities = tasks.map {
            TaskEntity(
                id = it.id,
                createdAt = it.createdAt,
                questionId = it.surveyQuestionId,
                surveyHeaderId = it.surveyHeaderId,
                surveyLogId = it.surveyLogId,
                imgUrl = it.taskClaimImgUrl,
                questionText = it.surveyQuestionText,
                answerText = it.taskAnswerText,
                taskText = it.taskClaimText,
                taskAnswer = it.taskAnswerText,
                taskImg = it.taskAnswerImg,
                taskHandledAt = it.taskHandledAt,
                localComment = null,
                localPhotoPath = null
            )
        }

        dao.insertTasks(entities)
    }
}