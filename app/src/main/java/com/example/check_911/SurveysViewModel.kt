package com.example.check_911

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.check_911.data.db.MainDb
import com.example.check_911.data.db.dao.ResultsSurveyDao
import com.example.check_911.data.db.dao.SurveyDao
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import com.example.check_911.data.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate


class SurveysViewModel(
    private val database: MainDb,
    private val state: SavedStateHandle,
    private val app: Application,
) : ViewModel() {

    private val surveyId: String = state["SURVEY_ID"] ?: ""
    private val surveyTitle: String = state["SURVEY_TITLE"] ?: "Опитування"

    private val _surveyItems = MutableStateFlow<List<SurveyItem>>(emptyList())
    val surveyItems: StateFlow<List<SurveyItem>> = _surveyItems.asStateFlow()

    val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _initialScrollIndex = MutableStateFlow(0)
    val initialScrollIndex: StateFlow<Int> = _initialScrollIndex.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    // 👉 фото до подтверждения
    private val _pendingPhotoPath = MutableStateFlow<String?>(null)
    val pendingPhotoPath: StateFlow<String?> = _pendingPhotoPath

    // 👉 основное фото, которое нужно сохранять между пересозданиями Activity
    private val KEY_PHOTO_PATH = "currentPhotoPath"
    val currentPhotoPath: MutableLiveData<String?> = state.getLiveData(KEY_PHOTO_PATH, null)

    val currentPhotoUri = MutableLiveData<String?>()

    // 👉 вопрос на который делается фото
    private val KEY_PHOTO_QUESTION_ID = "currentPhotoQuestionId"
    val currentPhotoQuestionId: MutableLiveData<String?> = state.getLiveData(KEY_PHOTO_QUESTION_ID, null)

    // в классе SurveysViewModel
    private val _updatedQuestion = MutableSharedFlow<Pair<String, List<SurveyAnswerEntity>>>(extraBufferCapacity = 16)
    val updatedQuestion = _updatedQuestion.asSharedFlow()




    init {
        loadSurveyData()
        AppLogger.log("Lifecycle", "SurveyViewModel создан")
    }

    override fun onCleared() {
        super.onCleared()
        AppLogger.log("Lifecycle", "SurveyViewModel уничтожен")
    }

    fun loadSurveyData() {
        viewModelScope.launch {
            try {
                val categoriesWithQuestions =
                    database.surveyDao().getCategoriesWithQuestionsAndAnswers(surveyId)
                val questionAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

                _surveyItems.value = categoriesWithQuestions.flatMap { catWithQuestions ->
                    val categoryItem = SurveyItem.CategoryItem(catWithQuestions.category)
                    val questionItems = catWithQuestions.questions.map {
                        val q = it.question
                        val options = it.answers
                        val answers = questionAnswers.filter { a -> a.questionId == q.id }
                        SurveyItem.QuestionItem(q, answers, options)
                    }
                    listOf(categoryItem) + questionItems
                }

                val firstCategoryIndex = _surveyItems.value.indexOfFirst { it is SurveyItem.CategoryItem }
                val firstQuestionIndex = _surveyItems.value.indexOfFirst { it is SurveyItem.QuestionItem }

                if (firstCategoryIndex != -1) {
                    _initialScrollIndex.value = firstCategoryIndex   // для RV scrollToPosition
                }
                if (firstQuestionIndex != -1) {
                    _currentQuestionIndex.value = firstQuestionIndex // для логики текущего вопроса
                }
            } catch (e: Exception) {
                _messages.emit("Помилка завантаження: ${e.message}")
            }
        }
    }

    fun selectQuestion(index: Int) {
        _currentQuestionIndex.value = index
    }

    fun scrollUp() {
        val visible = _surveyItems.value.mapIndexedNotNull { i, it -> if (it is SurveyItem.QuestionItem) i else null }
        val pos = visible.indexOf(_currentQuestionIndex.value)
        if (pos > 0) _currentQuestionIndex.value = visible[pos - 1]
    }

    fun scrollDown() {
        val visible = _surveyItems.value.mapIndexedNotNull { i, it -> if (it is SurveyItem.QuestionItem) i else null }
        val pos = visible.indexOf(_currentQuestionIndex.value)
        if (pos != -1 && pos < visible.size - 1) _currentQuestionIndex.value = visible[pos + 1]
    }

    fun saveAnswer(
        question: QuestionEntity,
        selectedOptionId: List<String>? = null,
        selectedOptionIds: List<String>? = null,
        selectedOptionTexts: List<String>? = null,
        number: Int? = null,
        percent: Double? = null,
        text: String? = null,
        comment: String? = null,
        photoPath: String? = null,
        skipped: Boolean = false,
        autoScrollDown: Boolean = false
    ) {
        viewModelScope.launch {
            val sharedPreferences = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val userId = sharedPreferences.getLong("selected_user_idGamma", -1L)

            val existingResult = database.resultsSurveyDao().getSurveyResult(surveyId).firstOrNull()
            if (existingResult == null) {
                database.resultsSurveyDao().insertSurveyResult(
                    SurveyResultEntity(surveyId, surveyTitle, userId, "draft")
                )
            }

            val existingAnswer = database.resultsSurveyDao().getAnswer(surveyId, question.id)
            val category = database.surveyDao().getCategoryById(question.categoryId)
            val categoryTitle = category?.description ?: "Без категорії"

            val updated = SurveyAnswerEntity(
                id = existingAnswer?.id ?: 0,
                surveyId = surveyId,
                categoryId = question.categoryId,
                categoryTitle = categoryTitle,
                questionId = question.id,
                questionText = question.text,
                selectedSingleAnswer = selectedOptionId?.joinToString(",") ?: existingAnswer?.selectedSingleAnswer,
                selectedMultiAnswers = selectedOptionIds?.joinToString(",") ?: existingAnswer?.selectedMultiAnswers,
                selectedAnswersText = selectedOptionTexts?.joinToString(",") ?: existingAnswer?.selectedAnswersText,
                numberAnswer = number ?: existingAnswer?.numberAnswer,
                percentAnswer = percent ?: existingAnswer?.percentAnswer,
                textAnswer = text ?: existingAnswer?.textAnswer,
                comment = comment ?: existingAnswer?.comment,
                photoPath = photoPath ?: existingAnswer?.photoPath,
                skipped = skipped
            )

            if (existingAnswer == null) database.resultsSurveyDao().insertAnswer(updated)
            else database.resultsSurveyDao().updateAnswer(updated)

//            refreshSurveyItems()
            // 🟡 запоминаем id вопроса
            val currentQuestionId = question.id

//            refreshSurveyItems()
            // получаем свежие ответы только для этого вопроса (в IO)
            val newAnswersForQuestion = withContext(Dispatchers.IO) {
                database.resultsSurveyDao().getAnswersForQuestion(surveyId, question.id)
            }

            // локально обновляем модель (если используешь частичное обновление списка)
            refreshSurveyItems(updatedQuestionId = question.id) // оставь/удали по необходимости

            // находим индекс в _surveyItems и восстанавливаем выделение
            val newIndex = _surveyItems.value.indexOfFirst {
                (it as? SurveyItem.QuestionItem)?.question?.id == question.id
            }
            if (newIndex != -1) {
                _currentQuestionIndex.value = newIndex
            }

            // эмитим событие, чтобы Activity обновил адаптер (non-suspending)
            _updatedQuestion.tryEmit(question.id to newAnswersForQuestion)

            // теперь safe scrollDown, если просили автоскролл — он использует текущую логику scrollDown()
            if (autoScrollDown) {
                scrollDown()
            }
        }
    }

//    08.10.25
//    private suspend fun refreshSurveyItems() {
//        val categoriesWithQuestions = database.surveyDao().getCategoriesWithQuestionsAndAnswers(surveyId)
//        val updatedAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//
//        _surveyItems.value = categoriesWithQuestions.flatMap { catWithQuestions ->
//            val catItem = SurveyItem.CategoryItem(catWithQuestions.category)
//            val questions = catWithQuestions.questions.map {
//                val q = it.question
//                val opts = it.answers
//                val ans = updatedAnswers.filter { a -> a.questionId == q.id }
//                SurveyItem.QuestionItem(q, ans, opts)
//            }
//            listOf(catItem) + questions
//        }
//    }

    suspend fun refreshSurveyItems(updatedQuestionId: String) {
        val updatedAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
        val newAnswersForQuestion = updatedAnswers.filter { it.questionId == updatedQuestionId }

        // 🔹 Обновляем только нужный вопрос в адаптере (без пересоздания списка)
        withContext(Dispatchers.Main) {
//            surveyAdapter.updateAnswerForQuestion(updatedQuestionId, newAnswersForQuestion)
        }
    }


    fun clearAnswer(question: QuestionEntity) {
        viewModelScope.launch {
            database.resultsSurveyDao().deleteSurveyAnswersForIdquestion(question.id)
//            refreshSurveyItems()
        }
    }

    fun onPhotoCaptured(path: String) {
        _pendingPhotoPath.value = path
    }


    fun confirmPhoto() {
        val questionId = currentPhotoQuestionId.value
        val path = currentPhotoPath.value

        AppLogger.log("PhotoDebug", "confirmPhoto  questionId: ${currentPhotoQuestionId.value}, path: ${currentPhotoPath.value}")
        if (questionId != null && path != null) {
            viewModelScope.launch {
                val question = database.surveyDao().getQuestionById(questionId)
                if (question != null) {
                    saveAnswer(question, photoPath = path, autoScrollDown = true)
                    AppLogger.log("PhotoDebug", "Фото привязано к вопросу ${question.id}", app)
//                    scrollDown()
                } else {
                    viewModelScope.launch { _messages.emit("⚠️ Питання з $questionId не знайдено") }
                }

                clearPhotoPath()
//            currentPhotoPath.value = null
//            currentPhotoQuestionId.value = null
            }
        } else {
            viewModelScope.launch { _messages.emit("Не вдалося зберегти фото") }
        }
    }

//    01.09.25
//    fun confirmPhoto() {
//        val items = _surveyItems.value
//        val index = _currentQuestionIndex.value
//        val question = (items.getOrNull(index) as? SurveyItem.QuestionItem)?.question
//
//        val path = _pendingPhotoPath.value
//        if (question != null && path != null) {
//            saveAnswer(question, photoPath = path)
//            scrollDown()
//            clearPhotoPath()
//            _pendingPhotoPath.value = null
//        } else {
//            viewModelScope.launch { _messages.emit("Не вдалося зберегти фото") }
//        }
//    }

    // 👉 Теперь с сохранением в SavedStateHandle
    fun setPhotoPath(path: String) {
        currentPhotoPath.value = path
        state[KEY_PHOTO_PATH] = path
        AppLogger.log("PhotoDebug", "Сохранение в SurveysViewModel setPhotoPath: $path")
    }

    fun setPhotoUri(uri: String) {
        currentPhotoUri.value = uri
        AppLogger.log("PhotoDebug", "✅ Збережено currentPhotoUri: $uri", null)
    }

    fun clearPhotoPath() {
        currentPhotoQuestionId.value = null
        currentPhotoPath.value = null
        state[KEY_PHOTO_PATH] = null
        AppLogger.log("PhotoDebug", "Очищен путь фото в ViewModel")
    }

    fun checkSurveyCompletion() {
        viewModelScope.launch {
            val all = _surveyItems.value.filterIsInstance<SurveyItem.QuestionItem>()
            val incomplete = mutableListOf<String>()

            for (item in all) {
                val ans = database.resultsSurveyDao().getAnswer(surveyId, item.question.id)
                val hasPhoto = ans?.photoPath?.isNotBlank() == true
                val requiresPhoto = item.question.alwaysRequired
                val ok = ans != null && (!requiresPhoto || hasPhoto)
                if (!ok) incomplete.add(item.question.id)
            }

            val today = LocalDate.now().toString() // например, "2025-09-26"
            if (incomplete.isEmpty()) {
//                database.resultsSurveyDao().updateSurveyStatus(surveyId, "ready")
                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "ready", today)
                _messages.emit("Опитування завершено!")
            } else {
//                database.resultsSurveyDao().updateSurveyStatus(surveyId, "draft")
                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "draft", today)
                _messages.emit("Є незаповнені питання")
            }
        }
    }

    suspend fun clearSurveyResults(surveyId: String, today: String) {
        withContext(Dispatchers.IO) {
            val answers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

            // удаляем фотофайлы
            answers.forEach { answer ->
                answer.photoPath?.let {
                    val file = File(it)
                    if (file.exists()) file.delete()
                }
            }

            // удаляем только ответы
            database.resultsSurveyDao().deleteSurveyAnswers(surveyId)

            // сбрасываем статус и дату
            database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "draft", today)

            AppLogger.log(
                "SurveyViewModel",
                "Автоматическое очищение результатов для ежедневного опросника",
                app
            )
        }
    }
}



