package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "options",
    foreignKeys = [ForeignKey(
        entity = QuestionEntity::class,
        parentColumns = ["id"],
        childColumns = ["questionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
class OptionForQuestionsEntity (
    @PrimaryKey val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean
)