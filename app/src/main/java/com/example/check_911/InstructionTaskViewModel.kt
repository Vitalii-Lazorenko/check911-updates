package com.example.check_911

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.repository.InstructionTaskRepository
import kotlinx.coroutines.launch

class InstructionTaskViewModel(
    private val repository: InstructionTaskRepository
) : ViewModel() {

    private val _taskState = MutableLiveData<Result<Unit>>()
    val taskState: LiveData<Result<Unit>> = _taskState

    fun loadTasks(token: String) {
        viewModelScope.launch {
            try {
                repository.getAndSaveTasks(token)
                _taskState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _taskState.postValue(Result.failure(e))
            }
        }
    }
}
