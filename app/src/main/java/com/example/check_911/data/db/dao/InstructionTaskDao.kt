package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.check_911.data.db.entity.InstructionTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstructionTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<InstructionTaskEntity>)

    @Query("DELETE FROM instruction_tasks")
    suspend fun clearTasks()

    @Query("SELECT COUNT(*) FROM instruction_tasks")
    fun getTasksCountFlow(): Flow<Int>

    @Query("SELECT * FROM instruction_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<InstructionTaskEntity>>

    @Query("SELECT * FROM instruction_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): InstructionTaskEntity?

    @Query("SELECT id FROM instruction_tasks")
    suspend fun getAllTaskIds(): List<String>

    @Query(
        """
        UPDATE instruction_tasks
        SET local_comment = :comment, local_photo_path = :photoPath, is_sent = 0
        WHERE id = :taskId
    """
    )
    suspend fun saveTaskProgress(taskId: String, comment: String?, photoPath: String?)

    @Query(
        """
        UPDATE instruction_tasks
        SET local_comment = NULL, local_photo_path = NULL
        WHERE id = :taskId
    """
    )
    suspend fun clearTaskProgress(taskId: String)

    @Query("DELETE FROM instruction_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
}
