package com.example.check_911

import com.example.check_911.data.db.dao.QuestionWithAnswers
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity

sealed class SurveyItem {
    data class CategoryItem(val category: CategoryQuestionsEntity) : SurveyItem()
    data class QuestionItem(
        val question: QuestionEntity,
        val answers: List<SurveyAnswerEntity> = emptyList(),
        val options: List<OptionForQuestionsEntity> = emptyList()
    ) : SurveyItem()
}





