package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "categoriesQuestions",
    foreignKeys = [ForeignKey(
        entity = SurveyEntity::class,
        parentColumns = ["id"],
        childColumns = ["surveyId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CategoryQuestionsEntity(
    @PrimaryKey val id: String,
    val surveyId: String,
    val description: String,
    val orderNumber: Int
)