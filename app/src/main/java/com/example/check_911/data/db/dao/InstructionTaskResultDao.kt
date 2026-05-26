package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.check_911.data.db.entity.InstructionTaskResultEntity

@Dao
interface InstructionTaskResultDao {

    @Query("SELECT * FROM instruction_task_results WHERE taskId = :taskId LIMIT 1")
    suspend fun getResult(taskId: String): InstructionTaskResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: InstructionTaskResultEntity)

    @Query(
        """
        UPDATE instruction_task_results
        SET comment = :comment, photoPath = :photoPath
        WHERE taskId = :taskId
    """
    )
    suspend fun update(taskId: String, comment: String?, photoPath: String?)

    @Query("DELETE FROM instruction_task_results WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)
}
