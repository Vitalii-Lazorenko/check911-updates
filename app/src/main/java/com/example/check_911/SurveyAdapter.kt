package com.example.check_911

import android.animation.ObjectAnimator
import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.check_911.data.db.dao.CategoryWithQuestions
import com.example.check_911.data.db.dao.QuestionWithAnswers
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.SurveyItem
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import androidx.recyclerview.widget.ListAdapter
import android.graphics.Typeface
import androidx.camera.core.processing.SurfaceProcessorNode.In


// Вспомогательная модель для группировки
//data class CategoryWithQuestions(
//    val categoryDescription: String,
//    val questions: List<QuestionWithAnswers>
//)

//sealed class SurveyItem {
//    data class CategoryItem(val title: String) : SurveyItem()
//    data class QuestionItem(val question: QuestionWithAnswers) : SurveyItem()
//}

class SurveyAdapter(
    private val userAnswers: List<SurveyAnswerEntity>,
    private val onQuestionClick: (QuestionEntity, Int) -> Unit,
    private val onAnswerSelected: (QuestionEntity, String, String, Int) -> Unit, // новое для вывода и дачи ответов в графе ответы
    private val onPhotoClick: (QuestionEntity, Int) -> Unit // для фото по нажатию на значок
) : ListAdapter<SurveyItem, RecyclerView.ViewHolder>(SurveyItemDiffCallback()) {

    private val originalItems = mutableListOf<SurveyItem>()
    private val expandedCategories = mutableSetOf<String>()
    private var selectedPosition = RecyclerView.NO_POSITION
    private val incompleteQuestionIds = mutableSetOf<String>()
    private var searchQuery: String? = null
    private val filteredItems = mutableListOf<SurveyItem>()
    private var selectedQuestionId: String? = null






    companion object {
        private const val CATEGORY_VIEW_TYPE = 0
        private const val QUESTION_VIEW_TYPE = 1
    }

//    fun setItems(surveyItems: List<SurveyItem>) {
//        originalItems.clear()
//        originalItems.addAll(surveyItems)
//
//        surveyItems.forEach {
//            if (it is SurveyItem.CategoryItem) {
//                expandedCategories.add(it.category.id)
//            }
//        }
//
//        updateItems()
//        notifyDataSetChanged()
//    }

    fun setItems(surveyItems: List<SurveyItem>) {
        originalItems.clear()

        // 1. Разделяем на категории и вопросы
        val categories = surveyItems.filterIsInstance<SurveyItem.CategoryItem>()
            .sortedBy { it.category.orderNumber }

        val questions = surveyItems.filterIsInstance<SurveyItem.QuestionItem>()

        // 2. Строим итоговый список
        val sortedItems = mutableListOf<SurveyItem>()

        categories.forEach { categoryItem ->
            sortedItems.add(categoryItem)

            val categoryQuestions = questions
                .filter { it.question.categoryId == categoryItem.category.id }
                .sortedBy { it.question.orderNumber }

            sortedItems.addAll(categoryQuestions)

            // раскрываем категории по умолчанию
            expandedCategories.add(categoryItem.category.id)
        }

        // 3. Обновляем адаптер
        originalItems.addAll(sortedItems)
        updateItems()
        notifyDataSetChanged()
    }




    private fun updateItems() {
        val updatedItems = mutableListOf<SurveyItem>()

        for (item in originalItems) {
            when (item) {
                is SurveyItem.CategoryItem -> {
//                    if (expandedCategories.contains(item.category.id)) {
                        updatedItems.add(item)
//                    }
                }
                is SurveyItem.QuestionItem -> {
                    val questionText = item.question.text.lowercase()
                    val query = searchQuery?.lowercase()

                    val matches = query != null && query.length >= 3 && query in questionText

                    val include = when {
                        query.isNullOrBlank() -> expandedCategories.contains(item.question.categoryId)
                        matches -> true
                        else -> false
                    }

                    if (include) {
                        updatedItems.add(item)
                    }
                }
            }
        }

        filteredItems.clear()
        filteredItems.addAll(updatedItems)

        submitList(filteredItems) {
            notifyDataSetChanged()
        }
    }


    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SurveyItem.CategoryItem -> CATEGORY_VIEW_TYPE
            is SurveyItem.QuestionItem -> QUESTION_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            CATEGORY_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }

            QUESTION_VIEW_TYPE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_question, parent, false)
                QuestionViewHolder(view)
            }

            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SurveyItem.CategoryItem -> (holder as CategoryViewHolder).bind(item.category)
            is SurveyItem.QuestionItem -> (holder as QuestionViewHolder).bind(
                item.question, item.answers, item.options, position
            )
        }
    }

//    для анимации появления/исчезновения вопросов
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (holder is QuestionViewHolder) {
            holder.animateIn()
        }
        super.onViewAttachedToWindow(holder)

    }


    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.textViewCategoryTitle)
        private val arrow: ImageView = itemView.findViewById(R.id.imageViewArrow)

        fun bind(category: CategoryQuestionsEntity) {
            title.text = category.description

            val isExpanded = expandedCategories.contains(category.id)
            arrow.setColorFilter(Color.BLACK)
            arrow.rotation = if (isExpanded) 90f else 0f

            itemView.setOnClickListener {
                val categoryId = category.id
                val shouldExpand = !expandedCategories.contains(categoryId)

                if (shouldExpand) {
                    expandedCategories.add(categoryId)
                    animateArrowRotation(arrow, 0f, 90f)
                    updateItems()
                } else {
                    expandedCategories.remove(categoryId)
                    updateItems()

                    // Проверяем после обновления списка!
                    val collapsedQuestions = originalItems.filterIsInstance<SurveyItem.QuestionItem>()
                        .filter { it.question.categoryId == categoryId }
                        .map { it.question.id }
                        .toSet()

                    val selectedQuestionId = (currentList.getOrNull(selectedPosition) as? SurveyItem.QuestionItem)?.question?.id

                    if (selectedQuestionId in collapsedQuestions) {
                        // Текущий вопрос скрыт — выбрать новый видимый вопрос
                        val newSelection = currentList.indexOfFirst { it is SurveyItem.QuestionItem }
                        if (newSelection != -1) {
                            selectQuestionAt(newSelection)
                            (itemView.parent as? RecyclerView)?.smoothScrollToPosition(newSelection)
                        } else {
                            // Нет доступных вопросов
                            selectedPosition = RecyclerView.NO_POSITION
                        }
                    }
                    animateArrowRotation(arrow, 90f, 0f)
                }
            }

        }

        private fun animateArrowRotation(arrow: ImageView, from: Float, to: Float) {
            val rotate = ObjectAnimator.ofFloat(arrow, View.ROTATION, from, to)
            rotate.duration = 300
            rotate.interpolator = DecelerateInterpolator()
            rotate.start()
        }
    }


    inner class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val questionTextView: TextView = itemView.findViewById(R.id.textViewQuestion)
        private val photoImageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)
        private val photoCommentView: ImageView = itemView.findViewById(R.id.imageViewComment)
        private val targetTextView: TextView = itemView.findViewById(R.id.textViewTarget)
        private val answerTextView: TextView = itemView.findViewById(R.id.textViewAnswer)



        fun bind(
            question: QuestionEntity,
            answers: List<SurveyAnswerEntity>,
            options: List<OptionForQuestionsEntity>,
            position: Int
        ) {
//            questionTextView.text = question.text
            if (!searchQuery.isNullOrBlank()) {
                val pattern = Regex("(${Regex.escape(searchQuery!!).replace(" ", "\\s+")})", RegexOption.IGNORE_CASE)
                val spannable = SpannableString(question.text)
                pattern.findAll(question.text).forEach {
                    spannable.setSpan(
                        BackgroundColorSpan(Color.parseColor("#FFFF9800")),
                        it.range.first,
                        it.range.last + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                questionTextView.text = spannable
            } else {
                questionTextView.text = question.text
            }

            val userAnswer = answers.firstOrNull()
            val hasPhoto = userAnswer?.photoPath?.isNotBlank() == true
//            val userText = userAnswer?.textAnswer?.trim()
            val userText = userAnswer?.selectedAnswersText?.trim()
            val skipped = userAnswer?.skipped == true

            // ====== ЗНАЧОК ФОТО (цвет и условие) ======
            val showPhoto = question.requiredIfYes || question.requiredIfNo || question.alwaysRequired
            photoImageView.visibility = if (showPhoto) View.VISIBLE else View.INVISIBLE
            if (showPhoto) {
                photoImageView.setOnClickListener {
                    onPhotoClick(question, position) // вызвать камеру
                }
            } else {
                photoImageView.setOnClickListener(null)
            }


            if (showPhoto) {
                val needsPhoto = when {
                    question.alwaysRequired -> true
                    question.requiredIfYes && userText.equals("так", ignoreCase = true) -> true
                    question.requiredIfNo && userText.equals("ні", ignoreCase = true) -> true
                    else -> false
                }

                val color = when {
                    hasPhoto -> Color.BLUE
                    needsPhoto -> Color.RED
                    else -> Color.GRAY
                }
                photoImageView.setColorFilter(color)
            }

            // ====== КОММЕНТАРИЙ ======
            photoCommentView.visibility = if (userAnswer?.comment?.isNotBlank() == true) View.VISIBLE else View.INVISIBLE

            // ====== ПРАВИЛЬНЫЙ ОТВЕТ (целевой) ======
            val correctAnswer = options.find { it.isCorrect }
            targetTextView.text = correctAnswer?.text?.replaceFirstChar { it.uppercaseChar() } ?: ""
            targetTextView.setTextColor(Color.GRAY)

            // ====== ОТВЕТ ПОЛЬЗОВАТЕЛЯ ======
            val answerText = when {
                skipped -> "NA"
                userAnswer != null -> {
                    val rawText = userAnswer.selectedAnswersText
                        ?: userAnswer.numberAnswer?.toString()
                        ?: userAnswer.percentAnswer?.toString()
                        ?: userAnswer.textAnswer ?: ""

                    rawText.replaceFirstChar { it.uppercaseChar() }
                }
                else -> ""
            }
//            answerTextView.text = answerText
            if (userAnswer?.selectedAnswersText == null && question.singleChoiceInput) {
                val context = itemView.context
                val spannableBuilder = SpannableStringBuilder()

                options.forEachIndexed { index, option ->
                    val optionText = option.text.replaceFirstChar { it.uppercaseChar() } // 🔠 С заглавной
                    val optionId = option.id

                    val start = spannableBuilder.length
                    spannableBuilder.append(optionText)
                    val end = spannableBuilder.length

                    spannableBuilder.setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onAnswerSelected(question, optionId, optionText, position)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.color = Color.parseColor("#3F51B5")
                            ds.isUnderlineText = false
                            ds.typeface = Typeface.DEFAULT_BOLD
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                    // Добавляем перенос строки после каждого варианта, кроме последнего
                    if (index != options.lastIndex) {
                        spannableBuilder.append("\n")
                    }
                }

                answerTextView.text = spannableBuilder
                answerTextView.movementMethod = LinkMovementMethod.getInstance()
                answerTextView.highlightColor = Color.TRANSPARENT
            }
            else {
                answerTextView.text = answerText
            }


            // ====== ЦВЕТ ФОНА ОТВЕТА ======
//            val correctOptionId = options.find { it.isCorrect }?.id
//            val userAnswerId = userAnswer?.selectedSingleAnswer
//
//            val backgroundRes = when {
//                userAnswer?.skipped == true || userAnswerId.isNullOrBlank() -> R.drawable.answer_background_default
//                correctOptionId != null && userAnswerId.trim() == correctOptionId.trim() -> R.drawable.answer_background_correct
//                correctOptionId != null -> R.drawable.answer_background_incorrect
//                else -> R.drawable.answer_background_default
//            }
//
//            answerTextView.setBackgroundResource(backgroundRes)
            // ====== ЦВЕТ ФОНА ОТВЕТА ======

            val correctOptionIds = options
                .filter { it.isCorrect }
                .map { it.id.trim() }

            val userAnswerId = userAnswer?.selectedSingleAnswer?.trim()

            val backgroundRes = when {
                userAnswer?.skipped == true || userAnswerId.isNullOrBlank() ->
                    R.drawable.answer_background_default

                correctOptionIds.contains(userAnswerId) ->
                    R.drawable.answer_background_correct

                correctOptionIds.isNotEmpty() ->
                    R.drawable.answer_background_incorrect

                else ->
                    R.drawable.answer_background_default
            }

            answerTextView.setBackgroundResource(backgroundRes)



            // ====== ВЫДЕЛЕНИЕ ВЫБРАННОГО ======
//            itemView.setBackgroundColor(if (selectedPosition == position) Color.YELLOW else Color.TRANSPARENT)
// ====== ВЫДЕЛЕНИЕ ВОПРОСА ======
            when {
                selectedPosition == position -> {
                    itemView.setBackgroundColor(Color.parseColor("#FCFEBB")) // выбранный вопрос
                }
                incompleteQuestionIds.contains(question.id) -> {
                    itemView.setBackgroundColor(Color.parseColor("#FFEBEE")) // светло-красный для незаполненных
                }
                else -> {
                    itemView.setBackgroundColor(Color.TRANSPARENT) // фон по умолчанию
                }
            }


            itemView.setOnClickListener {
                val previous = selectedPosition
                selectedPosition = position
                if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
                notifyItemChanged(position)
                onQuestionClick(question, position)
            }
        }


        //        настройка анимации появления/исчезновения вопросов
fun animateIn() {
    itemView.alpha = 0f
    itemView.translationY = 0f // сдвигаем вверх, а не вниз
    itemView.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(250)
        .setInterpolator(DecelerateInterpolator())
        .start()
}


    }

    fun setSelectedPosition(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    fun updateAnswerForQuestion(questionId: String, newAnswers: List<SurveyAnswerEntity>) {
        val index = currentList.indexOfFirst {
            it is SurveyItem.QuestionItem && it.question.id == questionId
        }

        if (index != -1) {
            val oldItem = currentList[index] as SurveyItem.QuestionItem

            // 👇 Создаём новый список с копиями (гарантированно другие ссылки)
            val freshAnswers = newAnswers.map { it.copy() }

            val updatedItem = oldItem.copy(answers = freshAnswers)
            val newList = currentList.toMutableList()
            newList[index] = updatedItem

            // Проверим, заполнен ли вопрос
            val answer = newAnswers.firstOrNull()
            val hasAnswer = answer != null && (
                    !answer.selectedSingleAnswer.isNullOrBlank() ||
                            !answer.selectedMultiAnswers.isNullOrBlank() ||
                            answer.numberAnswer != null ||
                            answer.percentAnswer != null ||
                            !answer.textAnswer.isNullOrBlank() ||
                            answer.skipped
                    )
            if (hasAnswer) {
                incompleteQuestionIds.remove(questionId)
            }

            submitList(newList)
            notifyItemChanged(index) // ✅ Явно принудим обновление
        }
    }

    fun highlightIncompleteQuestions(ids: List<String>) {
        incompleteQuestionIds.clear()
        incompleteQuestionIds.addAll(ids)
        notifyDataSetChanged()
    }

    fun filterByQuery(query: String) {
        searchQuery = query.trim()
        updateItems()
        notifyDataSetChanged()
    }

    fun clearSearch() {
        searchQuery = null
        updateItems()
        notifyDataSetChanged()
    }

    private fun findNearestVisibleQuestion(fromIndex: Int): Int? {
        // Поиск вниз
        for (i in fromIndex + 1 until currentList.size) {
            if (currentList[i] is SurveyItem.QuestionItem) return i
        }

        // Поиск вверх
        for (i in fromIndex - 1 downTo 0) {
            if (currentList[i] is SurveyItem.QuestionItem) return i
        }

        return null
    }

    // Новый метод: правильно выбирать вопрос и синхронизировать выделение
    fun selectQuestionAt(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
        if (selectedPosition != RecyclerView.NO_POSITION) notifyItemChanged(selectedPosition)

        val item = currentList.getOrNull(selectedPosition)
        if (item is SurveyItem.QuestionItem) {
            onQuestionClick(item.question, selectedPosition)
        }
    }

}

