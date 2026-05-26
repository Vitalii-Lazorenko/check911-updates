package com.example.check_911.data.db.repository

import com.example.check_911.ApiServiceData
import com.example.check_911.data.db.dao.InstructionDao
import com.example.check_911.data.db.entity.InstructionCategoryEntity
import com.example.check_911.data.db.entity.InstructionDetailEntity
import com.example.check_911.data.db.entity.InstructionEntity

class InstructionRepository(
    private val api: ApiServiceData,
    private val dao: InstructionDao
) {
    suspend fun getInstructions(token: String) {
        val response = api.getInstructions(token)
        if (!response.isSuccessful) {
            throw Exception(response.errorBody()?.string() ?: "Instruction loading error: ${response.code()}")
        }

        val instructions = response.body().orEmpty()

        val instructionEntities = instructions.map {
            InstructionEntity(
                id = it.id,
                title = it.title,
                createdAt = it.createdAt,
                createdBy = it.createdBy,
                createdByName = it.createdByName,
                parentId = it.parentId,
                periodId = it.periodId,
                periodTitle = it.periodTitle,
                constraintPharmacy = it.constraintPharmacy
            )
        }

        val categoryEntities = instructions.flatMap { instruction ->
            instruction.categories.map { category ->
                InstructionCategoryEntity(
                    id = category.id,
                    instructionId = instruction.id,
                    title = category.title
                )
            }
        }

        val detailEntities = instructions.flatMap { instruction ->
            instruction.categories.flatMap { category ->
                val duplicateCounter = mutableMapOf<String, Int>()
                category.details.mapIndexed { index, detail ->
                    val signature = "${detail.id}_${detail.templateId}"
                    val duplicateIndex = duplicateCounter.getOrDefault(signature, 0)
                    duplicateCounter[signature] = duplicateIndex + 1
                    InstructionDetailEntity(
                        localId = "${category.id}_${detail.id}_${detail.templateId}_$duplicateIndex",
                        id = detail.id,
                        categoryId = category.id,
                        title = detail.title,
                        templateId = detail.templateId,
                        orderNumber = index
                    )
                }
            }
        }

        dao.clearInstructions()
        dao.insertInstructions(instructionEntities)
        dao.insertCategories(categoryEntities)
        dao.insertDetails(detailEntities)
    }
}
