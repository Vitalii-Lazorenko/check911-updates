package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.check_911.data.db.entity.InstructionAnswerEntity
import com.example.check_911.data.db.entity.InstructionResultEntity

@Dao
interface InstructionResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResult(result: InstructionResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswer(answer: InstructionAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnswers(answers: List<InstructionAnswerEntity>)

    @Query("SELECT * FROM instruction_results WHERE instructionId = :instructionId LIMIT 1")
    suspend fun getResult(instructionId: String): InstructionResultEntity?

    @Query("SELECT * FROM instruction_results")
    suspend fun getAllResults(): List<InstructionResultEntity>

    @Query("SELECT * FROM instruction_answers WHERE instructionId = :instructionId")
    suspend fun getAnswersByInstruction(instructionId: String): List<InstructionAnswerEntity>

    @Query("SELECT * FROM instruction_answers WHERE instructionId = :instructionId AND detailLocalId = :detailLocalId LIMIT 1")
    suspend fun getAnswer(instructionId: String, detailLocalId: String): InstructionAnswerEntity?

    @Query("DELETE FROM instruction_answers WHERE instructionId = :instructionId AND detailLocalId = :detailLocalId")
    suspend fun deleteAnswer(instructionId: String, detailLocalId: String)

    @Query("DELETE FROM instruction_answers WHERE instructionId = :instructionId")
    suspend fun clearAnswersForInstruction(instructionId: String)

    @Query("DELETE FROM instruction_results WHERE instructionId = :instructionId")
    suspend fun deleteResult(instructionId: String)

    @Query("UPDATE instruction_results SET status = :status, sentDate = :sentDate WHERE instructionId = :instructionId")
    suspend fun updateResultStatus(instructionId: String, status: String, sentDate: String?)

    @Transaction
    suspend fun replaceInstructionAnswers(instructionId: String, answers: List<InstructionAnswerEntity>) {
        clearAnswersForInstruction(instructionId)
        upsertAnswers(answers)
    }

    @Transaction
    suspend fun clearInstructionResult(instructionId: String) {
        clearAnswersForInstruction(instructionId)
        deleteResult(instructionId)
    }
}
