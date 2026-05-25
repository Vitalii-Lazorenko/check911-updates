package com.example.check_911.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "users")
data class UsersEntity(

//    @ColumnInfo(name = "id")
//    var id: Long,
    @ColumnInfo(name = "positionName")
    var positionName: String,

    @ColumnInfo(name = "userName")
    var userName: String,

    @PrimaryKey()
    @ColumnInfo(name = "idGamma")
    var idGamma: Long,

    ) : Parcelable

