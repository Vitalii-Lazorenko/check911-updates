package com.example.check_911.data.mapper

import com.example.check_911.data.db.entity.UsersEntity
import com.example.check_911.data.networking.networking.models.UsersDto

fun UsersDto.toEntity() = UsersEntity(
//    id, userName, idGamma
    positionName, userName, idGamma
)

fun List<UsersDto>.toEntity() = map { it.toEntity() }