package com.example.check_911

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.repository.InstructionRepository
import kotlinx.coroutines.launch

class InstructionViewModel(
    private val repository: InstructionRepository
) : ViewModel() {

    private val _instructionState = MutableLiveData<Result<Unit>>()
    val instructionState: LiveData<Result<Unit>> = _instructionState

    fun loadInstructions(token: String) {
        viewModelScope.launch {
            try {
                repository.getInstructions(token)
                _instructionState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _instructionState.postValue(Result.failure(e))
            }
        }
    }
}
