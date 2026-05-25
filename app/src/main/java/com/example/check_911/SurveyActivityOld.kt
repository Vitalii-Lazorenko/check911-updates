package com.example.check_911

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView

class SurveyActivityOld : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tableHeader: TableLayout
    private lateinit var questionList: LinearLayout
    private lateinit var commentInput: EditText
    private lateinit var answerToolbar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey_old)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val surveyName = intent.getStringExtra("SURVEY_NAME") ?: "Survey"
        supportActionBar?.title = surveyName

        tableHeader = findViewById(R.id.table_header)
        questionList = findViewById(R.id.question_list)
        commentInput = findViewById(R.id.comment_input)
        answerToolbar = findViewById(R.id.answer_toolbar)

        // Эмуляция получения данных вопросов с сервера
        val questions = listOf(
            "Азромены",
            "Вид аптеки снаружи",
            "Фасад аптеки в порядке, чистый, реклама работает?",
            "Входная дверь снаружи оформлена по стандартам",
            "Экраны та ТВ",
            "Вид аптеки внутри (зона для покупателей)"
        )
        val targets = listOf("Да", "Нет", "Да", "Да", "Нет", "Да")
        populateQuestions(questions, targets)
    }

    private fun populateQuestions(questions: List<String>, targets: List<String>) {
        for (i in questions.indices) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val questionText = TextView(this).apply {
                text = questions[i]
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    4f
                )
                setPadding(8, 8, 8, 8)
            }

            val actionView = TextView(this).apply {
                text = "" // Здесь могут быть дополнительные действия
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setPadding(8, 8, 8, 8)
            }

            val targetText = TextView(this).apply {
                text = targets[i]
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    2f
                )
                setPadding(8, 8, 8, 8)
            }

            val answerText = TextView(this).apply {
                text = "" // Ответ пользователя будет установлен здесь
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    2f
                )
                setPadding(8, 8, 8, 8)
            }

            row.addView(questionText)
            row.addView(actionView)
            row.addView(targetText)
            row.addView(answerText)

            questionList.addView(row)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.survey_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_finish -> {
                // Сохранить результаты и завершить опрос
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
