package com.example.check_911.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instructions")
data class InstructionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: String,
    val createdBy: Long,
    val createdByName: String,
    val parentId: String,
    val periodId: Int,
    val periodTitle: String,
    val constraintPharmacy: Boolean
)
