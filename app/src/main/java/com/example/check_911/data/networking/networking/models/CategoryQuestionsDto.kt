package com.example.check_911.data.networking.models

import com.example.check_911.data.networking.models.QuestionDto

data class CategoryQuestionsDto(
    val id: String,
    val description: String,
    val orderNumber: Int,
    val questions: List<QuestionDto>
)
