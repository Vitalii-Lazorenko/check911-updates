package com.example.check_911.data.db.repository

import android.util.Log
import com.example.check_911.ApiServiceData
import com.example.check_911.data.db.dao.DaoUsers
import com.example.check_911.data.db.entity.UsersEntity

class UsersRepository(
    private val api: ApiServiceData,
    private val dao: DaoUsers
) {
    suspend fun getAndSaveUsers(token: String) {
        try {
            val usersDtoList = api.getUsers(token)
            val usersEntityList = usersDtoList.map { dto ->
                UsersEntity(
//                    id = dto.id,
                    positionName = dto.positionName,
                    userName = dto.userName,
                    idGamma = dto.idGamma
                )
            }
            dao.refreshUsers(usersEntityList)
            Log.d("qqq", "Запись пользователей: ${usersEntityList}")
        } catch (e: Exception) {
//            throw Exception("Помилка завантаження користувачів: ${e.message}")
            throw Exception("${e.message}")
        }
    }
}
