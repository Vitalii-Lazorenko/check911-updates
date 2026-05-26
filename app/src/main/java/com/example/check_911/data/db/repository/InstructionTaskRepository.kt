package com.example.check_911.data.db.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Transaction
import com.example.check_911.ApiServiceData
import com.example.check_911.InstructionTasksActivity
import com.example.check_911.data.NotificationHelper
import com.example.check_911.data.db.dao.InstructionTaskDao
import com.example.check_911.data.db.entity.InstructionTaskEntity
import com.example.check_911.data.networking.networking.models.InstructionTaskDto
import com.example.check_911.data.utils.AppLogger

class InstructionTaskRepository(
    private val api: ApiServiceData,
    private val dao: InstructionTaskDao,
    private val appContext: Context
) {

    suspend fun getAndSaveTasks(token: String) {
        try {
            val response = api.getInstructionTasks(token)
            if (!response.isSuccessful) {
                throw Exception(response.errorBody()?.string())
            }

            val tasks = response.body().orEmpty()
            val oldIds = dao.getAllTaskIds()
            val newIds = tasks.map { it.id }
            val newTasks = newIds.filter { it !in oldIds }

            saveTasksToDatabase(tasks)

            if (newTasks.isNotEmpty()) {
                notifyAboutNewTasks(newTasks.size)
            }
        } catch (e: Exception) {
            AppLogger.log("CRASH", "Instruction tasks error: ${e.message}", null)
            throw Exception(e.message, e)
        }
    }

    private fun notifyAboutNewTasks(newCount: Int) {
        val helper = NotificationHelper(appContext)
        helper.ensureChannels()

        val intent = Intent(appContext, InstructionTasksActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        helper.notify(
            NotificationHelper.CH_STATUS,
            NotificationHelper.ID_NEW_INSTRUCTION_TASKS,
            "Нові задачі по інструкціях",
            "Отримано нових задач: $newCount",
            pendingIntent
        )
    }

    @Transaction
    suspend fun saveTasksToDatabase(tasks: List<InstructionTaskDto>) {
        dao.clearTasks()
        val entities = tasks.map {
            InstructionTaskEntity(
                id = it.id,
                createdAt = it.createdAt,
                questionId = it.instructionDetailId,
                surveyHeaderId = it.instructionHeaderId,
                surveyLogId = it.instructionLogHeaderId,
                imgUrl = it.taskClaimImgUrl,
                questionText = it.instructionDetailTitle,
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
