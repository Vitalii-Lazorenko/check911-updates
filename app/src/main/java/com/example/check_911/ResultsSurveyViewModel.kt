package com.example.check_911

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import com.example.check_911.data.db.repository.ResultsSurveyRepository
import kotlinx.coroutines.launch

class ResultsSurveyViewModel(private val repository: ResultsSurveyRepository) : ViewModel() {

    private val _surveyState = MutableLiveData<Result<Unit>>()
    val surveyState: LiveData<Result<Unit>> get() = _surveyState

    fun saveSurveyResult(result: SurveyResultEntity, answers: List<SurveyAnswerEntity>) {
        viewModelScope.launch {
            try {
                repository.saveSurveyResult(result, answers)
                _surveyState.postValue(Result.success(Unit))
            } catch (e: Exception) {
                _surveyState.postValue(Result.failure(e))
            }
        }
    }

    fun getSurveyResult(surveyId: String): LiveData<SurveyResultEntity?> =
        repository.getSurveyResult(surveyId).asLiveData()

    fun getSurveyAnswers(surveyId: String): LiveData<List<SurveyAnswerEntity>> =
        repository.getSurveyAnswers(surveyId).asLiveData()

    fun updateSurveyStatus(surveyId: String, status: String) {
        viewModelScope.launch {
            repository.updateSurveyStatus(surveyId, status)
        }
    }

    fun deleteSurvey(surveyId: String) {
        viewModelScope.launch {
            repository.deleteSurvey(surveyId)
        }
    }
}