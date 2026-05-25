package com.example.check_911

data class InstructionDetailUi(
    val localId: String,
    val id: String,
    val categoryId: String,
    val title: String,
    val templateId: String,
    val orderNumber: Int
)

sealed class InstructionUiItem {
    data class CategoryItem(
        val categoryId: String,
        val title: String
    ) : InstructionUiItem()

    data class DetailItem(
        val detail: InstructionDetailUi
    ) : InstructionUiItem()
}
