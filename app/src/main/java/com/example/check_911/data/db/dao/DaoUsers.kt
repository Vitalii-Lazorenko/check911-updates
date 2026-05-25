package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.check_911.data.db.entity.UsersEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoUsers {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setUsers(checks: List<UsersEntity>): List<Long>

    @Query("SELECT * FROM users ORDER BY userName")
    fun getUsers(): Flow<List<UsersEntity>>

    @Query("DELETE FROM  users ")
    suspend fun deleteUsers()


    @Transaction
    suspend fun refreshUsers(users: List<UsersEntity>) {
        deleteUsers()
        setUsers(users)
    }

}