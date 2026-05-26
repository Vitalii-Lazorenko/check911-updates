package com.example.check_911.data.networking.networking.models

import java.util.Date
import com.google.gson.annotations.SerializedName

data class InstructionLogPostRequest(
    val pharmacyId: Long,
    val gammaId: Long,
    val dateVersion: Date?,
    val instructionHeaderId: String
)

data class InstructionLogPostResponse(
    @SerializedName("logHeaderId") val logHeaderId: String? = null,
    @SerializedName("id") val id: String? = null
)
