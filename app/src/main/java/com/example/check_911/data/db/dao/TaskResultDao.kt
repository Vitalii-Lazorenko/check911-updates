package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.check_911.data.db.entity.TaskResultEntity

@Dao
interface TaskResultDao {

    @Query("SELECT * FROM task_results WHERE taskId = :taskId LIMIT 1")
    suspend fun getResult(taskId: String): TaskResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: TaskResultEntity)

    @Query("""
        UPDATE task_results 
        SET comment = :comment, photoPath = :photoPath 
        WHERE taskId = :taskId
    """)
    suspend fun update(taskId: String, comment: String?, photoPath: String?)

    @Query("DELETE FROM task_results WHERE taskId = :taskId")
    suspend fun clear(taskId: String)

    @Query("DELETE FROM task_results WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)
}