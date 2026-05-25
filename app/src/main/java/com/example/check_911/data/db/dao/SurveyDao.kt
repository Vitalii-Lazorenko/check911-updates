package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyEntity


data class QuestionWithAnswers(
    @Embedded val question: QuestionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "questionId"
    )
    val answers: List<OptionForQuestionsEntity>
)

data class CategoryWithQuestions(
    @Embedded val category: CategoryQuestionsEntity,
    @Relation(
        entity = QuestionEntity::class,
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val questions: List<QuestionWithAnswers>
)

@Dao
interface SurveyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<SurveyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryQuestionsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<OptionForQuestionsEntity>)

    @Query("DELETE FROM surveys")
    suspend fun clearSurveys()

    @Query("DELETE FROM categoriesQuestions")
    suspend fun clearCategories()

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Query("DELETE FROM options")
    suspend fun clearOptions()

    @Query("DELETE FROM surveys WHERE id = :surveyId")
    suspend fun deleteSurveyById(surveyId: String)


    @Query("SELECT * FROM surveys ORDER BY periodId ASC")
    suspend fun getAllSurveysSorted(): List<SurveyEntity>

    @Query("SELECT * FROM surveys WHERE isVisible = 1")
    suspend fun getVisibleSurveys(): List<SurveyEntity>

    // Получение списка уникальных значений periodDescription отфильтрованных по isVisible
    @Query("SELECT DISTINCT periodDescription FROM surveys WHERE isVisible=1")
    suspend fun getUniquePeriodDescriptions(): List<String>

    // Получение списка опросов, отфильтрованных по periodDescription и isVisible
    @Query("SELECT * FROM surveys WHERE periodDescription IN (:selectedPeriods) AND isVisible=1")
    suspend fun getSurveysByPeriod(selectedPeriods: List<String>): List<SurveyEntity>
//
//    @Query("SELECT * FROM surveys WHERE periodDescription IN (:selectedPeriods) ")
//    suspend fun getSurveysByPeriod(selectedPeriods: List<String>): List<SurveyEntity>

//        @Transaction
//        @Query("SELECT * FROM categoriesQuestions WHERE surveyId = :surveyId")
//        suspend fun getCategoriesWithQuestions(surveyId: String): List<CategoryWithQuestionsRelation>

    @Transaction
    @Query("SELECT * FROM categoriesQuestions WHERE surveyId = :surveyId")
    suspend fun getCategoriesWithQuestionsAndAnswers(surveyId: String): List<CategoryWithQuestions>

    @Query("SELECT * FROM categoriesQuestions WHERE surveyId = :surveyId")
    suspend fun getCategoriesForSurvey(surveyId: String): List<CategoryQuestionsEntity>


    @Query("SELECT * FROM categoriesQuestions WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): CategoryQuestionsEntity?

    @Query("SELECT * FROM questions WHERE id = :questionId LIMIT 1")
    suspend fun getQuestionById(questionId: String): QuestionEntity?

    @Query("SELECT * FROM surveys WHERE id = :surveyId LIMIT 1")
    suspend fun getSurveyById(surveyId: String): SurveyEntity?

    //    Получение списка вариантов ответов
    @Query("SELECT * FROM options WHERE questionId = :questionId")
    suspend fun getOptionsForQuestion(questionId: String): List<OptionForQuestionsEntity>

    suspend fun getQuestionsForSurvey(surveyId: String): List<QuestionEntity> {
        val categories = getCategoriesWithQuestionsAndAnswers(surveyId)
        return categories.flatMap { it.questions.map { qw -> qw.question } }
    }



}