package com.example.check_911.data.db.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class SurveyEntity(
    @PrimaryKey val id: String,
    val createdAt: String,
    val createdBy: Int,
    val title: String,
    val periodId: Int,
    val periodDescription: String,
    val typeId: Int,
    val typeDescription: String,
    val isVisible: Boolean,
    val orderNumber: Int,
    val sendToTelegram: Boolean = false,
    val onlyPharmacy: Boolean
)