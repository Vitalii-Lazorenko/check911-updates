package com.example.check_911.data.networking.networking.models

import com.google.gson.annotations.SerializedName

data class UsersDto(
//    @SerializedName("id")
//    val id: Long,
    @SerializedName("positionName")
    val positionName: String,
    @SerializedName("userName")
    val userName: String,
    @SerializedName("id_gamma")
    val idGamma: Long,

    )
