package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_results")
data class TaskResultEntity(
    @PrimaryKey val taskId: String,
    val comment: String? = null,
    val photoPath: String? = null,
    val gammaId: Long?
//    val status: String = "draft" // draft / ready / sent
)
