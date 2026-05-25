package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instruction_categories",
    indices = [Index("instructionId")],
    foreignKeys = [ForeignKey(
        entity = InstructionEntity::class,
        parentColumns = ["id"],
        childColumns = ["instructionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InstructionCategoryEntity(
    @PrimaryKey val id: String,
    val instructionId: String,
    val title: String
)
