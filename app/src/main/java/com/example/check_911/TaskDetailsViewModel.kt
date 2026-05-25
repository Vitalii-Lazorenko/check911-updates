package com.example.check_911

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.dao.TaskResultDao
import com.example.check_911.data.db.entity.TaskResultEntity
import com.example.check_911.data.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TaskDetailsViewModel(
    private val dao: TaskResultDao,
    private val app: Application,
) : ViewModel() {

    val currentPhotoPath = MutableStateFlow<String?>(null)
    val currentTaskId = MutableStateFlow<String?>(null)

    fun init(taskId: String) {
        currentTaskId.value = taskId

        viewModelScope.launch {
            val result = dao.getResult(taskId)
            currentPhotoPath.value = result?.photoPath
        }
    }

    fun setPhotoPath(path: String) {
        currentPhotoPath.value = path
    }

    val sharedPreferences = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val gammaId = sharedPreferences.getLong("selected_user_idGamma", -1L)

//    fun confirmPhoto(comment: String?) {
//        val taskId = currentTaskId.value ?: return
//        val path = currentPhotoPath.value ?: return
//
//        viewModelScope.launch {
//            dao.insert(
//                TaskResultEntity(
//                    taskId = taskId,
//                    comment = comment,
//                    photoPath = path
//                )
//            )
//        }
//    }
fun confirmPhoto(comment: String?) {
    val taskId = currentTaskId.value ?: return
    val newPath = currentPhotoPath.value ?: return

    viewModelScope.launch {

        val existing = dao.getResult(taskId)

        // 🧹 удаляем старое фото если есть
        val oldPath = existing?.photoPath
        if (!oldPath.isNullOrBlank() && oldPath != newPath) {
            deletePhoto(oldPath)
        }

        dao.insert(
            TaskResultEntity(
                taskId = taskId,
                comment = comment,
                photoPath = newPath,
                gammaId = gammaId
            )
        )
    }
}

    private fun deletePhoto(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                AppLogger.log(
                    "PhotoDelete",
                    if (deleted) "🗑️ Старе фото видалено: $path"
                    else "⚠️ Не вдалося видалити: $path",
                    null
                )
            }
        } catch (e: Exception) {
            AppLogger.log("PhotoDelete", "❌ ${e.message}", null)
        }
    }

    fun saveComment(text: String?) {
        val taskId = currentTaskId.value ?: return

        viewModelScope.launch {
            val existing = dao.getResult(taskId)
            dao.insert(
                TaskResultEntity(
                    taskId = taskId,
                    comment = text,
                    photoPath = existing?.photoPath,
                    gammaId = gammaId
                )
            )
        }
    }
}