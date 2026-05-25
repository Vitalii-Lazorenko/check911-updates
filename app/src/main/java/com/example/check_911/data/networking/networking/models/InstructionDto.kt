package com.example.check_911.data.networking.networking.models

data class InstructionDto(
    val id: String,
    val title: String,
    val createdAt: String,
    val createdBy: Long,
    val createdByName: String,
    val parentId: String,
    val periodId: Int,
    val periodTitle: String,
    val constraintPharmacy: Boolean,
    val categories: List<InstructionCategoryDto>
)

data class InstructionCategoryDto(
    val id: String,
    val title: String,
    val details: List<InstructionDetailDto>
)

data class InstructionDetailDto(
    val id: String,
    val title: String,
    val templateId: String
)
