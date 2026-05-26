package com.example.check_911.data.networking.networking.models

data class InstructionTaskDto(
    val id: String,
    val createdAt: String,
    val instructionHeaderId: String,
    val instructionDetailId: String,
    val instructionLogHeaderId: String,
    val instructionLogDetailId: String,
    val instructionDetailTitle: String,
    val taskClaimText: String,
    val taskClaimImgUrl: String?,
    val taskAnswerText: String?,
    val taskAnswerImg: String?,
    val taskAnswerGammaId: Number?,
    val taskHandledAt: String?,
    val taskDeadlineAt: String?
)
