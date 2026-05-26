package com.example.check_911.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instruction_tasks")
data class InstructionTaskEntity(
    @PrimaryKey val id: String,
    val createdAt: String,
    val questionId: String,
    val surveyHeaderId: String,
    val surveyLogId: String,
    val imgUrl: String?,
    val questionText: String,
    val answerText: String?,
    val taskText: String,
    val taskAnswer: String?,
    val taskImg: String?,
    val taskHandledAt: String?,
    @ColumnInfo(name = "local_comment") val localComment: String?,
    @ColumnInfo(name = "local_photo_path") val localPhotoPath: String?,
    @ColumnInfo(name = "is_sent") val isSent: Boolean = false
)
