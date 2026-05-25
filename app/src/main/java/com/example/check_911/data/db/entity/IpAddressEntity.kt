package com.example.check_911.data.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "ipaddress")
data class IpAddressEntity(
    @ColumnInfo(name = "ipaddress")
    var ipaddress: String,
    @PrimaryKey()
    var id: Long = 1,

    ) : Parcelable
