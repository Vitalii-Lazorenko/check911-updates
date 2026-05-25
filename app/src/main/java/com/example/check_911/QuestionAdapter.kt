package com.example.check_911

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.check_911.data.db.dao.QuestionWithAnswers
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity

class QuestionAdapter(
    private var questions: List<QuestionWithAnswers>,
    private var userAnswers: List<SurveyAnswerEntity>,
    private val onQuestionClick: (QuestionEntity, Int) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTextView: TextView = itemView.findViewById(R.id.textViewQuestion)
        private val photoImageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        private val targetTextView: TextView = itemView.findViewById(R.id.textViewTarget)
        private val answerTextView: TextView = itemView.findViewById(R.id.textViewAnswer)


        fun bind(questionWithAnswers: QuestionWithAnswers, position: Int) {
            val question = questionWithAnswers.question
            val answers = questionWithAnswers.answers
            val questionId = question.id

            questionTextView.text = question.text

            photoImageView.visibility = if (question.requiredIfYes || question.requiredIfNo || question.alwaysRequired) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

            val correctAnswer = answers.find { it.isCorrect }
            targetTextView.text = correctAnswer?.text?.replaceFirstChar { it.uppercaseChar() } ?: ""
            targetTextView.setTextColor(Color.GRAY)

//            answerTextView.text = ""
// Ответ пользователя, если есть
            val userAnswer = userAnswers.find { it.questionId == questionId }
            answerTextView.text = userAnswer?.let {
                it.selectedAnswersText ?: it.numberAnswer?.toString()
                ?: it.percentAnswer?.toString()
                ?: it.textAnswer ?: ""
            } ?: ""

            // ✅ Подсвечиваем выбранный вопрос
            itemView.setBackgroundColor(if (selectedPosition == position) Color.YELLOW else Color.TRANSPARENT)

            itemView.setOnClickListener {
                selectedPosition = position
                notifyDataSetChanged()
                onQuestionClick(question, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position)
    }

    override fun getItemCount(): Int = questions.size

    // ✅ Функция для обновления выбранного вопроса при перелистывании
    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    // Обновить данные
    fun setData(newQuestions: List<QuestionWithAnswers>, newAnswers: List<SurveyAnswerEntity>) {
        this.questions = newQuestions
        this.userAnswers = newAnswers
        notifyDataSetChanged()
    }
}

