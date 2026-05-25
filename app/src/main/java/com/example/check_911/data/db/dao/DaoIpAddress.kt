package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.check_911.data.db.entity.IpAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoIpAddress {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setIpAddress(checks: IpAddressEntity)

    @Query("SELECT ipaddress FROM ipaddress limit 1")
    suspend fun getIpAddress(): String?

    @Query("SELECT ipaddress FROM ipaddress limit 1")
    fun getIpAddressFlow(): Flow<String?>

    @Query("DELETE FROM ipaddress ")
    suspend fun deleteIpAddress()

}