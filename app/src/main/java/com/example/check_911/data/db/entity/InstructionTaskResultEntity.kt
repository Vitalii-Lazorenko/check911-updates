package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instruction_task_results")
data class InstructionTaskResultEntity(
    @PrimaryKey val taskId: String,
    val comment: String? = null,
    val photoPath: String? = null,
    val gammaId: Long?
)
