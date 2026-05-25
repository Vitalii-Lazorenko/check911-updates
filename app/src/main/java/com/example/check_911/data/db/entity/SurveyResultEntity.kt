package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survey_results")
data class SurveyResultEntity(
    @PrimaryKey val surveyId: String,  // ID опросника
    val surveyTitle: String,           // Название опросника
    val userId: Long,                   // ID пользователя
    val status: String,              // Статус (черновик, готов к отправке)
    val sentDate: String? = null       // Дата отправки в формате "yyyy-MM-dd"
)


