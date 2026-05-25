package com.example.check_911.data.db.repository

import com.example.check_911.ApiServiceData

class InstructionRepository(
    private val api: ApiServiceData
) {
    suspend fun getInstructions(token: String) {
        val response = api.getInstructions(token)
        if (!response.isSuccessful) {
            throw Exception(response.errorBody()?.string() ?: "Instruction loading error: ${response.code()}")
        }
    }
}
