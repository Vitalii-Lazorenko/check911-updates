package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(
        entity = CategoryQuestionsEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val text: String,
    val requiredIfYes: Boolean,
    val requiredIfNo: Boolean,
    val alwaysRequired: Boolean,
    val numberInput: Boolean,
    val percentInput: Boolean,
    val textInput: Boolean,
    val singleChoiceInput: Boolean,
    val multiChoiceInput: Boolean,
    val isImportant: Boolean,
    val orderNumber: Int,
    val sendToTelegram: Boolean = false
)