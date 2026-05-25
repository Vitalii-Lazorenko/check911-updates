package com.example.check_911

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.repository.SurveyRepository
import kotlinx.coroutines.launch

class SurveyViewModel(private val surveyRepository: SurveyRepository) : ViewModel() {
    private val _surveyState = MutableLiveData<Result<Unit>>()
    val surveyState: LiveData<Result<Unit>> = _surveyState

    fun loadSurveys(token: String) {
        viewModelScope.launch {
            try {
                surveyRepository.getAndSaveSurveys(token)
                _surveyState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _surveyState.postValue(Result.failure(e))
            }
        }
    }
}
