package com.example.check_911.data.db.repository

import androidx.room.Transaction
import com.example.check_911.ApiServiceData
import com.example.check_911.data.db.dao.SurveyDao
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyEntity
import com.example.check_911.data.networking.models.SurveyDto
import retrofit2.Response

class SurveyRepository(
    private val api: ApiServiceData,
    private val dao: SurveyDao
) {
    // Получаем с сервера и сохраняем в базу
    suspend fun getAndSaveSurveys(token: String) {
        try {
            val response: Response<List<SurveyDto>> = api.getSurveys("$token")

            if (response.isSuccessful) {
                val surveys = response.body()
                if (surveys != null) {
                    saveSurveyToDatabase(surveys)
                } else {
                    throw Exception("Сервер повернув порожню відповідь")
                }
            } else {
//                throw Exception("Помилка завантаження із сервера: ${response.errorBody()?.string()}")
                throw Exception("${response.errorBody()?.string()}")
            }

        } catch (e: Exception) {
//            throw Exception("Помилка запиту: ${e.message}", e)
            throw Exception("${e.message}", e)
        }
    }

    // Сохранение в базу
    @Transaction
     suspend fun saveSurveyToDatabase(surveys: List<SurveyDto>) {


        dao.clearOptions()
        dao.clearQuestions()
        dao.clearCategories()
        dao.clearSurveys() // Очистка таблиц перед добавлением новых данных

//        surveys.forEach { survey ->
        surveys.filter { it.isVisible }.forEach { survey -> // фильтруем по isVisible
            dao.insertSurveys(listOf(
                SurveyEntity(
                    id = survey.id,
                    createdAt = survey.createdAt,
                    createdBy = survey.createdBy,
                    title = survey.title,
                    periodId = survey.periodId,
                    periodDescription = survey.periodDescription,
                    typeId = survey.typeId,
                    typeDescription = survey.typeDescription,
                    isVisible = survey.isVisible,
                    orderNumber = survey.orderNumber,
                    sendToTelegram = survey.isSendTelegram,
                    onlyPharmacy = survey.onlyPharmacy
                )
            ))

            survey.categories.forEach { category ->
                dao.insertCategories(listOf(
                    CategoryQuestionsEntity(
                        id = category.id,
                        surveyId = survey.id,
                        description = category.description,
                        orderNumber = category.orderNumber
                    )
                ))

                category.questions.forEach { question ->
                    dao.insertQuestions(listOf(
                        QuestionEntity(
                            id = question.id,
                            categoryId = category.id,
                            text = question.text,
//                            requiredIfYes = question.requiredIfYes,
//                            requiredIfNo = question.requiredIfNo,
//                            alwaysRequired = question.alwaysRequired,
                            requiredIfYes = question.photoRequiredIfYes,
                            requiredIfNo = question.photoRequiredIfNo,
                            alwaysRequired = question.photoAlwaysRequired,
                            numberInput = question.numberInput,
                            percentInput = question.percentInput,
                            textInput = question.textInput,
                            singleChoiceInput = question.singleChoiceInput,
                            multiChoiceInput = question.multiChoiceInput,
                            isImportant = question.isImportant,
                            orderNumber = question.orderNumber,
                            sendToTelegram = question.isSendTelegram,
                        )
                    ))

                    question.answers.forEach { answer ->
                        dao.insertOptions(listOf(
                            OptionForQuestionsEntity(
                                id = answer.id,
                                questionId = question.id,
                                text = answer.text,
                                isCorrect = answer.isCorrect
                            )
                        ))
                    }
                }
            }
        }
    }

    suspend fun addSurveysToDatabase(surveys: List<SurveyDto>) {
        surveys.filter { it.isVisible }.forEach { survey ->
            dao.insertSurveys(listOf(
                SurveyEntity(
                    id = survey.id,
                    createdAt = survey.createdAt,
                    createdBy = survey.createdBy,
                    title = survey.title,
                    periodId = survey.periodId,
                    periodDescription = survey.periodDescription,
                    typeId = survey.typeId,
                    typeDescription = survey.typeDescription,
                    isVisible = survey.isVisible,
//                    isVisible = true,
                    orderNumber = survey.orderNumber,
                    sendToTelegram = survey.isSendTelegram,
                    onlyPharmacy = survey.onlyPharmacy
                )
            ))

            survey.categories.forEach { category ->
                dao.insertCategories(listOf(
                    CategoryQuestionsEntity(
                        id = category.id,
                        surveyId = survey.id,
                        description = category.description,
                        orderNumber = category.orderNumber
                    )
                ))

                category.questions.forEach { question ->
                    dao.insertQuestions(listOf(
                        QuestionEntity(
                            id = question.id,
                            categoryId = category.id,
                            text = question.text,
                            requiredIfYes = question.photoRequiredIfYes,
                            requiredIfNo = question.photoRequiredIfNo,
                            alwaysRequired = question.photoAlwaysRequired,
                            numberInput = question.numberInput,
                            percentInput = question.percentInput,
                            textInput = question.textInput,
                            singleChoiceInput = question.singleChoiceInput,
                            multiChoiceInput = question.multiChoiceInput,
                            isImportant = question.isImportant,
                            orderNumber = question.orderNumber,
                            sendToTelegram = question.isSendTelegram
                        )
                    ))

                    question.answers.forEach { answer ->
                        dao.insertOptions(listOf(
                            OptionForQuestionsEntity(
                                id = answer.id,
                                questionId = question.id,
                                text = answer.text,
                                isCorrect = answer.isCorrect
                            )
                        ))
                    }
                }
            }
        }
    }
}
