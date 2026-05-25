package com.example.check_911.data.db.repository

import com.example.check_911.data.db.dao.ResultsSurveyDao
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import kotlinx.coroutines.flow.Flow

class ResultsSurveyRepository(private val resultsSurveyDao: ResultsSurveyDao) {

    fun getSurveyResult(surveyId: String): Flow<SurveyResultEntity?> =
        resultsSurveyDao.getSurveyResult(surveyId)

    fun getSurveyAnswers(surveyId: String): Flow<List<SurveyAnswerEntity>> =
        resultsSurveyDao.getSurveyAnswers(surveyId)

    suspend fun saveSurveyResult(result: SurveyResultEntity, answers: List<SurveyAnswerEntity>) {
        resultsSurveyDao.insertSurveyResult(result)
        resultsSurveyDao.insertSurveyAnswers(answers)
    }

    suspend fun updateSurveyStatus(surveyId: String, status: String) {
//        resultsSurveyDao.updateSurveyStatus(surveyId, status)
    }

    suspend fun deleteSurvey(surveyId: String) {
        resultsSurveyDao.deleteSurvey(surveyId)
    }
}