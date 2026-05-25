package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.check_911.data.db.entity.TaskEntity


@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTasksCountFlow(): Flow<Int>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTasksCount(): Int

    @Query("SELECT id FROM tasks")
    suspend fun getAllTaskIds(): List<String>

    @Query("""
    UPDATE tasks 
    SET local_comment = :comment, local_photo_path = :photoPath, is_sent = 0 
    WHERE id = :taskId
""")
    suspend fun saveTaskProgress(taskId: String, comment: String?, photoPath: String?)

        @Query("""
    UPDATE tasks 
    SET local_comment = NULL, 
        local_photo_path = NULL 
    WHERE id = :taskId
    """)
        suspend fun clearTaskProgress(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
}