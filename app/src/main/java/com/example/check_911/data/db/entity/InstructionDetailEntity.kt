package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instruction_details",
    indices = [Index("categoryId")],
    foreignKeys = [ForeignKey(
        entity = InstructionCategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InstructionDetailEntity(
    @PrimaryKey val localId: String,
    val id: String,
    val categoryId: String,
    val title: String,
    val templateId: String,
    val orderNumber: Int
)
