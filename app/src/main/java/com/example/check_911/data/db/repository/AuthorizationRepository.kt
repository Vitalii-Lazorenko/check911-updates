package com.example.check_911.data.db.repository

import com.example.check_911.ApiServiceData
import com.example.check_911.data.networking.networking.models.reserve.AuthorizationResponse
import com.example.check_911.data.networking.networking.models.reserve.LoginRequest
import retrofit2.HttpException


class AuthorizationRepository(private val api: ApiServiceData) {
    suspend fun login(login: String, password: String): AuthorizationResponse {
        return try {
            val response = api.login(LoginRequest(login, password))
            response
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw Exception("Невірний логін або пароль!")
            } else {
                throw Exception("Помилка авторизації: ${e.message()}")
            }
        } catch (e: Exception) {
            throw Exception("Помилка з'єднання: ${e.message}")
        }
    }
}


//class AuthorizationRepository (private val api: ApiServiceData) {
//    suspend fun login(login: String, password: String): AuthorizationResponse {
//        return api.login(LoginRequest(login, password))
//    }
//}