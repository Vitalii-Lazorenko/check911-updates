package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "instruction_answers",
    primaryKeys = ["detailLocalId"],
    foreignKeys = [ForeignKey(
        entity = InstructionResultEntity::class,
        parentColumns = ["instructionId"],
        childColumns = ["instructionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("instructionId"), Index("groupKey")]
)
data class InstructionAnswerEntity(
    val detailLocalId: String,
    val instructionId: String,
    val detailId: String,
    val detailTitle: String,
    val groupKey: String?,
    val comment: String?,
    val photoPath: String?
)
