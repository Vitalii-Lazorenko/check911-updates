package com.example.check_911.data.networking.networking.models

data class InstructionLogPostRequest(
    val gammaId: Long,
    val dateVersion: String,
    val instructionHeaderId: String
)

data class InstructionLogPostResponse(
    val logHeaderId: String?
)
