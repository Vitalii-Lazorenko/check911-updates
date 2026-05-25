package com.example.check_911.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,     //ID задачи

    val createdAt: String,         //дата создания
//    val updatedAt: String?,         //дата обновления

    val questionId: String,        //из какого вопроса
    val surveyHeaderId: String,          //опрос
    val surveyLogId: String,       //лог отправки

    val imgUrl: String?,   // img_url

    val questionText: String,      //текст вопроса
    val answerText: String?,        //ответ

    val taskText: String,           //текст задачи

//    val status: String?,

//    не используются
    val taskAnswer: String?,
    val taskImg: String?,
    val taskHandledAt: String?,

//для сохранения ответов
    @ColumnInfo(name = "local_comment") val localComment: String?,
@ColumnInfo(name = "local_photo_path") val localPhotoPath: String?,
@ColumnInfo(name = "is_sent") val isSent: Boolean = false
)
