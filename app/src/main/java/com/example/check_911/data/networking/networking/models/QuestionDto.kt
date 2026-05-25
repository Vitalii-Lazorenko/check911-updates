package com.example.check_911.data.networking.models

data class QuestionDto(
    val id: String,
    val text: String,
//    val requiredIfYes: Boolean,
    val photoRequiredIfYes: Boolean,
//    val requiredIfNo: Boolean,
    val photoRequiredIfNo: Boolean,
//    val alwaysRequired: Boolean,
    val photoAlwaysRequired: Boolean,
    val numberInput: Boolean,
    val percentInput: Boolean,
    val textInput: Boolean,
    val singleChoiceInput: Boolean,
    val multiChoiceInput: Boolean,
    val isImportant: Boolean,
    val orderNumber: Int,
    val isSendTelegram: Boolean,
    val answers: List<OptionForQuestionsDto>
)
