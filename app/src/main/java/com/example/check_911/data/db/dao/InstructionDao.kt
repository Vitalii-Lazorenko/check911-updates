package com.example.check_911.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.example.check_911.data.db.entity.InstructionCategoryEntity
import com.example.check_911.data.db.entity.InstructionDetailEntity
import com.example.check_911.data.db.entity.InstructionEntity

data class InstructionCategoryWithDetails(
    @Embedded val category: InstructionCategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val details: List<InstructionDetailEntity>
)

data class InstructionWithCategories(
    @Embedded val instruction: InstructionEntity,
    @Relation(
        entity = InstructionCategoryEntity::class,
        parentColumn = "id",
        entityColumn = "instructionId"
    )
    val categories: List<InstructionCategoryWithDetails>
)

@Dao
interface InstructionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(items: List<InstructionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(items: List<InstructionCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(items: List<InstructionDetailEntity>)

    @Query("DELETE FROM instructions")
    suspend fun clearInstructions()

    @Query("SELECT * FROM instructions ORDER BY createdAt DESC")
    suspend fun getAllInstructions(): List<InstructionEntity>

    @Transaction
    @Query("SELECT * FROM instructions ORDER BY createdAt DESC")
    suspend fun getInstructionsWithCategories(): List<InstructionWithCategories>
}
