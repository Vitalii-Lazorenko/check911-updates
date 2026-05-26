package com.example.check_911.data.networking.networking.models

data class InstructionTaskDto(
    val id: String,
    val createdAt: String,
    val surveyHeaderId: String,
    val surveyQuestionId: String,
    val surveyLogId: String,
    val surveyQuestionText: String,
    val surveyAnswerText: String?,
    val taskClaimText: String,
    val taskClaimImgUrl: String?,
    val taskAnswerText: String?,
    val taskAnswerImg: String?,
    val taskAnswerGammaId: Number?,
    val taskHandledAt: String?
)
