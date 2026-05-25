package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultsSurveyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveyResult(result: SurveyResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveyAnswers(answers: List<SurveyAnswerEntity>)

    @Query("SELECT * FROM survey_results WHERE surveyId = :surveyId")
    fun getSurveyResult(surveyId: String): Flow<SurveyResultEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnswer(answer: SurveyAnswerEntity): Long

    @Update
    suspend fun updateAnswer(answer: SurveyAnswerEntity)

    @Query("SELECT COUNT(*) FROM survey_results WHERE status = 'draft'")
    suspend fun countDraft(): Int

    @Query("SELECT COUNT(*) FROM survey_results WHERE status = 'ready'")
    suspend fun countReady(): Int

    @Query("SELECT * FROM survey_results")
    suspend fun getAllResults(): List<SurveyResultEntity>

    @Query("SELECT * FROM survey_answers WHERE surveyId = :surveyId AND questionId = :questionId LIMIT 1")
    suspend fun getAnswer(surveyId: String, questionId: String): SurveyAnswerEntity?

    @Query("SELECT * FROM survey_answers WHERE surveyId = :surveyId")
    fun getSurveyAnswers(surveyId: String): Flow<List<SurveyAnswerEntity>>

    @Query("SELECT * FROM survey_answers WHERE surveyId = :surveyId")
    suspend fun getAnswersForSurvey(surveyId: String): List<SurveyAnswerEntity>

    @Query("SELECT * FROM survey_answers")
    suspend fun getAllSurveyAnswers(): List<SurveyAnswerEntity>

    @Query("SELECT * FROM survey_answers WHERE surveyId = :surveyId AND questionId = :questionId")
    suspend fun getAnswersForQuestion(surveyId: String, questionId: String): List<SurveyAnswerEntity>

    @Query("SELECT * FROM survey_results")
    suspend fun getAllSurveyResults(): List<SurveyResultEntity>


    @Query("UPDATE survey_answers SET comment = :comment WHERE surveyId = :surveyId AND questionId = :questionId")
    suspend fun updateCommentForQuestion(surveyId: String, questionId: String, comment: String?)

//    @Query("UPDATE survey_results SET status = :status WHERE surveyId = :surveyId")
//    suspend fun updateSurveyStatus(surveyId: String, status: String)

    @Query("UPDATE survey_results SET status = :status, sentDate = :date WHERE surveyId = :surveyId")
    suspend fun updateSurveyStatusWithDate(surveyId: String, status: String, date: String)


    @Query("DELETE FROM survey_results WHERE surveyId = :surveyId")
    suspend fun deleteSurveyResult(surveyId: String)

    @Query("DELETE FROM survey_answers WHERE surveyId = :surveyId")
    suspend fun deleteSurveyAnswers(surveyId: String)

    @Query("DELETE FROM survey_answers WHERE questionId = :questionId")
    suspend fun deleteSurveyAnswersForIdquestion(questionId: String)

    @Query("DELETE FROM survey_results")
    suspend fun clearResults()

    @Query("DELETE FROM survey_answers")
    suspend fun clearAnswers()

    @Transaction
    suspend fun deleteAllSurvey() {
        clearResults()
        clearAnswers()
    }

    @Transaction
    suspend fun deleteSurvey(surveyId: String) {
        deleteSurveyAnswers(surveyId)
        deleteSurveyResult(surveyId)
    }

    @Query("DELETE FROM survey_answers WHERE surveyId = :surveyId AND questionId = :questionId")
    suspend fun deleteSurveyAnswerForQuestion(surveyId: String, questionId: String)

}
