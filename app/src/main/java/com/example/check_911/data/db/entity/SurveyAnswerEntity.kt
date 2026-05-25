package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "survey_answers",
    foreignKeys = [ForeignKey(
        entity = SurveyResultEntity::class,
        parentColumns = ["surveyId"],
        childColumns = ["surveyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["surveyId", "questionId"], unique = true)
    ]
)
data class SurveyAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val surveyId: String,             // Внешний ключ — ID опросника из SurveyResultEntity

    val categoryId: String,           // ID категории
    val categoryTitle: String,        // Название категории

    val questionId: String,           // ID вопроса
    val questionText: String,         // Текст вопроса

    val selectedSingleAnswer: String?,     // ID ответа
    val selectedMultiAnswers: String?,      // ID ответов (через запятую)
    val selectedAnswersText: String?, // Текст выбранных ответов

    val numberAnswer: Int?,        // Числовой ответ
    val percentAnswer: Double?,       // Процентный ответ
    val textAnswer: String?,          // Текстовый ответ

    val comment: String?,             // Комментарий
    val photoPath: String?,           // Путь к фото (если нужно)
    val skipped: Boolean              // Флаг "не давать ответ"
)

