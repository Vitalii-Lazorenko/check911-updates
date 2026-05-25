package com.example.check_911.data.networking.networking.models.reserve

data class AuthorizationResponse(
    val token: String,
    val pharmacyAddress: String,
    val pharmacyName: String,
    val pharmacyId: Long
)
