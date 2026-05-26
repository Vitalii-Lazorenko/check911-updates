package com.example.check_911.data.networking.networking.models

import java.util.Date

data class InstructionLogPostRequest(
    val pharmacyId: Long,
    val gammaId: Long,
    val dateVersion: Date?,
    val instructionHeaderId: String
)

data class InstructionLogPostResponse(
    val logHeaderId: String?
)
