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
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.check_911.data.db.MainDb
import com.example.check_911.data.db.repository.SurveyRepository
import com.example.check_911.data.utils.AppLogger
import kotlinx.coroutines.launch

class LoadingQRActivity : AppCompatActivity() {

        private lateinit var progressBar: ProgressBar
        private lateinit var successImageView: ImageView
        private lateinit var successMessageTextView: TextView
        private lateinit var retryButton: Button

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_loading)

            progressBar = findViewById(R.id.progressBar)
            successImageView = findViewById(R.id.successImageView)
            successMessageTextView = findViewById(R.id.successMessageTextView)
            retryButton = findViewById(R.id.retryButton)

            val headerId = intent.getStringExtra("headerId")

            if (headerId.isNullOrEmpty()) {
                showError("❌ Немає ID опитування")
                AppLogger.log("QrAnalyzer", "❌ Немає ID опитування")
                return
            }

            downloadSurveys(headerId)
        }

        private fun downloadSurveys(headerId: String) {
            showLoading()

            lifecycleScope.launch {
                try {
                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val token = sharedPreferences.getString("auth_token", null)

                    if (token.isNullOrEmpty()) {
                        showError("❌ Немає токена")
                        AppLogger.log("QrAnalyzer", "❌ Немає токена")
                        return@launch
                    }

                    val retrofit = NetWorkProvider.provideRetrofit(
                        link = "${NetWorkProvider.BASE_URL}/"
                    )
                    val api = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)

                    val db = Room.databaseBuilder(
                        applicationContext,
                        MainDb::class.java,
                        "survey_db"
                    ).build()

                    val response = api.getSurveysByHeaderId(token, headerId.trim())
                    AppLogger.log("QrAnalyzer", "✅ Загрузка опитування за headerId: $headerId")

                    if (response.isSuccessful) {
                        val surveys = response.body().orEmpty()
                        if (surveys.isNotEmpty()) {
                            val repo = SurveyRepository(api, db.surveyDao())
                            repo.addSurveysToDatabase(surveys)

                            val titles = surveys.joinToString(", ") { it.title }
                            showSuccess("✅ Додано: $titles")
                            AppLogger.log("QrAnalyzer", "✅ Додано: $titles")

                            Handler(Looper.getMainLooper()).postDelayed({
                                startActivity(Intent(this@LoadingQRActivity, StoreActivity::class.java))
                                finish()
                            }, 1500)
                        } else {
                            showError("⚠️ Опитування відсутні")
                        }
                    } else {
                        showError("❌ Помилка сервера: ${response.code()}")
                        AppLogger.log("QrAnalyzer", "❌ Помилка сервера: ${response.code()}")
                    }
                } catch (e: Exception) {
                    showError("⚠️ Помилка: ${e.message}")
                    AppLogger.log("QrAnalyzer", "⚠️ Помилка: ${e.message}")
                }
            }
        }

        private fun showLoading() {
            progressBar.visibility = View.VISIBLE
            successImageView.visibility = View.GONE
            successMessageTextView.visibility = View.GONE
            retryButton.visibility = View.GONE
        }

        private fun showSuccess(message: String) {
            progressBar.visibility = View.GONE
            successImageView.visibility = View.VISIBLE
            successMessageTextView.text = message
            successMessageTextView.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
        }

        private fun showError(message: String) {
            progressBar.visibility = View.GONE
            successImageView.visibility = View.GONE
            successMessageTextView.text = message
            successMessageTextView.visibility = View.VISIBLE
            retryButton.visibility = View.VISIBLE

            retryButton.setOnClickListener {
                val headerId = intent.getStringExtra("headerId")
                if (!headerId.isNullOrEmpty()) {
                    downloadSurveys(headerId)
                }
            }
        }
    }
