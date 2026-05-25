package com.example.check_911

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.example.check_911.data.utils.AppLogger
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoadingActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var successMessageTextView: TextView
    private lateinit var successImageView: ImageView
    private lateinit var retryButton: Button
    private lateinit var continueButton: Button

    private val surveyViewModel: SurveyViewModel by viewModel()
    private val usersViewModel: UsersViewModel by viewModel()
    private val taskViewModel: TaskViewModel by viewModel()
    private val instructionViewModel: InstructionViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        progressBar = findViewById(R.id.progressBar)
        successMessageTextView = findViewById(R.id.successMessageTextView)
        successImageView = findViewById(R.id.successImageView)
        retryButton = findViewById(R.id.retryButton)
        continueButton = findViewById(R.id.continueButton)

        retryButton.setOnClickListener { loadData() }
        continueButton.setOnClickListener {
            startActivity(Intent(this, PromoActivity::class.java))
            finish()
        }

        loadData()
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        successMessageTextView.visibility = View.GONE
        successImageView.visibility = View.GONE
        retryButton.visibility = View.GONE
        continueButton.visibility = View.GONE

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null) {
            showError("Токен не знайдено. Виконайте вхід знову.")
            AppLogger.log("LoadingActivity", "Токена не знайдено.", this@LoadingActivity)
            return
        }

        val surveyResult = MutableLiveData<Result<Unit>>()
        val usersResult = MutableLiveData<Result<Unit>>()
        val tasksResult = MutableLiveData<Result<Unit>>()
        val instructionsResult = MutableLiveData<Result<Unit>>()

        surveyViewModel.surveyState.removeObservers(this)
        usersViewModel.usersState.removeObservers(this)
        taskViewModel.taskState.removeObservers(this)
        instructionViewModel.instructionState.removeObservers(this)

        surveyViewModel.surveyState.observe(this) { result ->
            surveyResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value, instructionsResult.value)
        }

        usersViewModel.usersState.observe(this) { result ->
            usersResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value, instructionsResult.value)
        }

        taskViewModel.taskState.observe(this) { result ->
            tasksResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value, instructionsResult.value)
        }

        instructionViewModel.instructionState.observe(this) { result ->
            instructionsResult.value = result
            checkCompletion(surveyResult.value, usersResult.value, tasksResult.value, instructionsResult.value)
        }

        surveyViewModel.loadSurveys(token)
        usersViewModel.loadUsers(token)
        taskViewModel.loadTasks(token)
        instructionViewModel.loadInstructions(token)
    }

    private fun checkCompletion(
        surveyResult: Result<Unit>?,
        usersResult: Result<Unit>?,
        tasksResult: Result<Unit>?,
        instructionsResult: Result<Unit>?
    ) {
        if (surveyResult == null || usersResult == null || tasksResult == null || instructionsResult == null) return

        if (surveyResult.isSuccess && usersResult.isSuccess && tasksResult.isSuccess && instructionsResult.isSuccess) {
            showSuccess()
            AppLogger.log("LoadingActivity", "Отримання даних пройшло успішно", this@LoadingActivity)
            return
        }

        val errors = listOfNotNull(
            surveyResult.exceptionOrNull()?.let { "Помилка завантаження опитувань: ${it.message}" },
            usersResult.exceptionOrNull()?.let { "Помилка завантаження користувачів: ${it.message}" },
            tasksResult.exceptionOrNull()?.let { "Помилка завантаження задач: ${it.message}" },
            instructionsResult.exceptionOrNull()?.let { "Помилка завантаження інструкцій: ${it.message}" }
        ).joinToString("\n")

        val allowContinue = surveyResult.isFailure || tasksResult.isFailure || instructionsResult.isFailure
        showError(errors, allowContinue)
    }

    private fun showSuccess() {
        progressBar.visibility = View.GONE
        successMessageTextView.visibility = View.VISIBLE
        successImageView.visibility = View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, PromoActivity::class.java))
            finish()
        }, 2)
    }

    private fun showError(message: String, allowContinue: Boolean = false) {
        progressBar.visibility = View.GONE
        successMessageTextView.text = message
        successMessageTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        continueButton.visibility = if (allowContinue) View.VISIBLE else View.GONE
        AppLogger.log("LoadingActivity", "Помилка завантаження: $message", this@LoadingActivity)
    }
}
