package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instruction_results")
data class InstructionResultEntity(
    @PrimaryKey val instructionId: String,
    val instructionTitle: String,
    val status: String, // draft | ready | sent
    val sentDate: String? = null
)
