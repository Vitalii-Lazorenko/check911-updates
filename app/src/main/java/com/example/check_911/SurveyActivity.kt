package com.example.check_911

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ConcatAdapter
import com.example.check_911.data.db.dao.QuestionWithAnswers
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.ActivityResultLauncher
import com.example.check_911.data.utils.AppLogger
import android.provider.Settings
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.StatFs
import android.provider.MediaStore
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import java.io.FileOutputStream
import java.time.LocalDate


enum class FilterType {
    COMPLETED, INCOMPLETE, ALL, WRONG_ANSWERS
}

class SurveyActivity : AppCompatActivity() {
//    private val viewModel: SurveysViewModel by viewModels()
private val viewModel: SurveysViewModel by viewModels {
    val db = (application as App).database
    SurveysViewModelFactory(db, application, this, intent.extras)
}
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: Toolbar
    private lateinit var commentEditText: EditText
    private var currentCommentWatcher: TextWatcher? = null

    private lateinit var surveyAdapter: SurveyAdapter
    private var surveyItems: List<SurveyItem> = emptyList() // для навигации и обновления данных


    private var currentQuestionIndex: Int = 0 // Индекс текущего вопроса
//    private lateinit var questions: List<QuestionWithAnswers>
    private var currentPhotoUri: Uri? = null

    private val database by lazy { (application as App).database }

//    private var currentPhotoPath: String? = null

//    14.08.25
//    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//        if (success && currentPhotoPath != null) {
////            showPhotoPreviewDialog(currentPhotoPath!!)
////            waitForPhotoReadyAndPreview(currentPhotoPath!!)
//            val file = File(currentPhotoPath!!)
//            if (file.exists() && file.length() > 0) {
//                AppLogger.log("PhotoDebug", "✅ Фото отримано: $currentPhotoPath", this)
//
//                val question = (surveyItems[currentQuestionIndex] as? SurveyItem.QuestionItem)?.question
//                if (question != null) {
//                    saveAnswer(question, photoPath = currentPhotoPath!!)
//                    scrollDown()
//                }
//            } else {
//                AppLogger.log("PhotoDebug", "❌ Фотофайл не існує або пустий", this)
//                Toast.makeText(this, "Не вдалося зберегти фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
//            }
//        } else {
//        AppLogger.log("PhotoDebug", "❌ Користувач скасував фото", this)
//    }
//    }


//    21.08.25
//    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//        if (success && currentPhotoPath != null) {
//            val file = File(currentPhotoPath!!)
//            var attempts = 0
//            var exists = file.exists()
//            var size = file.length()
//
//            // Ждём до 1 секунды пока файл появится
//            while ((!exists || size == 0L) && attempts < 5) {
//                Thread.sleep(200)
//                attempts++
//                exists = file.exists()
//                size = file.length()
//            }
//
//            AppLogger.log("PhotoDebug", "Результат камери success=$success, path=$currentPhotoPath, exists=$exists, size=$size, attempts=$attempts", this)
//
//            if (exists && size > 0) {
////                val question = (surveyItems[currentQuestionIndex] as? SurveyItem.QuestionItem)?.question
//                val question = (surveyAdapter.currentList[viewModel.currentQuestionIndex.value] as? SurveyItem.QuestionItem)?.question
//
//                if (question != null) {
//                    AppLogger.log("PhotoDebug", "✅ Фото збережено як відповідь", this)
//                    viewModel.saveAnswer(question, photoPath = currentPhotoPath!!)
////                    scrollDown()
//                    viewModel.scrollDown()
//                }
//            } else {
//                AppLogger.log("PhotoDebug", "❌ Фотофайл не існує або пустий", this)
//                Toast.makeText(this, "Не вдалося зберегти фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
//            }
//        } else {
//            AppLogger.log("PhotoDebug", "❌ Користувач скасував фото", this)
//        }
//    }


////    12.09.25
//    private val takePictureLauncher =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            val path = viewModel.currentPhotoPath.value
//            if (path == null) {
//                AppLogger.log("PhotoDebug", "currentPhotoPath == null после viewModel", this)
//                Toast.makeText(this, "Помилка: шлях до фото відсутній", Toast.LENGTH_LONG).show()
//                return@registerForActivityResult
//            }
//
//            val file = File(path)
//            val exists = file.exists()
//            val size = file.length()
//
//            AppLogger.log(
//                "PhotoDebug",
//                "Результат камери success=$success, path=$path, exists=$exists, size=$size",
//                this
//            )
//
//            if (success && exists && size > 0) {
//                // ✅ Фото реально есть → сохраняем
//                viewModel.onPhotoCaptured(path)
//                viewModel.confirmPhoto()
////                viewModel.clearPhotoPath() // очищаем после сохранения
//            } else {
//                if (!success) {
//                    // ❌ Пользователь отменил
//                    AppLogger.log("PhotoDebug", "Користувач скасував фото, success = $success, exists = $exists, size = $size", this)
////                Toast.makeText(this, "Користувач скасував фото", Toast.LENGTH_SHORT).show()
//                } else {
//                    // ❌ Камера вернула success, но файл пустой → ошибка камеры
//                    Toast.makeText(this, "Не вдалося зберегти фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }

//    private val takePictureLauncher =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            val path = viewModel.currentPhotoPath.value
//            if (path == null) {
//                AppLogger.log("PhotoDebug", "currentPhotoPath == null после viewModel", this)
//                Toast.makeText(this, "Помилка: шлях до фото відсутній", Toast.LENGTH_LONG).show()
//                return@registerForActivityResult
//            }
//
//            val file = File(path)
//            val exists = file.exists()
//            val size = file.length()
//
//            AppLogger.log(
//                "PhotoDebug",
//                "Результат камери success=$success, path=$path, exists=$exists, size=$size",
//                this
//            )
//
//            if (success && exists && size > 0) {
//                // ✅ Определяем текущий вопрос
//                val qId = viewModel.currentPhotoQuestionId.value
//                val currentItem = viewModel.surveyItems.value.find {
//                    it is SurveyItem.QuestionItem && it.question.id == qId
//                } as? SurveyItem.QuestionItem
//
////                val noCompression = currentItem?.question?.noCompression ?: false
//                val noCompression = false
//
//                // ✅ Сжимаем при необходимости
//                val finalPhotoPath = compressImageIfNeeded(path, noCompression)
//
//                // ✅ Сохраняем в ViewModel
//                viewModel.onPhotoCaptured(finalPhotoPath)
//                viewModel.confirmPhoto()
//
//            } else {
//                if (!success) {
//                    AppLogger.log(
//                        "PhotoDebug",
//                        "Користувач скасував фото, success = $success, exists = $exists, size = $size",
//                        this
//                    )
//                } else {
//                    Toast.makeText(this, "Не вдалося зберегти фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
//                }
//            }
//        }



    private fun compressImageIfNeeded(originalPath: String, noCompression: Boolean): String {
        val originalFile = File(originalPath)
        val originalSizeKb = originalFile.length() / 1024
        val originalSizeMb = originalSizeKb / 1024.0

        if (noCompression) {
            AppLogger.log(
                "PhotoDebug",
                "Фото сохранено БЕЗ сжатия: $originalPath (size=${originalSizeKb}KB / ${"%.2f".format(originalSizeMb)}MB)",
                this
            )
            return originalPath
        }

        val bitmap = BitmapFactory.decodeFile(originalPath)
        val compressedFile = File(originalPath.replace(".jpg", "_compressed.jpg"))

        FileOutputStream(compressedFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out) // 50% качество
        }

        val compressedSizeKb = compressedFile.length() / 1024
        val compressedSizeMb = compressedSizeKb / 1024.0

        AppLogger.log(
            "PhotoDebug",
            "Фото сжато: ${compressedFile.absolutePath} " +
                    "До=${originalSizeKb}KB / ${"%.2f".format(originalSizeMb)}MB → " +
                    "После=${compressedSizeKb}KB / ${"%.2f".format(compressedSizeMb)}MB",
            this
        )

        return compressedFile.absolutePath
    }



    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Потрібен дозвіл на камеру", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>


    // для поиска
    private lateinit var searchEditText: EditText
    private lateinit var searchBarContainer: View
    private lateinit var clearSearchButton: ImageButton

    private var currentFilter: FilterType = FilterType.ALL



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

        // Инициализация Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        commentEditText = findViewById(R.id.editTextComment)
        commentEditText.filters = arrayOf(InputFilter.LengthFilter(1024))


        // Получаем название опроса из Intent
        val surveyName = intent.getStringExtra("SURVEY_TITLE") ?: "Опитування"
        supportActionBar?.title = surveyName

        recyclerView = findViewById(R.id.recyclerViewSurvey)

        AppLogger.log("Lifecycle", "SurveyActivity onCreate. savedInstanceState=$savedInstanceState", this)

////       инициализируем пустой адаптер
//        surveyAdapter = SurveyAdapter(
//            userAnswers = emptyList(),
//            onQuestionClick = { _, _ -> },
//            onAnswerSelected = { _, _, _ -> }
//        )
//        recyclerView.adapter = surveyAdapter
        // 👉 пустой адаптер при старте
        surveyAdapter = SurveyAdapter(
            userAnswers = emptyList(),
            onQuestionClick = { question, position ->
                viewModel.selectQuestion(position)
                updateBottomBar(question)
            },
            onAnswerSelected = { question, optionId, optionText, position ->
                viewModel.selectQuestion(position)
                viewModel.saveAnswer(
                    question = question,
                    selectedOptionId = listOf(optionId),
                    selectedOptionTexts = listOf(optionText),
                    autoScrollDown = true
                )
//                viewModel.scrollDown()
            },
            onPhotoClick = { question, position ->
                // ✅ фиксируем выбранный вопрос
                viewModel.selectQuestion(position)
                // ✅ устанавливаем, какой вопрос связан с фото
                viewModel.currentPhotoQuestionId.value = question.id
                // ✅ открываем камеру (твоя функция)
                openCamera()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = surveyAdapter



        var firstLoadDone = false
        // 👉 подписка на данные из ViewModel
        lifecycleScope.launchWhenStarted {
            viewModel.surveyItems.collect { items ->
                if (items.isNotEmpty()) {
                    surveyAdapter.setItems(items)

                    if (!firstLoadDone) {
                        // если ещё не выбрано - выделяем первый вопрос
                        val firstQuestionIndex =
                            items.indexOfFirst { it is SurveyItem.QuestionItem }
                        if (firstQuestionIndex != -1 && viewModel.currentQuestionIndex.value == 0) {
                            val firstQuestion =
                                (items[firstQuestionIndex] as SurveyItem.QuestionItem).question
                            updateBottomBar(firstQuestion)
                            surveyAdapter.setSelectedPosition(firstQuestionIndex)
                            viewModel.selectQuestion(firstQuestionIndex) // фиксируем выбранный вопрос
                        }

                        // 👉 скроллим не на вопрос, а на категорию
                        val firstCategoryIndex =
                            items.indexOfFirst { it is SurveyItem.CategoryItem }
                        if (firstCategoryIndex != -1) {
                            recyclerView.scrollToPosition(firstCategoryIndex)
                        }

                        firstLoadDone = true // ✅ теперь не будет вызываться при обновлениях
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.updatedQuestion.collect { (questionId, answers) ->
                surveyAdapter.updateAnswerForQuestion(questionId, answers)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.currentQuestionIndex.collect { index ->
                if (index in surveyAdapter.currentList.indices) {
                    val q = (surveyAdapter.currentList[index] as? SurveyItem.QuestionItem)?.question
                    if (q != null) {
                        updateBottomBar(q)
                        surveyAdapter.setSelectedPosition(index)

                        // 🔥 Плавный скролл к выбранному вопросу
                        recyclerView.post {
                            recyclerView.smoothScrollToPosition(index)
                        }
                        // ⚠️ здесь убираем scrollToPosition(index), иначе он снова проскроллит на вопрос
                        // recyclerView.scrollToPosition(index) ← удалить
                    }
                }
            }
        }


        lifecycleScope.launchWhenStarted {
            viewModel.messages.collect { msg ->
                Toast.makeText(this@SurveyActivity, msg, Toast.LENGTH_LONG).show()
            }
        }


        // Подключаем кнопки навигации
//        findViewById<ImageButton>(R.id.btnArrowUp).setOnClickListener { scrollUp() }
//        findViewById<ImageButton>(R.id.btnArrowDown).setOnClickListener { scrollDown() }
        findViewById<ImageButton>(R.id.btnArrowUp).setOnClickListener { viewModel.scrollUp() }
        findViewById<ImageButton>(R.id.btnArrowDown).setOnClickListener { viewModel.scrollDown() }

        viewModel.loadSurveyData()

        //
        searchBarContainer = findViewById(R.id.searchBarContainer)
        searchEditText = findViewById(R.id.editTextSearch)
//        clearSearchButton = findViewById(R.id.btnClearSearch)

//        clearSearchButton.setOnClickListener {
//            searchEditText.setText("")
////            searchBarContainer.visibility = View.GONE
//            surveyAdapter.clearSearch()
//        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {

                if (!::surveyAdapter.isInitialized) {
                    AppLogger.log("SurveyDebug", "Адаптер ещё не инициализирован — пропускаем фильтрацию", this@SurveyActivity)
                    return
                }

                val query = s?.toString() ?: ""
                if (query.length >= 3) {
                    surveyAdapter.filterByQuery(query)
//                    surveyAdapter.notifyDataSetChanged()
                } else {
                    surveyAdapter.clearSearch()
//                    surveyAdapter.notifyDataSetChanged()
                }
            }
        })

        searchEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = 2 // индекс drawableEnd (справа)
                val drawable = searchEditText.compoundDrawables[drawableEnd]

                if (drawable != null && event.rawX >= (searchEditText.right - drawable.bounds.width() - searchEditText.paddingEnd)) {
                    searchEditText.text.clear() // очищаем поле
                    surveyAdapter.clearSearch()
                    return@setOnTouchListener true
                }
            }
            false
        }

        //
//
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            AppLogger.log("Permissions", "Результат запиту дозволів: $permissions", this)

            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

            if (cameraGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Потрібен дозвіл на камеру", Toast.LENGTH_LONG).show()
            }
        }
//
//42 5 33 15 13 50

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AppLogger.log("Lifecycle", "SurveyActivity onSaveInstanceState. currentPhotoPath=${viewModel.currentPhotoPath.value}", this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.survey_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Закрываем активити при нажатии "Назад"
                true
            }
            R.id.action_finish -> {
                // TODO: Сохранить результаты и завершить опрос
                checkSurveyCompletion()

//                finish()
                true
            }
            R.id.action_options -> {
                val view = findViewById<View>(R.id.action_options)
                showSurveyOptionsMenu(view)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSurveyOptionsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.survey_options_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_clear_answers -> {
                    confirmClearAnswers()
                    true
                }
                R.id.menu_delete_survey -> {
                    confirmDeleteSurvey()
                    true
                }
//                R.id.menu_search -> {
//                    searchBarContainer.visibility = View.VISIBLE
//                    searchEditText.requestFocus()
//                    true
//                }
                R.id.menu_search -> {
                    val isVisible = searchBarContainer.visibility == View.VISIBLE
                    if (isVisible) {
                        // Если уже показан — скрываем и очищаем
                        searchBarContainer.visibility = View.GONE
                        searchEditText.text.clear()
                        surveyAdapter.clearSearch()

                        // Скрыть клавиатуру
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
                    } else {
                        // Если скрыт — показать и сфокусировать
                        searchBarContainer.visibility = View.VISIBLE
                        searchEditText.requestFocus()

                        // Показать клавиатуру
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
                    }
                    true
                }

                // 🎯 Обработка фильтров:
                R.id.filter_completed -> {
                    applyFilter(FilterType.COMPLETED)
                    true
                }

                R.id.filter_incomplete -> {
                    applyFilter(FilterType.INCOMPLETE)
                    true
                }

                R.id.filter_all -> {
                    applyFilter(FilterType.ALL)
                    true
                }

                R.id.filter_wrong_answers -> {
                    applyFilter(FilterType.WRONG_ANSWERS)
                    true
                }

                else -> super.onOptionsItemSelected(item)
//                else -> false
            }
        }

        popup.show()
    }


    private fun confirmClearAnswers() {
        AlertDialog.Builder(this)
            .setTitle("Очистити відповіді?")
            .setMessage("Всі відповіді, включно з фото, буде видалено.")
            .setPositiveButton("Так") { _, _ -> clearAllAnswersWithPhotos() }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun confirmDeleteSurvey() {
        AlertDialog.Builder(this)
            .setTitle("Видалити опитування?")
            .setMessage("Обережно! Всі питання, відповіді та фото буде видалено.")
            .setPositiveButton("Видалити") { _, _ -> deleteSurveyCompletely() }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun deleteSurveyCompletely() {
        lifecycleScope.launch {
            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch

            val answers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
            answers.forEach { answer ->
                answer.photoPath?.let {
                    val file = File(it)
                    if (file.exists()) file.delete()
                }
            }

            database.resultsSurveyDao().deleteSurvey(surveyId)
            database.surveyDao().deleteSurveyById(surveyId)

            AppLogger.log("SurveyActivity", "Користувач видалив опитування ${surveyId} и результати", this@SurveyActivity)
            Toast.makeText(this@SurveyActivity, "Опитування видалено", Toast.LENGTH_SHORT).show()
            finish() // Закрыть активити
        }
    }


    private fun clearAllAnswersWithPhotos() {
        lifecycleScope.launch {
            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch

            val answers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

            // Удалим фотофайлы
            answers.forEach { answer ->
                answer.photoPath?.let {
                    val file = File(it)
                    if (file.exists()) file.delete()
                }
            }

            // Удалим только ответы, не сам опросник
            database.resultsSurveyDao().deleteSurveyAnswers(surveyId)

            AppLogger.log("SurveyActivity", "Користувач очистив відповіді на всі питання", this@SurveyActivity)
            Toast.makeText(this@SurveyActivity, "Всі відповіді очищено", Toast.LENGTH_SHORT).show()

            // Обновим UI
            refreshSurveyItems()
            surveyAdapter.notifyDataSetChanged()
        }
    }

    private fun applyFilter(type: FilterType) {
        currentFilter = type

//        val questionItems = surveyItems.filterIsInstance<SurveyItem.QuestionItem>()
        val questionItems = surveyAdapter.currentList.filterIsInstance<SurveyItem.QuestionItem>()
        val filtered = when (type) {
            FilterType.ALL -> questionItems
            FilterType.COMPLETED -> questionItems.filter {
                val ans = it.answers.firstOrNull()
                ans != null && (
                        !ans.selectedSingleAnswer.isNullOrBlank() ||
                                !ans.selectedMultiAnswers.isNullOrBlank() ||
                                ans.numberAnswer != null ||
                                ans.percentAnswer != null ||
                                !ans.textAnswer.isNullOrBlank() ||
                                ans.skipped || ans.photoPath != null
                        )
            }
            FilterType.INCOMPLETE -> questionItems.filter {
                val ans = it.answers.firstOrNull()
                ans == null || (
                        ans.selectedSingleAnswer.isNullOrBlank() &&
                                ans.selectedMultiAnswers.isNullOrBlank() &&
                                ans.numberAnswer == null &&
                                ans.percentAnswer == null &&
                                ans.textAnswer.isNullOrBlank() &&
                                ans.photoPath == null &&
                                !ans.skipped
                        )
            }
            FilterType.WRONG_ANSWERS -> questionItems.filter { item ->
                val ans = item.answers.firstOrNull()
                val correctOptionId = item.options.find { it.isCorrect }?.id
                val userOptionId = ans?.selectedSingleAnswer
                !userOptionId.isNullOrBlank() && correctOptionId != null && userOptionId.trim() != correctOptionId.trim()
            }
        }

        // Обновляем адаптер только с отфильтрованными вопросами
        val filteredItems = mutableListOf<SurveyItem>()
        val grouped = filtered.groupBy { it.question.categoryId }

        for ((catId, questions) in grouped) {
//            val cat = surveyItems.find { it is SurveyItem.CategoryItem && it.category.id == catId } as? SurveyItem.CategoryItem
            val cat = surveyAdapter.currentList.find { it is SurveyItem.CategoryItem && it.category.id == catId } as? SurveyItem.CategoryItem
            if (cat != null) {
                filteredItems.add(cat)
                filteredItems.addAll(questions)
            }
        }

        surveyAdapter.setItems(filteredItems)
        surveyAdapter.notifyDataSetChanged()

        val currentVisibleQuestions = filteredItems.mapIndexedNotNull { index, item ->
            if (item is SurveyItem.QuestionItem) index else null
        }
        if (currentQuestionIndex !in currentVisibleQuestions) {
            val fallbackIndex = currentVisibleQuestions.firstOrNull()
            if (fallbackIndex != null) {
                currentQuestionIndex = fallbackIndex
                val question = (filteredItems[fallbackIndex] as SurveyItem.QuestionItem).question
                updateBottomBar(question)
                surveyAdapter.setSelectedPosition(fallbackIndex)
                recyclerView.scrollToPosition(fallbackIndex)
            }
        }

    }


    private fun loadSurveyData() {
        lifecycleScope.launch {
            try {
                val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch
                val categoriesWithQuestions = database.surveyDao().getCategoriesWithQuestionsAndAnswers(surveyId)
                val questionAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

                // Преобразуем в SurveyItem
                surveyItems = categoriesWithQuestions.flatMap { categoryWithQuestions ->
                    val categoryItem = SurveyItem.CategoryItem(categoryWithQuestions.category)
                    val questionItems = categoryWithQuestions.questions.map {
                        val question = it.question
                        val options = it.answers
                        val answers = questionAnswers.filter { answer -> answer.questionId == question.id }
                        SurveyItem.QuestionItem(question, answers, options)
                    }

                    listOf(categoryItem) + questionItems
                }

                surveyAdapter = SurveyAdapter(
                    userAnswers = questionAnswers,
                    onQuestionClick = { question, position ->
//                        currentQuestionIndex = position
                        viewModel.selectQuestion(position)
                        updateBottomBar(question)
                    },
                            onAnswerSelected = { question, optionId, optionText, position ->
                                viewModel.selectQuestion(position)
                                viewModel.saveAnswer(
                            question = question,
                            selectedOptionId = listOf(optionId),
                            selectedOptionTexts = listOf(optionText),
                                    autoScrollDown = true
                                )
//                                scrollDown()
//                                viewModel.scrollDown()
                    },
                    onPhotoClick = { question, position ->
                        // ✅ фиксируем выбранный вопрос
                        viewModel.selectQuestion(position)
                        // ✅ устанавливаем, какой вопрос связан с фото
                        viewModel.currentPhotoQuestionId.value = question.id
                        // ✅ открываем камеру (твоя функция)
                        openCamera()
                    }
                )
                surveyAdapter.setItems(surveyItems)


                recyclerView.layoutManager = LinearLayoutManager(this@SurveyActivity)
                recyclerView.adapter = surveyAdapter

                // Выбираем первый вопрос
                val firstQuestionIndex = surveyItems.indexOfFirst { it is SurveyItem.QuestionItem }
                if (firstQuestionIndex != -1) {
                    currentQuestionIndex = firstQuestionIndex
                    val firstQuestion = (surveyItems[firstQuestionIndex] as SurveyItem.QuestionItem).question
                    updateBottomBar(firstQuestion)
                    surveyAdapter.setSelectedPosition(firstQuestionIndex)
                }

            } catch (e: Exception) {
                Log.e("SurveyActivity", "Ошибка загрузки данных: ${e.message}")
                AppLogger.log("SurveyActivity", "Помилка завантаження данних в адптер: ${e.message}", this@SurveyActivity)
            }
        }
    }



    private fun updateBottomBar(question: QuestionEntity) {
        val bottomBar: LinearLayout = findViewById(R.id.answerButtonsContainer)
        bottomBar.removeAllViews()

        val commentEditText: EditText = findViewById(R.id.editTextComment)
        commentEditText.removeTextChangedListener(currentCommentWatcher) // Удаляем старый watcher

        lifecycleScope.launch {
            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch
            val answers = database.resultsSurveyDao().getAnswersForQuestion(surveyId, question.id)
            val comment = answers.firstOrNull()?.comment ?: ""
            commentEditText.setText(comment)
        }

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val commentRaw = s?.toString()
                val comment = commentRaw?.takeIf { it.isNotBlank() }

                lifecycleScope.launch {
                    val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch
                    val questionId = question.id

                    Log.d("CommentDebug", "surveyId = $surveyId")
                    Log.d("CommentDebug", "questionId = $questionId")
                    Log.d("CommentDebug", "comment = $comment")

                    val existingAnswers = database.resultsSurveyDao().getAnswersForQuestion(surveyId, questionId)
                    Log.d("CommentDebug", "Existing answers count = ${existingAnswers.size}")

                    if (existingAnswers.isNotEmpty()) {
                        Log.d("CommentDebug", "Обновляем комментарий для вопроса $questionId")
                        database.resultsSurveyDao().updateCommentForQuestion(surveyId, questionId, comment)
                    } else if (comment != null) {
                        Log.d("CommentDebug", "Создаем новую запись с комментарием")
                        viewModel.saveAnswer(question = question, comment = comment)
                    } else {
                        Log.d("CommentDebug", "Пустой комментарий — ничего не делаем")
                    }

                    val updatedAnswers = database.resultsSurveyDao().getAnswersForQuestion(surveyId, questionId)
                    updatedAnswers.forEach {
                        Log.d("CommentDebug", "Обновлено: id=${it.id}, comment=${it.comment}, surveyId=${it.surveyId}, questionId=${it.questionId}")
                    }

                    surveyAdapter.updateAnswerForQuestion(questionId, updatedAnswers)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        commentEditText.addTextChangedListener(watcher)
        currentCommentWatcher = watcher

        fun addButton(iconRes: Int, onClick: () -> Unit) {
            val button = ImageButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
                    72.dp, // ширина
                    72.dp  // высота
                ).apply { marginEnd = 8 }
                setImageResource(iconRes)
                background = null
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                setPadding(16, 10, 16, 10)
                setOnClickListener { onClick() }
            }
            bottomBar.addView(button)
        }

        // Добавляем нужные кнопки в зависимости от типа вопроса
        if (question.singleChoiceInput || question.multiChoiceInput) {
            addButton(R.drawable.ic_list) {
                showChoiceDialog(question) // Диалог выбора ответа
            }
        }

        if (question.requiredIfYes || question.requiredIfNo || question.alwaysRequired) {
            addButton(R.drawable.ic_add_foto) {
                if (!hasEnoughStorage()) {
                    Toast.makeText(this, "Недостаточно памяти для фото", Toast.LENGTH_LONG).show()
                    AppLogger.log("PhotoDebug", "🚫 Камера не запущена — мало памяти", this)

                } else {
                    checkAllPermissionsAndLaunchCamera()
//                checkCameraPermissionAndLaunch()
//                openCamera() // Открываем камеру
                }
            }
        }

        if (question.numberInput) {
            addButton(R.drawable.ic_number) {
                showNumberInputDialog(question) // Ввод числа
            }
        }

        if (question.percentInput) {
            addButton(R.drawable.ic_calculator) {
                showPercentInputDialog(question) // Ввод процента
            }
        }

        if (question.textInput) {
            addButton(R.drawable.ic_edit) {
                showTextInputDialog(question) // Ввод текста
            }
        }

        addButton(R.drawable.ic_skip) {
            skipAnswer(question)
        }

        // Кнопка очистки (всегда присутствует)
        addButton(R.drawable.ic_clear) {
            clearAnswer(question) // Очистка ответа
        }
    }

    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()


    private fun showChoiceDialog(question: QuestionEntity) {
        // Открытие диалога выбора ответа
        lifecycleScope.launch {
            val options = withContext(Dispatchers.IO) {
                database.surveyDao().getOptionsForQuestion(question.id)
            }

            if (options.isEmpty()) {
                Toast.makeText(this@SurveyActivity, "Немає доступних відповідей", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val selectedItems = mutableListOf<Int>()
            val optionTexts = options.map { it.text }.toTypedArray()

            if (question.multiChoiceInput) {
                AlertDialog.Builder(this@SurveyActivity)
                    .setTitle(question.text)
                    .setMultiChoiceItems(optionTexts, null) { _, which, isChecked ->
                        if (isChecked) {
                            selectedItems.add(which)
                        } else {
                            selectedItems.remove(which)
                        }
                    }
                    .setPositiveButton("OK") { _, _ ->
                        val selectedOptionIds = selectedItems.map { options[it].id }
                        val selectedOptionTexts = selectedItems.map { options[it].text }

                        viewModel.saveAnswer(
                            question = question,
                            selectedOptionIds = selectedOptionIds,
                            selectedOptionTexts = selectedOptionTexts,
                            autoScrollDown = true
                        )
//                        scrollDown()
//                        viewModel.scrollDown()
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            } else {
                AlertDialog.Builder(this@SurveyActivity)
                    .setTitle(question.text)
                    .setItems(optionTexts) { _, which ->
                        val selectedId = options[which].id
                        val selectedText = options[which].text

                        viewModel.saveAnswer(
                            question = question,
                            selectedOptionId = listOf(selectedId),
                            selectedOptionTexts  = listOf(selectedText),
                            autoScrollDown = true
                        )
//                        scrollDown()
//                        viewModel.scrollDown()
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Дозвіл на камеру")
                    .setMessage("Для зроблення фото потрібен дозвіл на камеру.")
                    .setPositiveButton("OK") { _, _ ->
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

//
//
private fun hasAllPermissions(): Boolean {
    val cameraGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    if (!cameraGranted) {
        AppLogger.log("Permissions", "❌ Дозвіл на камеру не надано", this)
    }

    return cameraGranted
}

    private fun checkAllPermissionsAndLaunchCamera() {
        if (hasAllPermissions()) {
            AppLogger.log("Permissions", "✅ Дозвіл на камеру надано, відкриваємо камеру", this)
            openCamera()
        } else {
            AppLogger.log("Permissions", "⛔ Запитуємо дозвіл: CAMERA", this)
            requestPermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }





    //    private fun openCamera() {
//        // Открытие камеры
//        val photoFile = createImageFile() ?: return
//        val photoUri = FileProvider.getUriForFile(
//            this,
//            "${packageName}.provider",
//            photoFile
//        )
//        currentPhotoPath = photoFile.absolutePath
//        takePictureLauncher.launch(photoUri)
//    }

    private fun hasEnoughStorage(minBytes: Long = 5_000_000): Boolean { // 5MB
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val stat = StatFs(dir?.path ?: return false)
        val availableBytes = stat.availableBytes
        AppLogger.log("StorageCheck", "Доступно: $availableBytes байт", this)
        return availableBytes > minBytes
    }

////12.09.25
//    private fun openCamera() {
//        val photoFile = createImageFile()
//        if (photoFile == null) {
//            AppLogger.log("PhotoDebug", "❌ Не вдалося створити файл фото", this)
//            Toast.makeText(this, "Помилка створення фото", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val photoUri = FileProvider.getUriForFile(
//            this,
//            "${packageName}.provider",
//            photoFile
//        )
//
//        // ✅ фиксируем вопрос
//        val currentIndex = viewModel.currentQuestionIndex.value
//        val currentItem = viewModel.surveyItems.value
//            .getOrNull(currentIndex) as? SurveyItem.QuestionItem
//
//        currentItem?.let {
//            val qId = it.question.id
//            viewModel.currentPhotoQuestionId.value = qId
//            AppLogger.log("PhotoDebug", "Фиксируем вопрос для фото: id=$qId (index=$currentIndex)", this)
//        }
//
//        // ✅ сохраняем путь и Uri
//        viewModel.setPhotoPath(photoFile.absolutePath)
////        viewModel.setPhotoUri(photoUri.toString())
//
//        AppLogger.log("PhotoDebug", "Камера відкрита. Шлях до фото: ${photoFile.absolutePath}", this)
//        AppLogger.log("PhotoDebug", "URI для запуску камери: $photoUri", this)
//
//        // ✅ вручную выдаём разрешения для MIUI
//        val resInfoList = packageManager.queryIntentActivities(
//            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
//            PackageManager.MATCH_DEFAULT_ONLY
//        )
//        for (resolveInfo in resInfoList) {
//            grantUriPermission(
//                resolveInfo.activityInfo.packageName,
//                photoUri,
//                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
//            )
//        }
//
//        // 🚀 запускаем камеру
//        takePictureLauncher.launch(photoUri)
//    }

//    //15.09.25
//private fun openCamera() {
//    val photoFile = createImageFile()
//    if (photoFile == null) {
//        AppLogger.log("PhotoDebug", "❌ Не вдалося створити файл фото", this)
//        Toast.makeText(this, "Помилка створення фото", Toast.LENGTH_SHORT).show()
//        return
//    }
//
//    val photoUri = FileProvider.getUriForFile(
//        this,
//        "${packageName}.provider",
//        photoFile
//    )
//
//    // ✅ фиксируем вопрос
//    val currentIndex = viewModel.currentQuestionIndex.value
//    val currentItem = viewModel.surveyItems.value
//        .getOrNull(currentIndex) as? SurveyItem.QuestionItem
//
//    currentItem?.let {
//        val qId = it.question.id
//        viewModel.currentPhotoQuestionId.value = qId
//        AppLogger.log("PhotoDebug", "Фиксируем вопрос для фото: id=$qId (index=$currentIndex)", this)
//    }
//
//    // ✅ сохраняем путь и Uri
//    viewModel.setPhotoPath(photoFile.absolutePath)
////    viewModel.setPhotoUri(photoUri.toString())
//
//    AppLogger.log("PhotoDebug", "Камера відкрита. Шлях до фото: ${photoFile.absolutePath}", this)
//    AppLogger.log("PhotoDebug", "URI для запуску камери: $photoUri", this)
//
////    // ✅ вручную выдаём разрешения для MIUI
////    val resInfoList = packageManager.queryIntentActivities(
////        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
////        PackageManager.MATCH_DEFAULT_ONLY
////    )
////    for (resolveInfo in resInfoList) {
////        grantUriPermission(
////            resolveInfo.activityInfo.packageName,
////            photoUri,
////            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
////        )
////    }
//
//    // 🚀 запускаем камеру
//    takePictureLauncher.launch(photoUri)
//}

//    21.10.25
//    private fun openCamera() {
//        if (shouldUseCustomCamera()) {
//            // 🚀 Запуск кастомной камеры (CameraActivity)
//            val intent = Intent(this, CameraActivity::class.java)
//
//            // фиксируем вопрос (то же, что и для системной камеры)
//            val currentIndex = viewModel.currentQuestionIndex.value
//            val currentItem = viewModel.surveyItems.value
//                .getOrNull(currentIndex) as? SurveyItem.QuestionItem
//
//            currentItem?.let {
//                val qId = it.question.id
//                viewModel.currentPhotoQuestionId.value = qId
//                AppLogger.log("PhotoDebug", "⚡ Запуск кастомної камери. Фиксируем вопрос: id=$qId (index=$currentIndex)", this)
//            }
//
//            cameraLauncher.launch(intent)
//        }
//        else {
//            // 🚀 Старая логика системной камеры
//            val photoFile = createImageFile()
//            if (photoFile == null) {
//                AppLogger.log("PhotoDebug", "❌ Не вдалося створити файл фото", this)
//                Toast.makeText(this, "Помилка створення фото", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            val photoUri = FileProvider.getUriForFile(
//                this,
//                "${packageName}.provider",
//                photoFile
//            )
//
//            // ✅ фиксируем вопрос
//            val currentIndex = viewModel.currentQuestionIndex.value
//            val currentItem = viewModel.surveyItems.value
//                .getOrNull(currentIndex) as? SurveyItem.QuestionItem
//
//            currentItem?.let {
//                val qId = it.question.id
//                viewModel.currentPhotoQuestionId.value = qId
//                AppLogger.log("PhotoDebug", "Фиксируем вопрос для фото: id=$qId (index=$currentIndex)", this)
//            }
//
//            // ✅ сохраняем путь
//            viewModel.setPhotoPath(photoFile.absolutePath)
//
//            AppLogger.log("PhotoDebug", "Камера відкрита. Шлях до фото: ${photoFile.absolutePath}", this)
//            AppLogger.log("PhotoDebug", "URI для запуску камери: $photoUri", this)
//
////            takePictureLauncher.launch(photoUri)
//            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
//            }
//            takePictureLauncher.launch(intent)
//        }
//    }
private fun openCamera() {
// 🔹 Проверяем разрешение камеры
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED
    ) {
// Если разрешение не выдано — запрашиваем
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
        AppLogger.log("PhotoDebug", "📸 Запрошено дозвіл на камеру", this)
        return
    }

// 🔹 Если разрешение уже есть — запускаем нужный сценарий
    if (shouldUseCustomCamera()) {
        // 🚀 Кастомная камера
        val intent = Intent(this, CameraActivity::class.java)

        val currentIndex = viewModel.currentQuestionIndex.value
        val currentItem = viewModel.surveyItems.value
            .getOrNull(currentIndex) as? SurveyItem.QuestionItem

        currentItem?.let {
            val qId = it.question.id
            viewModel.currentPhotoQuestionId.value = qId
            AppLogger.log(
                "PhotoDebug",
                "⚡ Запуск кастомної камери. Фіксуємо питання: id=$qId (index=$currentIndex)",
                this
            )
        }

        cameraLauncher.launch(intent)
    } else {
        // 🚀 Системная камера
        val photoFile = createImageFile()
        if (photoFile == null) {
            AppLogger.log("PhotoDebug", "❌ Не вдалося створити файл фото", this)
            Toast.makeText(this, "Помилка створення фото", Toast.LENGTH_SHORT).show()
            return
        }

        val photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            photoFile
        )

        val currentIndex = viewModel.currentQuestionIndex.value
        val currentItem = viewModel.surveyItems.value
            .getOrNull(currentIndex) as? SurveyItem.QuestionItem

        currentItem?.let {
            val qId = it.question.id
            viewModel.currentPhotoQuestionId.value = qId
            AppLogger.log("PhotoDebug", "Фіксуємо питання для фото: id=$qId (index=$currentIndex)", this)
        }

        viewModel.setPhotoPath(photoFile.absolutePath)

        AppLogger.log("PhotoDebug", "Камера відкрита. Шлях до фото: ${photoFile.absolutePath}", this)
        AppLogger.log("PhotoDebug", "URI для запуску камери: $photoUri", this)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        takePictureLauncher.launch(intent)
    }

}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLogger.log("PhotoDebug", "✅ Дозвіл на камеру надано, повторний запуск openCamera()", this)
                openCamera()
            } else {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )

                if (!showRationale) {
                    // 🚫 Пользователь выбрал “Don’t ask again”
                    AlertDialog.Builder(this)
                        .setTitle("Доступ до камери заблоковано")
                        .setMessage("Для роботи фото необхідно дозволити доступ до камери у налаштуваннях додатку.")
                        .setPositiveButton("Відкрити налаштування") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                        }
                        .setNegativeButton("Скасувати", null)
                        .show()
                    AppLogger.log("PhotoDebug", "🚫 Камеру заборонено користувачем назавжди", this)
                } else {
                    Toast.makeText(this, "❌ Доступ до камери заборонено", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }



    private fun shouldUseCustomCamera(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()

        // Пример: кастом только на Xiaomi / Redmi
        return manufacturer.contains("xiaomi") || model.contains("redmi")
    }


    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val path = viewModel.currentPhotoPath.value
                if (path == null) {
                    AppLogger.log("PhotoDebug", "❌ currentPhotoPath == null після повернення з камери", this)
                    Toast.makeText(this, "Помилка: шлях до фото відсутній", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }

                val file = File(path)
                var exists = file.exists()
                var size = file.length()

                AppLogger.log(
                    "PhotoDebug",
                    "📷 Результат камери path=$path, exists=$exists, size=$size",
                    this
                )

                // ⚠️ fallback: если файл пустой, но камера вернула thumbnail
                if ((!exists || size == 0L) && result.data?.extras?.get("data") is Bitmap) {
                    val bitmap = result.data?.extras?.get("data") as Bitmap
                    try {
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                        }
                        exists = file.exists()
                        size = file.length()
                        AppLogger.log(
                            "PhotoDebug",
                            "⚠️ Фото відновлено з thumbnail, exists=$exists, size=$size",
                            this
                        )
                    } catch (e: Exception) {
                        AppLogger.log("PhotoDebug", "❌ Помилка збереження thumbnail: ${e.message}", this)
                    }
                }

                if (exists && size > 0) {
                    val qId = viewModel.currentPhotoQuestionId.value
                    val currentItem = viewModel.surveyItems.value.find {
                        it is SurveyItem.QuestionItem && it.question.id == qId
                    } as? SurveyItem.QuestionItem

                    val noCompression = false // ⚡ тут потом подставишь флаг из question

                    val finalPhotoPath = compressImageIfNeeded(path, noCompression)

                    // логируем размер после сжатия
                    val compressedSize = File(finalPhotoPath).length()
                    AppLogger.log(
                        "PhotoDebug",
                        "📉 Сжатие фото: до=${size} байт, після=${compressedSize} байт",
                        this
                    )

                    viewModel.onPhotoCaptured(finalPhotoPath)
                    viewModel.confirmPhoto()
                } else {
                    Toast.makeText(this, "Не вдалося зберегти фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
                }
            } else {
                AppLogger.log("PhotoDebug", "🚫 Фото було скасовано", this)
            }
        }



    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                currentPhotoUri?.let { uri ->
                    AppLogger.log("PhotoDebug", "✅ Фото сохранено: $uri", this)

                    // 👉 сохраняем Uri в ViewModel (без filePath)
                    viewModel.setPhotoUri(uri.toString())

                    // Если нужно показать превью — открываем InputStream
                    contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        AppLogger.log("PhotoDebug", "📸 Bitmap загружен: ${bitmap.width}x${bitmap.height}", this)
                    }
                }
            } else {
                AppLogger.log("PhotoDebug", "❌ Фото отменено", this)
                // Чистим пустую запись из MediaStore
                currentPhotoUri?.let { contentResolver.delete(it, null, null) }
            }
        }

//    private fun openCamera() {
//        val values = ContentValues().apply {
//            put(MediaStore.Images.Media.TITLE, "survey_${System.currentTimeMillis()}")
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//        }
//
//        // 📌 Вставляем запись в MediaStore
//        currentPhotoUri =
//            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
//            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        takePhotoLauncher.launch(intent)
//    }



    // 🚀 Лаунчер для камеры
//    private val cameraLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            val path = viewModel.currentPhotoPath.value
//            if (path == null) {
//                AppLogger.log("PhotoDebug", "currentPhotoPath == null після viewModel", this)
//                Toast.makeText(this, "Помилка: шлях до фото відсутній", Toast.LENGTH_LONG).show()
//                return@registerForActivityResult
//            }
//
//            val file = File(path)
//            val exists = file.exists()
//            val size = file.length()
//
//            AppLogger.log(
//                "PhotoDebug",
//                "📸 Результат камери resultCode=${result.resultCode}, path=$path, exists=$exists, size=$size",
//                this
//            )
//
//            if (result.resultCode == Activity.RESULT_OK && exists && size > 0) {
//                // ✅ Определяем текущий вопрос
//                val qId = viewModel.currentPhotoQuestionId.value
//                val currentItem = viewModel.surveyItems.value.find {
//                    it is SurveyItem.QuestionItem && it.question.id == qId
//                } as? SurveyItem.QuestionItem
//
////            val noCompression = currentItem?.question?.noCompression ?: false
//                val noCompression = false
//
//                // ✅ Сжимаем при необходимости
//                val finalPhotoPath = compressImageIfNeeded(path, noCompression)
//
//                // ✅ Сохраняем в ViewModel
//                viewModel.onPhotoCaptured(finalPhotoPath)
//                viewModel.confirmPhoto()
//
//            } else {
//                AppLogger.log("PhotoDebug", "Користувач скасував або файл порожній", this)
//            }
//        }



    //    private fun createImageFile(): File? {
//        return try {
//            val fileName = "photo_${System.currentTimeMillis()}"
//            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            File.createTempFile(fileName, ".jpg", storageDir)
//        } catch (e: IOException) {
//            e.printStackTrace()
//            null
//        }
//    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photoPath = result.data?.getStringExtra("photoPath")
                if (photoPath != null) {
                    AppLogger.log("PhotoDebug", "📷 CameraX результат: $photoPath", this)

                    val qId = viewModel.currentPhotoQuestionId.value
                    val currentItem = viewModel.surveyItems.value.find {
                        it is SurveyItem.QuestionItem && it.question.id == qId
                    } as? SurveyItem.QuestionItem

                    val noCompression = false // можно брать из question
                    val finalPhotoPath = compressImageIfNeeded(photoPath, noCompression)


                    viewModel.setPhotoPath(finalPhotoPath)
//                    viewModel.confirmPhoto()
                    showPhotoPreviewDialog(finalPhotoPath)
                } else {
                    Toast.makeText(this, "Не вдалося зберегти фото", Toast.LENGTH_LONG).show()
                }
            } else {
                AppLogger.log("PhotoDebug", "🚫 Фото было отменено", this)
            }
        }


    private fun createImageFile(): File? {
    return try {
        val fileName = "photo_${System.currentTimeMillis()}"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(fileName, ".jpg", storageDir)
        AppLogger.log("PhotoDebug", "Файл створено: ${file.absolutePath}", this)
        file
    } catch (e: IOException) {
        AppLogger.log("PhotoDebug", "❌ Помилка створення фотофайлу: ${e.message}", this)
        e.printStackTrace()
        null
    }
}


//    private fun showPhotoPreviewDialog(photoPath: String) {
////
//        AppLogger.log("PhotoDebug", "Открытие превью фото: $photoPath", this)
//
//        val bitmap = BitmapFactory.decodeFile(photoPath)
//        if (bitmap == null) {
//            AppLogger.log("PhotoDebug", "Ошибка: не удалось декодировать фото из файла: $photoPath", this)
//            Toast.makeText(this, "Не вдалося відобразити фото", Toast.LENGTH_SHORT).show()
//            return
//        } else {
//            AppLogger.log("PhotoDebug", "Фото успішно завантажене", this)
//        }
////
//        val imageView = ImageView(this).apply {
//            layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )
//            setImageBitmap(BitmapFactory.decodeFile(photoPath))
//            adjustViewBounds = true
//            scaleType = ImageView.ScaleType.FIT_CENTER
//        }
//
//        val container = LinearLayout(this).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(16, 16, 16, 16)
//            addView(imageView)
//        }
//
//        AlertDialog.Builder(this)
//            .setView(container)
//            .setPositiveButton("Підтвердити") { _, _ ->
////
//                AppLogger.log("PhotoDebug", "Користувач підтвердив фото", this)
////
//                currentPhotoPath?.let { path ->
//                    val question = (surveyItems[currentQuestionIndex] as? SurveyItem.QuestionItem)?.question
//                    if (question != null) {
////
//                        AppLogger.log("PhotoDebug", "Питання знайдено: ${question.text}", this)
////
//                        saveAnswer(question, photoPath = path)
//                        scrollDown()
//                    } else {
//                        AppLogger.log("PhotoDebug", "❌ Питання не знайдено для індексу $currentQuestionIndex", this)
//                        Toast.makeText(this, "Питання не знайдено", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//            .setNegativeButton("Відмінити") { _, _ ->
////
//                AppLogger.log("PhotoDebug", "Користувач відмінив фото — перезапуск камери", this)
////
//                openCamera() // Перезапуск камеры
//            }
//            .setCancelable(false)
//            .show()
//    }

    fun waitForPhotoReadyAndPreview(path: String) {
        val photoFile = File(path)

        GlobalScope.launch(Dispatchers.Main) {
            var waited = 0
            val maxWaitTime = 5000L // 5 секунд
            val delayStep = 200L

            while ((photoFile.length() == 0L) && waited < maxWaitTime) {
                delay(delayStep)
                waited += delayStep.toInt()
            }

            if (photoFile.exists() && photoFile.length() > 0) {
                AppLogger.log("PhotoDebug", "✅ Фотофайл готовий через ${waited}мс", this@SurveyActivity)
                showPhotoPreviewDialog(path)
            } else {
                AppLogger.log("PhotoDebug", "❌ Фотофайл так і не з'явився", this@SurveyActivity)
                Toast.makeText(this@SurveyActivity, "Не вдалося отримати фото. Спробуйте ще раз.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPhotoPreviewDialog(photoPath: String) {
    AppLogger.log("PhotoDebug", "Спроба відкрити превью фото: $photoPath", this)

    val file = File(photoPath)

    if (!file.exists()) {
        AppLogger.log("PhotoDebug", "❌ Файл не існує: $photoPath", this)
        Toast.makeText(this, "Файл фото не знайдено", Toast.LENGTH_LONG).show()
        return
    }

    // Повторяем попытки декодировать bitmap, пока файл не будет готов (до 2 секунд)
    var bitmap: Bitmap? = null
    var attempt = 0
    val maxAttempts = 10 // по 200мс, итого 2 секунды

    while (bitmap == null && attempt < maxAttempts) {
        try {
            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, Uri.fromFile(file))
                ImageDecoder.decodeBitmap(source)
            } else {
                BitmapFactory.decodeFile(photoPath)
            }
        } catch (e: Exception) {
            AppLogger.log("PhotoDebug", "⚠️ Спроба ${attempt + 1}: файл ще не готовий. ${e.message}", this)
            Thread.sleep(200)
        }
        attempt++
    }

    if (bitmap == null) {
        AppLogger.log("PhotoDebug", "❌ Не вдалося завантажити фото після $attempt спроб", this)
        Toast.makeText(this, "Не вдалося завантажити фото", Toast.LENGTH_LONG).show()
        return
    }

    AppLogger.log("PhotoDebug", "✅ Фото успішно завантажене з ${photoPath}", this)

    val imageView = ImageView(this).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setImageBitmap(bitmap)
        adjustViewBounds = true
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 16, 16, 16)
        addView(imageView)
    }

    AlertDialog.Builder(this)
        .setView(container)
        .setPositiveButton("Підтвердити") { _, _ ->
            AppLogger.log("PhotoDebug", "Користувач підтвердив фото", this)
            val path = viewModel.currentPhotoPath.value
            if (path != null) {
//                val question = (surveyItems[currentQuestionIndex] as? SurveyItem.QuestionItem)?.question
//                if (question != null) {
//                    AppLogger.log("PhotoDebug", "Питання знайдено: ${question.text}", this)
//                    viewModel.saveAnswer(question, photoPath = path)
//                    viewModel.scrollDown()
                    viewModel.confirmPhoto()
//                } else {
//                    AppLogger.log("PhotoDebug", "⚠️ Питання не знайдено", this)
//                }
            } else {
                AppLogger.log("PhotoDebug", "⚠️ currentPhotoPath в ViewModel = null", this)
            }
        }
        .setNegativeButton("Відмінити") { _, _ ->
            AppLogger.log("PhotoDebug", "Користувач скасував фото, відкриття камери повторно", this)

            // 🗑️ Удаляем файл
            val fileToDelete = File(photoPath)
            if (fileToDelete.exists()) {
                val deleted = fileToDelete.delete()
                AppLogger.log(
                    "PhotoDebug",
                    if (deleted) "🗑️ Фото видалено: $photoPath" else "⚠️ Не вдалося видалити фото: $photoPath",
                    this
                )
            }

            // 🚀 Открываем камеру снова
            openCamera()
        }
        .setCancelable(false)
        .show()
}



    private fun showNumberInputDialog(question: QuestionEntity) {
        // Диалоговое окно для ввода числа

            val dialogView = layoutInflater.inflate(R.layout.dialog_number_input, null)
            val editText = dialogView.findViewById<EditText>(R.id.editTextNumber)
            val charCountText = dialogView.findViewById<TextView>(R.id.charCountText)

            // Ограничение по длине
            editText.filters = arrayOf(InputFilter.LengthFilter(10))

            // Слушатель ввода для обновления счётчика
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val length = s?.length ?: 0
                    charCountText.text = "$length/10"
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            val dialog = AlertDialog.Builder(this)
                .setTitle(question.text)
                .setView(dialogView)
                .setPositiveButton("OK") { _, _ ->
                    val inputText = editText.text.toString()
                    val number = inputText.toIntOrNull()
                    if (number != null && number in 0..1000) {
                        viewModel.saveAnswer(question = question, number = number, autoScrollDown = true)
//                        viewModel.scrollDown()
//                        scrollDown()
                    } else {
                        Toast.makeText(this, "Введіть ціле число від 0 до 1000", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Скасувати", null)
                .create()

            dialog.setOnShowListener {
                editText.requestFocus()
                editText.postDelayed({
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }, 100)
            }

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.show()
        }




    private fun showPercentInputDialog(question: QuestionEntity) {
        // Диалог для ввода процентов
            val dialogView = layoutInflater.inflate(R.layout.dialog_percent_input, null)

            val titleView = dialogView.findViewById<TextView>(R.id.textViewQuestionTitle)
            val totalShelfEdit = dialogView.findViewById<EditText>(R.id.editTextTotalShelf)
            val productTakesEdit = dialogView.findViewById<EditText>(R.id.editTextProductTakes)
            val percentEdit = dialogView.findViewById<EditText>(R.id.editTextPercent)

            val addTotalButton = dialogView.findViewById<Button>(R.id.buttonAddTotal)
            val addProductButton = dialogView.findViewById<Button>(R.id.buttonAddProduct)

            titleView.text = question.text

            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("Скасувати", null)

            val dialog = builder.create()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.show()

            // 👉 Добавление "+" в поле, если допустимо
            fun addPlusToField(editText: EditText) {
                val current = editText.text.toString()
                if (current.isNotEmpty() && !current.endsWith("+")) {
                    editText.append("+")
                }
            }

            addTotalButton.setOnClickListener { addPlusToField(totalShelfEdit) }
            addProductButton.setOnClickListener { addPlusToField(productTakesEdit) }

            // 👉 Автоматический расчет процента
            fun calculatePercentage() {
                try {
                    val totalParts = totalShelfEdit.text.toString().split("+")
                    val takenParts = productTakesEdit.text.toString().split("+")

                    val total = totalParts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }
                    val taken = takenParts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }

                    if (total > 0) {
                        val percent = (taken / total) * 100
                        percentEdit.setText(String.format("%.2f", percent))
                    }
                } catch (_: Exception) {
                    // Игнорировать ошибки
                }
            }

            totalShelfEdit.doAfterTextChanged { calculatePercentage() }
            productTakesEdit.doAfterTextChanged { calculatePercentage() }

            // 👉 Сохранение при нажатии OK
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val value = percentEdit.text.toString().replace(",", ".").toDoubleOrNull()
                if (value != null) {
                    viewModel.saveAnswer(question, percent = value, autoScrollDown = true)
//                    scrollDown()

                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Введіть число", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun showTextInputDialog(question: QuestionEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextAnswer)
        val counterView = dialogView.findViewById<TextView>(R.id.textCounter)

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                counterView.text = "$length/1024"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle(question.text)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val inputText = editText.text.toString().take(1024)
                if (inputText.isNotBlank()) {
                    viewModel.saveAnswer(question = question, text = inputText, autoScrollDown = true)
//                    scrollDown()
                }
            }
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            // Отложенное открытие клавиатуры
            editText.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.show()
    }

    private fun skipAnswer(question: QuestionEntity) {
        clearAnswer(question)
        viewModel.saveAnswer(question, skipped = true, autoScrollDown = true)
//        scrollDown()
    }

    private fun clearAnswer(question: QuestionEntity) {
        lifecycleScope.launch {
            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch

            // Удалить фото, если оно есть
            val existingAnswer = database.resultsSurveyDao().getAnswer(surveyId, question.id)
            existingAnswer?.photoPath?.let { photoPath ->
                val photoFile = File(photoPath)
                if (photoFile.exists()) {
                    photoFile.delete()
                }
            }

            database.resultsSurveyDao().deleteSurveyAnswersForIdquestion(question.id)

            val updatedAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

            // Обновим все QuestionItem-ы
//            surveyItems = surveyItems.map {
            surveyItems = surveyAdapter.currentList.map {
                if (it is SurveyItem.QuestionItem && it.question.id == question.id) {
                    it.copy(answers = updatedAnswers)
                } else it
            }
            val newAnswers = database.resultsSurveyDao().getAnswersForQuestion(surveyId, question.id)
            surveyAdapter.updateAnswerForQuestion(question.id, newAnswers)
        }
        commentEditText.text.clear()
    }


//    private fun saveAnswer(
//        question: QuestionEntity,
//        selectedOptionIds: List<String>? = null,
//        selectedOptionTexts: List<String>? = null,
//        number: Int? = null,
//        percent: Double? = null,
//        text: String? = null,
//        comment: String? = null,
//        photoPath: String? = null,
//        skipped: Boolean = false
//    ) {
//        lifecycleScope.launch {
//            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch
//            val surveyTitle = intent.getStringExtra("SURVEY_TITLE") ?: "Без назви"
//            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//            val userId = sharedPreferences.getLong("selected_user_idGamma", -1L)
//
//            // Получаем или создаём SurveyResult
//            val existingResult = database.resultsSurveyDao().getSurveyResult(surveyId).firstOrNull()
//            if (existingResult == null) {
//                val newResult = SurveyResultEntity(
//                    surveyId = surveyId,
//                    surveyTitle = surveyTitle,
//                    userId = userId,
//                    status = "draft"
//                )
//                database.resultsSurveyDao().insertSurveyResult(newResult)
//            }
//
//
//            // Удаляем предыдущий ответ на этот вопрос, если есть
////            database.resultsSurveyDao().deleteSurveyAnswerForQuestion(surveyId, question.id)
//
//            val existingAnswer = database.resultsSurveyDao().getAnswer(surveyId, question.id)
//            val category = database.surveyDao().getCategoryById(question.categoryId)
//            val categoryTitle = category?.description ?: "Без категорії"
//
//            // Удаляем старое фото, если есть и задано новое
//            existingAnswer?.photoPath?.let { oldPath ->
//                if (!photoPath.isNullOrBlank() && oldPath != photoPath) {
//                    val oldFile = File(oldPath)
//                    if (oldFile.exists()) oldFile.delete()
//                }
//            }
//
//            val updatedAnswer = SurveyAnswerEntity(
//                id = existingAnswer?.id ?: 0,
//                surveyId = surveyId,
//                categoryId = question.categoryId,
//                categoryTitle = categoryTitle,
//                questionId = question.id,
//                questionText = question.text,
//                selectedAnswers = selectedOptionIds?.joinToString(",") ?: existingAnswer?.selectedAnswers,
//                selectedAnswersText = selectedOptionTexts?.joinToString(",") ?: existingAnswer?.selectedAnswersText,
//                numberAnswer = number ?: existingAnswer?.numberAnswer,
//                percentAnswer = percent ?: existingAnswer?.percentAnswer,
//                textAnswer = text ?: existingAnswer?.textAnswer,
//                comment = comment ?: existingAnswer?.comment,
//                photoPath = photoPath ?: existingAnswer?.photoPath,
//                skipped = skipped
//            )
//
//            if (existingAnswer == null) {
//                database.resultsSurveyDao().insertAnswer(updatedAnswer)
//            } else {
//                database.resultsSurveyDao().updateAnswer(updatedAnswer)
//            }
//
////            логирование в файл
//            val logBuilder = StringBuilder("✅ Відповідь користувача:\n")
//            logBuilder.append("Опрос: [$surveyId] $surveyTitle\n")
//            logBuilder.append("Категорія: [${question.categoryId}] $categoryTitle\n")
//            logBuilder.append("Питання: [${question.id}] ${question.text}\n")
//
//            updatedAnswer.selectedAnswers?.takeIf { it.isNotBlank() }?.let {
//                logBuilder.append("🔹 ID відповідей: $it\n")
//            }
//            updatedAnswer.selectedAnswersText?.takeIf { it.isNotBlank() }?.let {
//                logBuilder.append("🔸 Текст відповідей: $it\n")
//            }
//            updatedAnswer.numberAnswer?.let {
//                logBuilder.append("🔢 Числове значення: $it\n")
//            }
//            updatedAnswer.percentAnswer?.let {
//                logBuilder.append("📊 Відсоток: $it\n")
//            }
//            updatedAnswer.textAnswer?.takeIf { it.isNotBlank() }?.let {
//                logBuilder.append("✏️ Текст: $it\n")
//            }
//            updatedAnswer.comment?.takeIf { it.isNotBlank() }?.let {
//                logBuilder.append("💬 Коментар: $it\n")
//            }
//            updatedAnswer.photoPath?.takeIf { it.isNotBlank() }?.let {
//                logBuilder.append("📷 Фото: $it\n")
//            }
//            if (updatedAnswer.skipped) {
//                logBuilder.append("⚠️ Пропущено користувачем\n")
//            }
//
//// Сохраняем в лог-файл через AppLogger
//            AppLogger.log("SurveyAnswer", logBuilder.toString(), this@SurveyActivity)
//
//
//            // После сохранения ответа:
//                refreshSurveyItems()
//
//                // 👇 Сразу обновим конкретный элемент в адаптере
//                val updatedIndex = surveyItems.indexOfFirst {
//                    it is SurveyItem.QuestionItem && it.question.id == question.id
//                }
//                if (updatedIndex != -1) {
//                    val updatedAnswers = database.resultsSurveyDao().getAnswersForQuestion(surveyId, question.id)
//                    surveyAdapter.updateAnswerForQuestion(question.id, updatedAnswers)
//                }
//
////            database.resultsSurveyDao().updateSurveyStatus(surveyId, "draft")
//            val today = LocalDate.now().toString()
//            database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "draft", today)
//
//        }
//    }


    private suspend fun refreshSurveyItems() {
        val surveyId = intent.getStringExtra("SURVEY_ID") ?: return
        val categoriesWithQuestions = database.surveyDao().getCategoriesWithQuestionsAndAnswers(surveyId)
        val updatedAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)

        surveyItems = categoriesWithQuestions.flatMap { categoryWithQuestions ->
            val categoryItem = SurveyItem.CategoryItem(categoryWithQuestions.category)
            val questionItems = categoryWithQuestions.questions.map {
                val question = it.question
                val options = it.answers
                val answers = updatedAnswers.filter { answer -> answer.questionId == question.id }
                SurveyItem.QuestionItem(question, answers, options)
            }
            listOf(categoryItem) + questionItems
        }

        surveyAdapter.setItems(surveyItems)
    }

    private fun checkSurveyCompletion() {
        lifecycleScope.launch {
            val surveyId = intent.getStringExtra("SURVEY_ID") ?: return@launch
//            val allQuestions = surveyItems.filterIsInstance<SurveyItem.QuestionItem>()
            val allQuestions = viewModel.surveyItems.value.filterIsInstance<SurveyItem.QuestionItem>()
            val incompleteIds = mutableListOf<String>()

//            for (item in allQuestions) {
//                val questionId = item.question.id
//                val answer = database.resultsSurveyDao().getAnswer(surveyId, questionId)
//
//                val hasAnswer = answer != null && (
//                        !answer.selectedAnswers.isNullOrBlank() ||
//                                answer.numberAnswer != null ||
//                                answer.percentAnswer != null ||
//                                !answer.textAnswer.isNullOrBlank() ||
//                                answer.skipped
//                        )
//
//                if (!hasAnswer) {
//                    incompleteIds.add(questionId)
//                }
//            }

            for (item in allQuestions) {
                val question = item.question
                val questionId = question.id
                val answer = database.resultsSurveyDao().getAnswer(surveyId, questionId)

//                val hasPhoto = answer?.photoPath?.isNotBlank() == true
//
//                val hasBaseAnswer = answer != null && (
//                        !answer.selectedAnswers.isNullOrBlank() ||
//                                answer.numberAnswer != null ||
//                                answer.percentAnswer != null ||
//                                !answer.textAnswer.isNullOrBlank() ||
//                                answer.skipped ||
//                                (question.alwaysRequired && hasPhoto)
//                        )
//
//
//                val textAnswer = answer?.textAnswer?.trim()?.lowercase()
//
//                val requiresPhoto = when {
//                    question.alwaysRequired -> true
//                    question.requiredIfYes && textAnswer == "так" -> true
//                    question.requiredIfNo && textAnswer == "ні" -> true
//                    else -> false
//                }
//
//                val photoOk = !requiresPhoto || hasPhoto
//
//                // Добавляем в список незаполненных, если не выполнены условия
//                if (!hasBaseAnswer || !photoOk) {
//                    incompleteIds.add(questionId)
//                }
                val hasPhoto = answer?.photoPath?.isNotBlank() == true
                val textAnswer = answer?.textAnswer?.trim()?.lowercase()

//                val requiresPhoto = when {
//                    question.alwaysRequired -> true
//                    question.requiredIfYes && textAnswer == "так" -> true
//                    question.requiredIfNo && textAnswer == "ні" -> true
//                    else -> false
//                }


                val hasBaseAnswer = answer != null && (
                        !answer.selectedSingleAnswer.isNullOrBlank() ||
                                !answer.selectedMultiAnswers.isNullOrBlank() ||
                                answer.numberAnswer != null ||
                                answer.percentAnswer != null ||
                                !answer.textAnswer.isNullOrBlank() ||
                                answer.skipped ||
//                                (question.alwaysRequired && hasPhoto) // <-- фото достаточно для alwaysRequired
                                hasPhoto
                        )
                val requiresPhoto = when {
                    question.alwaysRequired -> true
                    question.requiredIfYes && textAnswer == "так" -> true
                    question.requiredIfNo && textAnswer == "ні" -> true
                    else -> false
                }

                val photoOk = !requiresPhoto || hasPhoto

                if (!hasBaseAnswer || !photoOk) {
                    incompleteIds.add(questionId)
                }
            }

            val today = LocalDate.now().toString()
            if (incompleteIds.isEmpty()) {
                // Все вопросы пройдены — меняем статус
//                database.resultsSurveyDao().updateSurveyStatus(surveyId, "ready")
                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "ready", today)
                AppLogger.log("SurveyActivity", "Перевірка опитування на завершення прошла успішно дані відповіді на всі питання surveyId: ${surveyId} статус змінено на \"ready\"", this@SurveyActivity)
                Toast.makeText(this@SurveyActivity, "Опитування завершено!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Есть незаполненные — подсветить
//                database.resultsSurveyDao().updateSurveyStatus(surveyId, "draft")
                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "draft", today)
//                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "sent", today)
                AppLogger.log("SurveyActivity", "Перевірка опитування на завершення прошла невдало, дані відповіді не на всі питання surveyId: ${surveyId} статус \"draft\")", this@SurveyActivity)
                Toast.makeText(this@SurveyActivity, "Є незаповнені питання", Toast.LENGTH_LONG).show()
                applyFilter(FilterType.ALL)
                surveyAdapter.highlightIncompleteQuestions(incompleteIds)
                scrollToFirstIncompleteQuestion(incompleteIds)
            }
        }

    }

    private fun scrollToFirstIncompleteQuestion(incompleteIds: List<String>) {
        val index = surveyItems.indexOfFirst {
            it is SurveyItem.QuestionItem && incompleteIds.contains(it.question.id)
        }

        if (index != -1) {
            currentQuestionIndex = index
            val question = (surveyItems[index] as SurveyItem.QuestionItem).question
            updateBottomBar(question)
            surveyAdapter.setSelectedPosition(index)
            recyclerView.smoothScrollToPosition(index)
        }
    }


    private fun scrollUp() {
        val visibleItems = surveyAdapter.currentList
            .mapIndexedNotNull { index, item -> if (item is SurveyItem.QuestionItem) index else null }

        val currentVisibleIndex = visibleItems.indexOf(currentQuestionIndex)

        if (currentVisibleIndex > 0) {
            val newIndex = visibleItems[currentVisibleIndex - 1]
            currentQuestionIndex = newIndex
            val question = (surveyAdapter.currentList[newIndex] as SurveyItem.QuestionItem).question
            updateBottomBar(question)
            surveyAdapter.setSelectedPosition(newIndex)
            recyclerView.smoothScrollToPosition(newIndex)
        }
    }

    private fun scrollDown() {
        val visibleItems = surveyAdapter.currentList
            .mapIndexedNotNull { index, item -> if (item is SurveyItem.QuestionItem) index else null }

        val currentVisibleIndex = visibleItems.indexOf(currentQuestionIndex)

        if (currentVisibleIndex != -1 && currentVisibleIndex < visibleItems.size - 1) {
            val newIndex = visibleItems[currentVisibleIndex + 1]
            currentQuestionIndex = newIndex
            val question = (surveyAdapter.currentList[newIndex] as SurveyItem.QuestionItem).question
            updateBottomBar(question)
            surveyAdapter.setSelectedPosition(newIndex)
            recyclerView.smoothScrollToPosition(newIndex)
            surveyAdapter.setItems(surveyItems)
        }
    }


//    private fun scrollUp() {
//        val prevIndex = surveyItems.subList(0, currentQuestionIndex).indexOfLast { it is SurveyItem.QuestionItem }
//        if (prevIndex != -1) {
//            currentQuestionIndex = prevIndex
//            val question = (surveyItems[currentQuestionIndex] as SurveyItem.QuestionItem).question
//            updateBottomBar(question)
//            surveyAdapter.setSelectedPosition(currentQuestionIndex)
//            recyclerView.smoothScrollToPosition(currentQuestionIndex)
//        }
//    }
//private fun scrollUp() {
//    val visibleItems = surveyAdapter.currentList
//    val currentId = (surveyItems.getOrNull(currentQuestionIndex) as? SurveyItem.QuestionItem)?.question?.id ?: return
//    val currentVisibleIndex = visibleItems.indexOfFirst {
//        it is SurveyItem.QuestionItem && it.question.id == currentId
//    }
//
//    val nextIndex = visibleItems.subList(0, currentVisibleIndex).indexOfLast { it is SurveyItem.QuestionItem }
//    if (nextIndex != -1) {
//        val actualIndex = currentVisibleIndex + 1 + nextIndex
//        val item = visibleItems[actualIndex] as SurveyItem.QuestionItem
//        updateBottomBar(item.question)
//        surveyAdapter.setSelectedPosition(actualIndex)
//        recyclerView.smoothScrollToPosition(actualIndex)
//    }
//}
//
//
//        private fun scrollDown() {
//        val nextIndex = surveyItems.subList(currentQuestionIndex + 1, surveyItems.size).indexOfFirst { it is SurveyItem.QuestionItem }
//        if (nextIndex != -1) {
//            currentQuestionIndex += nextIndex + 1
//            val question = (surveyItems[currentQuestionIndex] as SurveyItem.QuestionItem).question
//            updateBottomBar(question)
//            surveyAdapter.setSelectedPosition(currentQuestionIndex)
//            recyclerView.smoothScrollToPosition(currentQuestionIndex)
//        }
//    }
//

//private fun scrollDown() {
//    val visibleItems = surveyAdapter.currentList
//    val currentId = (surveyItems.getOrNull(currentQuestionIndex) as? SurveyItem.QuestionItem)?.question?.id ?: return
//    val currentVisibleIndex = visibleItems.indexOfFirst {
//        it is SurveyItem.QuestionItem && it.question.id == currentId
//    }
//
//    val nextIndex = visibleItems.subList(currentVisibleIndex + 1, visibleItems.size).indexOfFirst { it is SurveyItem.QuestionItem }
//    if (nextIndex != -1) {
//        val actualIndex = currentVisibleIndex + 1 + nextIndex
//        val item = visibleItems[actualIndex] as SurveyItem.QuestionItem
//        updateBottomBar(item.question)
//        surveyAdapter.setSelectedPosition(actualIndex)
//        recyclerView.smoothScrollToPosition(actualIndex)
//    }
//}




}


