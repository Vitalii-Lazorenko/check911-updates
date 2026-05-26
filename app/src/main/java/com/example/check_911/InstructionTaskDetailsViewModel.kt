package com.example.check_911

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.dao.InstructionTaskResultDao
import com.example.check_911.data.db.entity.InstructionTaskResultEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class InstructionTaskDetailsViewModel(
    private val dao: InstructionTaskResultDao,
    private val app: Application,
) : ViewModel() {

    val currentPhotoPath = MutableStateFlow<String?>(null)
    val currentTaskId = MutableStateFlow<String?>(null)

    private val sharedPreferences = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val gammaId = sharedPreferences.getLong("selected_user_idGamma", -1L)

    fun init(taskId: String) {
        currentTaskId.value = taskId
        viewModelScope.launch {
            currentPhotoPath.value = dao.getResult(taskId)?.photoPath
        }
    }

    fun setPhotoPath(path: String) {
        currentPhotoPath.value = path
    }

    fun confirmPhoto(comment: String?) {
        val taskId = currentTaskId.value ?: return
        val newPath = currentPhotoPath.value ?: return
        viewModelScope.launch {
            val existing = dao.getResult(taskId)
            val oldPath = existing?.photoPath
            if (!oldPath.isNullOrBlank() && oldPath != newPath) {
                runCatching {
                    val file = File(oldPath)
                    if (file.exists()) file.delete()
                }
            }
            dao.insert(
                InstructionTaskResultEntity(
                    taskId = taskId,
                    comment = comment,
                    photoPath = newPath,
                    gammaId = gammaId
                )
            )
        }
    }

    fun saveComment(text: String?) {
        val taskId = currentTaskId.value ?: return
        viewModelScope.launch {
            val existing = dao.getResult(taskId)
            dao.insert(
                InstructionTaskResultEntity(
                    taskId = taskId,
                    comment = text,
                    photoPath = existing?.photoPath,
                    gammaId = gammaId
                )
            )
        }
    }
}
