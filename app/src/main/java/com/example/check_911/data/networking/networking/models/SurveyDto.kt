package com.example.check_911.data.networking.models

data class SurveyDto(
    val id: String,
    val createdAt: String,
    val createdBy: Int,
    val title: String,
    val periodId: Int,
    val periodDescription: String,
    val typeId: Int,
    val typeDescription: String,
    val isVisible: Boolean,
    val orderNumber: Int,
    val isSendTelegram: Boolean,
    val onlyPharmacy: Boolean,
    val categories: List<CategoryQuestionsDto>
)
