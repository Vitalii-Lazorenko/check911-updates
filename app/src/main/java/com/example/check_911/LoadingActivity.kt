package com.example.check_911

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import com.example.check_911.data.utils.AppLogger
import org.koin.androidx.viewmodel.ext.android.viewModel


class LoadingActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var successMessageTextView: TextView
    private lateinit var successImageView: ImageView
    private lateinit var retryButton: Button

    private val surveyViewModel: SurveyViewModel by viewModel()
    private val usersViewModel: UsersViewModel by viewModel()
    private val taskViewModel: TaskViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        progressBar = findViewById(R.id.progressBar)
        successMessageTextView = findViewById(R.id.successMessageTextView)
        successImageView = findViewById(R.id.successImageView)
        retryButton = findViewById(R.id.retryButton)

        loadData()

        retryButton.setOnClickListener {
            loadData()
        }
    }

    private fun loadData() {
        // Сбрасываем состояние
        progressBar.visibility = View.VISIBLE
        successMessageTextView.visibility = View.GONE
        successImageView.visibility = View.GONE
        retryButton.visibility = View.GONE

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null) {
            showError("Токена не знайдено. Виконайте вхід знову.")
            AppLogger.log("LoadingActivity", "Токена не знайдено.", this@LoadingActivity)
            return
        }

        val surveyResult = MutableLiveData<Result<Unit>>()
        val usersResult = MutableLiveData<Result<Unit>>()
        val tasksResult = MutableLiveData<Result<Unit>>()

        surveyViewModel.loadSurveys(token)
        surveyViewModel.surveyState.observe(this) { result ->
            surveyResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value)
        }

        usersViewModel.loadUsers(token)
        usersViewModel.usersState.observe(this) { result ->
            usersResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value)
        }

        taskViewModel.loadTasks(token)
        taskViewModel.taskState.observe(this) { result ->
            tasksResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value)
        }
    }

    private fun checkCompletion(
        surveyResult: Result<Unit>?,
        usersResult: Result<Unit>?,
        tasksResult: Result<Unit>?) {
        if (surveyResult == null || usersResult == null || tasksResult == null) return // Ждем все результаты

        if (surveyResult.isSuccess && usersResult.isSuccess && tasksResult.isSuccess) {
            showSuccess()
            AppLogger.log("LoadingActivity", "Отримання користувачів, опитувань і завдань прошло успішно", this@LoadingActivity)
        } else {
            val errors = listOfNotNull(
                surveyResult.exceptionOrNull()?.let { "Помилка завантаження опитувань: ${it.message}" },
                usersResult.exceptionOrNull()?.let { "Помилка завантаження користувачів: ${it.message}" },
                tasksResult.exceptionOrNull()?.let { "Помилка завантаження задач: ${it.message}" }
            ).joinToString("\n")
            showError(errors)
        }
    }

    private fun showSuccess() {
        progressBar.visibility = View.GONE
        successMessageTextView.visibility = View.VISIBLE
        successImageView.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
//            startActivity(Intent(this, SelectionActivity::class.java))
//            finish()
////        }, 2000)
            startActivity(Intent(this, PromoActivity::class.java))
            finish()
        }, 2)
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        successMessageTextView.text = message
        AppLogger.log("LoadingActivity", "Помилка завантаження: ${message}", this@LoadingActivity)
        successMessageTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
    }
}




//class LoadingActivity : AppCompatActivity() {
//    private lateinit var progressBar: ProgressBar
//    private lateinit var successMessageTextView: TextView
//    private lateinit var successImageView: ImageView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_loading)
//
//        progressBar = findViewById(R.id.progressBar)
//        successMessageTextView = findViewById(R.id.successMessageTextView)
//        successImageView = findViewById(R.id.successImageView)
//
//        // Симуляция загрузки данных
//        simulateDataLoading()
//    }
//
//    private fun simulateDataLoading() {
//        // Используем Handler для задержки (симуляция загрузки данных)
//        Handler(Looper.getMainLooper()).postDelayed({
//            // Загрузка завершена, показываем сообщение и изображение
//            progressBar.visibility = ProgressBar.GONE
//            successMessageTextView.visibility = TextView.VISIBLE
//            successImageView.visibility = ImageView.VISIBLE
//
//            // Автоматический переход на следующую активити через 2 секунды
//            Handler(Looper.getMainLooper()).postDelayed({
//                // Переход на SelectionActivity
//                val intent = Intent(this, SelectionActivity::class.java)
//                startActivity(intent)
//                finish()
//            }, 2000)
//        }, 3000) // 3 секунды задержки для симуляции загрузки данных
//    }
//}