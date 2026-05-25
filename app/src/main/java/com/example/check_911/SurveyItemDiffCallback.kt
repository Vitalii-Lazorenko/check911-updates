package com.example.check_911

import androidx.recyclerview.widget.DiffUtil

class SurveyItemDiffCallback : DiffUtil.ItemCallback<SurveyItem>() {
    override fun areItemsTheSame(oldItem: SurveyItem, newItem: SurveyItem): Boolean {
        return when {
            oldItem is SurveyItem.CategoryItem && newItem is SurveyItem.CategoryItem ->
                oldItem.category.id == newItem.category.id

            oldItem is SurveyItem.QuestionItem && newItem is SurveyItem.QuestionItem ->
                oldItem.question.id == newItem.question.id

            else -> false
        }
    }

//    override fun areContentsTheSame(oldItem: SurveyItem, newItem: SurveyItem): Boolean {
//        return oldItem == newItem
//    }
override fun areContentsTheSame(oldItem: SurveyItem, newItem: SurveyItem): Boolean {
    return when {
        oldItem is SurveyItem.CategoryItem && newItem is SurveyItem.CategoryItem ->
            oldItem.category == newItem.category
        oldItem is SurveyItem.QuestionItem && newItem is SurveyItem.QuestionItem ->
            oldItem.question == newItem.question &&
                    oldItem.answers == newItem.answers &&
                    oldItem.options == newItem.options
        else -> false
    }
}

}
