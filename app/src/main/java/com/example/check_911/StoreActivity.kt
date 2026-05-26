package com.example.check_911

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.check_911.data.db.entity.InstructionEntity
import com.example.check_911.data.db.entity.SurveyAnswerEntity
import com.example.check_911.data.db.entity.SurveyEntity
import com.example.check_911.data.db.entity.SurveyResultEntity
import com.example.check_911.data.networking.networking.models.SurveyAnswerUpload
import com.example.check_911.data.networking.networking.models.SurveyUploadRequest
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File
import android.util.Base64
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.check_911.data.TestNotificationActivity
import com.example.check_911.data.UpdateChecker
import com.example.check_911.data.db.repository.SurveyRepository
import com.example.check_911.data.db.repository.InstructionUploadRepository
import com.example.check_911.data.utils.AppLogger
import com.example.check_911.data.utils.NetworkUtils
import com.example.check_911.data.utils.TelegramLogger
import com.example.check_911.data.utils.TelegramNotifier
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.example.check_911.data.utils.LocationChecker
import kotlinx.coroutines.delay


class StoreActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var storeNameTextView: TextView
    private lateinit var storeAddressTextView: TextView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var button_gps: Button
    private lateinit var button_send: Button
    private lateinit var gpsStatusText: TextView
    private lateinit var gpsStatusTextView: TextView
    private lateinit var buttonContainer: LinearLayout
    private var isUploading = false
    private lateinit var progressDialog: AlertDialog
    private var progressTextView: TextView? = null
    private var progressCountView: TextView? = null
    private var progressBar: ProgressBar? = null

    private var lastSelectedPeriods: List<String> = emptyList()
    private fun getSelectedPeriods(): List<String> = lastSelectedPeriods
    private val database by lazy { (application as App).database }
    private var uploadJob: Job? = null
    private lateinit var installPermissionLauncher: ActivityResultLauncher<Intent>
    private val viewModel: SurveysViewModel by viewModels {
        val db = (application as App).database
        SurveysViewModelFactory(db, application, this, intent.extras)
    }
    // 👉 Глобальная переменная для хранения индекса фото, где был сбой
    private var failedPhotoIndex: Int? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store)

        navigationView = findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

//        // принудительно создаем лог-файл
//        val logFile = AppLogger.getLogFile(this)
//        if (!logFile.exists()) logFile.createNewFile()
//        Toast.makeText(this, "Лог-файл: ${logFile.absolutePath}", Toast.LENGTH_LONG).show()

        AppLogger.log("StoreActivity", "Відкриття StoreActivity", this)
//        AppLogger.log("StoreActivity", "Лог-файл: ${logFile.absolutePath}", this)



        val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
        val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)


        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Показываем диалог при запуске
        showReportSelectionDialog()

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        storeNameTextView = findViewById(R.id.storeNameTextView)
        storeAddressTextView = findViewById(R.id.storeAddressTextView)
        buttonContainer = findViewById(R.id.buttonContainer)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Установить название и адрес торговой точки
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val storeName = sharedPreferences.getString("pharmacyName", null)
        storeNameTextView.text = storeName
        storeAddressTextView.text = sharedPreferences.getString("pharmacyAddress", null)

//        installPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if (packageManager.canRequestPackageInstalls()) {
//                    UpdateChecker.apkFile?.let { UpdateChecker.installApk(this, it) }
//                } else {
//                    Toast.makeText(this, "Дозвіл INSTALL_UNKNOWN_APPS не надано!", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
        installPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            UpdateChecker.continueUpdateIfPermissionGranted(this)
        }
    }

//    private fun addGPSButton() {
//        val gpsCircleNumber = createCircleNumberTextView("1", true)
//        val buttonGPS = createButton("GPS", R.drawable.ic_gps_fixed, true)
//
//        val gpsLayout = createButtonLayout(gpsCircleNumber, buttonGPS)
//        buttonContainer.addView(gpsLayout)
//
//        buttonGPS.setOnClickListener {
//            val intent = Intent(this, MapActivity::class.java)
//            startActivityForResult(intent, 1)
//        }
//    }
private fun addGPSCheckButton() {
    val gpsCircleNumber = createCircleNumberTextView("1", true)

    // создаём обычную кнопку (тот же createButton)
    val buttonGPS = createButton("", R.drawable.ic_gps_fixed, true)

    // Настроим кнопку для двухстрочного текста
    buttonGPS.isAllCaps = false
    buttonGPS.gravity = Gravity.CENTER
    // Позволим перенос строк
    if (buttonGPS is TextView) {
        buttonGPS.setLines(2)
        buttonGPS.ellipsize = null
    }

    // Установим начальный текст: заголовок + статус (вторая строка серым)
    val title = "GPS перевірка"
    val status = "Координати не встановлені"
    val spannable = SpannableStringBuilder()
        .append(title)
        .append("\n")
    val start = spannable.length
    spannable.append(status)
    spannable.setSpan(AbsoluteSizeSpan(10, true), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(ForegroundColorSpan(Color.LTGRAY), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    // Можно также настроить размер заголовка, если нужно:
//    spannable.setSpan(AbsoluteSizeSpan(16, true), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//    spannable.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    buttonGPS.text = spannable

    val gpsLayout = createButtonLayout(gpsCircleNumber, buttonGPS)
    buttonContainer.addView(gpsLayout)

    buttonGPS.setOnClickListener {
        checkGpsLocation(buttonGPS) // передадим кнопку чтобы обновлять её текст внутри
    }
}

    private fun checkGpsLocation(buttonView: View) {
        val ttLat = 48.447259
        val ttLon = 27.412677

        // Показываем временный статус (внутри кнопки)
        updateGpsStatusOnButton(buttonView, "Перевіряємо координати…")

        LocationChecker.getCurrentLocation(this) { userLat, userLon ->
            if (userLat == null || userLon == null) {
                updateGpsStatusOnButton(buttonView, "Помилка визначення координат")
                return@getCurrentLocation
            }

            val distance = LocationChecker.calculateDistance(
                userLat, userLon,
                ttLat, ttLon
            )

            AppLogger.log("GPS", "Відстань до ТТ: ${distance}м.")

            if (distance <= 30f) {
                updateGpsStatusOnButton(buttonView, "Розташування підтверджено (${distance.toInt()} м.)")
            } else {
                updateGpsStatusOnButton(buttonView, "Розташування не підтверджено (${distance.toInt()} м.)")
            }
        }
    }

    /** Обновляет текст внутри кнопки (две строки) */
    private fun updateGpsStatusOnButton(buttonView: View, statusText: String) {
        if (buttonView !is TextView) return
        val title = "GPS перевірка"
        val spannable = SpannableStringBuilder()
            .append(title)
            .append("\n")
        val start = spannable.length
        spannable.append(statusText)
        // стили
//        spannable.setSpan(AbsoluteSizeSpan(16, true), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        spannable.setSpan(ForegroundColorSpan(Color.WHITE), 0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(AbsoluteSizeSpan(10, true), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.LTGRAY), start, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Обновление UI в главном потоке
        runOnUiThread {
            buttonView.text = spannable
        }
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Обработка Home
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return true
            }
            R.id.nav_settings -> {
                // Обработка Settings
//                val intent = Intent(this, TestNotificationActivity::class.java)
//                startActivity(intent)
//                return true
            }
            R.id.nav_support -> {
                // Обработка Support
                sendLogsToTelegram()
                return true
            }
            R.id.nav_update -> {
                // Обработка Update
                UpdateChecker.checkForUpdate(this, installPermissionLauncher, showNoUpdateMessage = true)
                return true
            }
        }
        return false
    }

private fun sendLogsToTelegram() {
    val dir = getExternalFilesDir(null)
    Log.d("TelegramLogger", "вызван sendLogsToTelegram() ")

    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
    val storeName = sharedPreferences.getString("pharmacyName", null)
    val storeId = sharedPreferences.getLong("pharmacy_Id", 1L)

    val filesToSend = dir?.listFiles()?.filter {
        it.name.endsWith(".txt") || it.name.endsWith(".json")
    } ?: emptyList()

    if (filesToSend.isEmpty()) {
        Toast.makeText(this, "Немає логів для відправки", Toast.LENGTH_SHORT).show()
        return
    }

    isUploading = true
    showLoadingDialog()

    var completedCount = 0
    val totalCount = filesToSend.size

    filesToSend.forEach { file ->
        TelegramLogger.sendFileToTelegram(
            context = this,
            file = file,
            caption = "ID: ${storeId}, ${storeName} Файл: ${file.name}",
            onComplete = {
                completedCount++
                if (completedCount == totalCount) {
                    runOnUiThread {
                        hideLoadingDialog()
                        isUploading = false
                        Toast.makeText(this, "Всі файли надіслано", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}


    private fun fetchSurveysFromDatabase(selectedPeriods: List<String>) {
    val db = (application as App).database
    lifecycleScope.launch {

        val surveys = db.surveyDao().getSurveysByPeriod(selectedPeriods)
        buttonContainer.removeAllViews() // Очистить контейнер перед добавлением новых кнопок
//        addGPSButton()
        addGPSCheckButton()


        if (surveys.isNotEmpty()) {
//            addSurveyButtons(surveys)

//            сортировка по порядку
            val sortedSurveys = surveys.sortedWith(compareBy<SurveyEntity> { it.orderNumber }.thenBy { it.title })
            addSurveyButtons(sortedSurveys)
        } else {
            // Добавим кнопку "Додати опитування через QR-код"
//            addScunButton()
            // Покажи заглушку или сообщение "Нет доступных опросов"
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StoreActivity, "Немає доступних опитувань", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

private fun addSurveyButtons(surveys: List<SurveyEntity>) {
    AppLogger.log(
        "StoreActivity",
        "запуск addSurveyButtons",
        this@StoreActivity
    )
    var indexOffset = 1
    val db = (application as App).database

    lifecycleScope.launch {
        val results = db.resultsSurveyDao().getAllSurveyResults()
//        val statusMap = results.associateBy { it.surveyId }
        val today = LocalDate.now().toString()


        // 🔹 Проверим только переодические
        surveys.forEach { survey ->
            val result = results.find { it.surveyId == survey.id }
            // Проверяем только периодические опросники
            val isPeriodic = survey.periodDescription.contains("щод", ignoreCase = true) ||
                    survey.periodDescription.contains("щотиж", ignoreCase = true) ||
                    survey.periodDescription.contains("щоміс", ignoreCase = true)
            AppLogger.log(
                "StoreActivity",
                "запуск addSurveyButtons, isPeriodic = $isPeriodic",
                this@StoreActivity
            )
//            if (survey.periodDescription.contains("щод", ignoreCase = true)) {
            if (result != null) {
                AppLogger.log(
                    "StoreActivity",
                    "условия sentDate = ${result.sentDate}, today = $today, status = ${result.status}",
                    this@StoreActivity
                )
            }
                if (isPeriodic && result?.status == "sent" && result.sentDate != null && result.sentDate < today) {
                    AppLogger.log(
                        "StoreActivity",
                        "сработало условие очистки",
                        this@StoreActivity
                    )
                    // ❌ Очищаем, потому что устарело
                    withContext(Dispatchers.Main) {
                        viewModel.clearSurveyResults(survey.id, today)
                    }

                    AppLogger.log(
                        "StoreActivity",
                        "Очищено прострочений щоденний опитувальник ${survey.title}, ID: ${survey.id}",
                        this@StoreActivity
                    )
                }
//            }

            // 🔹 Дополнительно: удаляем опросник, если он не только для аптеки
            if (survey.onlyPharmacy == false && result?.status == "sent" && result.sentDate != null && result.sentDate < today) {
                withContext(Dispatchers.IO) {
                    db.surveyDao().deleteSurveyById(survey.id)
//                    db.resultsSurveyDao().deleteAnswersBySurveyId(survey.id)
//                    db.resultsSurveyDao().deleteSurveyResult(survey.id)
                }
                withContext(Dispatchers.Main) {
                    viewModel.clearSurveyResults(survey.id, today)
                }
                AppLogger.log(
                    "StoreActivity",
                    "Видалено опитувальник (onlyPharmacy=false) ${survey.title}, ID: ${survey.id}",
                    this@StoreActivity
                )
            }
        }

        // ⬇️ Теперь берём обновлённые данные
        val freshResults = db.resultsSurveyDao().getAllSurveyResults()
        val statusMap = freshResults.associateBy { it.surveyId }

        buttonContainer.removeAllViews() // <<< ОЧИЩАЕМ перед добавлением

//        addGPSCheckButton()

        var hasReadySurvey = false

        surveys.forEachIndexed { index, survey ->
            val status = statusMap[survey.id]?.status
            val iconRes = when (status) {
                "ready" -> R.drawable.ic_check_gray
                "sent" -> R.drawable.ic_check
                else -> R.drawable.ic_assignment
            }

            if (status == "ready") hasReadySurvey = true

            val circleNumber = createCircleNumberTextView((indexOffset + index).toString(), true)
//            val button = createButton("${survey.title} (${survey.periodDescription}) (${survey.typeDescription})", iconRes, true)
            // 🔹 Формируем текст кнопки
            val buttonText = if (status == "sent") {
                // добавляем новую строку с красным текстом
                val fullText = "${survey.title} (${survey.periodDescription}) (${survey.typeDescription})\nВІДПРАВЛЕНО"
                val spannable = SpannableString(fullText)
                val start = fullText.indexOf("ВІДПРАВЛЕНО")
                val end = start + "ВІДПРАВЛЕНО".length
                spannable.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable
            } else {
                SpannableString("${survey.title} (${survey.periodDescription}) (${survey.typeDescription})")
            }

            val button = createButton(buttonText, iconRes, true)

            val buttonLayout = createButtonLayout(circleNumber, button)
            buttonContainer.addView(buttonLayout)

            button.setOnClickListener {
//                val intent = Intent(this@StoreActivity, SurveyActivity::class.java).apply {
//                    putExtra("SURVEY_ID", survey.id)
//                    putExtra("SURVEY_TITLE", survey.title)
//                }
//                startActivity(intent)
                when (status) {
                    "sent" -> {
                        AlertDialog.Builder(this@StoreActivity)
                            .setTitle("Опитувальник вже надіслано")
                            .setMessage("Ви хочете повторно надіслати або редагувати опитування?")
                            .setPositiveButton("Повторно надіслати") { _, _ ->
                                lifecycleScope.launch {
                                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                                    val token = sharedPreferences.getString("auth_token", null) ?: return@launch
                                    val gammaId = sharedPreferences.getLong("selected_user_idGamma", -1L)
                                    val pharmacyId = sharedPreferences.getLong("pharmacy_Id", -1L)

//if (BuildConfig.DEBUG_VERSION) {
    uploadSurveyResults(survey.id, survey.title, token, pharmacyId, gammaId)
    withContext(Dispatchers.Main) {
        AppLogger.log(
            "StoreActivity",
            "Повторне відправляння опитувальника ${survey.title}, ID: ${survey.id}",
            this@StoreActivity
        )
//                                        Toast.makeText(
//                                            this@StoreActivity,
//                                            "Опитувальник ${survey.title} повторно надіслано",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
    }
//} else {
//    if (NetworkUtils.isWifiGammaActive(this@StoreActivity)) {
//                                        // Ок, выполняем запросы
//                                        uploadSurveyResults(survey.id, survey.title, token, pharmacyId, gammaId)
//                                        withContext(Dispatchers.Main) {
//                                            AppLogger.log(
//                                                "StoreActivity",
//                                                "Повторне відправляння опитувальника ${survey.title}, ID: ${survey.id}",
//                                                this@StoreActivity
//                                            )
////                                            Toast.makeText(
////                                                this@StoreActivity,
////                                                "Опитувальник ${survey.title} повторно надіслано",
////                                                Toast.LENGTH_SHORT
////                                            ).show()
//                                        }
//                                    } else {
//                                        Toast.makeText(this@StoreActivity, "Підключіться до Wi-Fi GAMMA. Також перевірте, що ви не використовуєте мобільну мережу.", Toast.LENGTH_SHORT).show()
//                                    }
//}
                                }
                            }
                            .setNegativeButton("Редагувати") { _, _ ->
                                lifecycleScope.launch {
//                                    database.resultsSurveyDao().updateSurveyStatus(survey.id, "draft")
                                    val today = LocalDate.now().toString() // например, "2025-09-26"
                                    database.resultsSurveyDao().updateSurveyStatusWithDate(survey.id, "draft", today)
                                    val intent = Intent(this@StoreActivity, SurveyActivity::class.java).apply {
                                        putExtra("SURVEY_ID", survey.id)
                                        putExtra("SURVEY_TITLE", survey.title)
                                    }
                                    AppLogger.log("StoreActivity", "Відкриття реадагування опитувальника ${survey.title}, ID: ${survey.id}", this@StoreActivity)

                                    startActivity(intent)
                                }
                            }
                            .setNeutralButton("Скасувати", null)
                            .show()
                    }

                    else -> {
                        val intent = Intent(this@StoreActivity, SurveyActivity::class.java).apply {
                            putExtra("SURVEY_ID", survey.id)
                            putExtra("SURVEY_TITLE", survey.title)
                        }
                        AppLogger.log("SelectionActivity", "Користувач відкрив опитувальник: ${survey.title}, ID: ${survey.id}", this@StoreActivity)
                        startActivity(intent)
                    }
                }
            }
        }

        // Добавим кнопку "Додати опитування через QR-код"
        addScunButton()
        addManualInputButton()

        lifecycleScope.launch {
            val instructions = database.instructionDao().getAllInstructions()
            val instructionResults = database.instructionResultDao().getAllResults()
            val today = LocalDate.now().toString()

            instructionResults
                .filter { it.status == "sent" && !it.sentDate.isNullOrBlank() && it.sentDate < today }
                .forEach { old ->
                    val oldAnswers = database.instructionResultDao().getAnswersByInstruction(old.instructionId)
                    oldAnswers.mapNotNull { it.photoPath }.distinct().forEach { path ->
                        runCatching {
                            val f = File(path)
                            if (f.exists()) f.delete()
                        }
                    }
                    database.instructionResultDao().clearInstructionResult(old.instructionId)
                }

            val freshResults = database.instructionResultDao().getAllResults().associateBy { it.instructionId }
            removeInstructionButtonsIfExists()
            addInstructionButtons(instructions, freshResults)
            val hasReadyInstruction = freshResults.values.any { it.status == "ready" }
            AppLogger.log(
                "StoreActivity",
                "instruction results loaded: total=${freshResults.size}, ready=${freshResults.values.count { it.status == "ready" }}",
                this@StoreActivity
            )
            if (hasReadyInstruction && !hasReadySurvey) {
                removeSendButtonIfExists()
                addSendButton(true)
            }
            updateButtonNumbers()
        }

        // Добавим кнопку "МОЇ ЗАВДАННЯ"
//        addTasksButton(tasksCount = 100)
        lifecycleScope.launch {
            database.taskDao().getTasksCountFlow().collect { count ->

                // ❗ сначала очищаем старую кнопку (если была)
                removeTasksButtonIfExists()

                // ❗ добавляем новую, только если есть задачи
//                if (count > 0) {
                    addTasksButton(count)
//                }

                updateButtonNumbers()
            }
        }

        // Добавим кнопку "Надіслати"
        addSendButton(hasReadySurvey)
    }
}

    override fun onResume() {
        super.onResume()
        if (lastSelectedPeriods.isNotEmpty()) {
            fetchSurveysFromDatabase(lastSelectedPeriods)
        }
    }

    override fun onDestroy() {
        uploadJob?.cancel()
        super.onDestroy()
    }


private fun addSendButton(isEnabled: Boolean) {
    val sendCircleNumber = createCircleNumberTextView((buttonContainer.childCount + 1).toString(), isEnabled)
    val buttonSend = createButton("Надіслати дані", R.drawable.ic_send, isEnabled)

    val sendLayout = createButtonLayout(sendCircleNumber, buttonSend)
    sendLayout.tag = "SEND_BUTTON"
    buttonContainer.addView(sendLayout)

    if (isEnabled) {
        buttonSend.setOnClickListener {
            uploadAllSurveyResults()
        }
    }
}

private fun removeSendButtonIfExists() {
    val existing = buttonContainer.findViewWithTag<View>("SEND_BUTTON")
    if (existing != null) {
        buttonContainer.removeView(existing)
    }
}


    private fun addScunButton() {
        val sendCircleNumber = createCircleNumberTextView((buttonContainer.childCount + 1).toString(), true)
        val buttonSend = createButton("Додати опитування через QR-код", R.drawable.ic_qrscunner, true)

        val sendLayout = createButtonLayout(sendCircleNumber, buttonSend)
        buttonContainer.addView(sendLayout)


            buttonSend.setOnClickListener {
                val intent = Intent(this, QrScannerActivity::class.java)
                startActivity(intent)
            }

    }

//  временное скачивание по id
private fun addManualInputButton() {
    val circleNumber = createCircleNumberTextView((buttonContainer.childCount + 1).toString(), true)
    val buttonManual = createButton("Додати опитування вручну", R.drawable.ic_manual_input, true)

    val manualLayout = createButtonLayout(circleNumber, buttonManual)
    buttonContainer.addView(manualLayout)

    buttonManual.setOnClickListener {
        showManualIdDialog()
    }
}

    private fun showManualIdDialog() {
        val editText = EditText(this).apply {
            hint = "Введіть ID опитування"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Додавання опитування вручну")
            .setView(editText)
            .setPositiveButton("Завантажити") { _, _ ->
                val id = editText.text.toString().trim()
                if (id.isNotEmpty()) {
                    val intent = Intent(this, LoadingQRActivity::class.java)
                    intent.putExtra("headerId", id)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Введіть ID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

//private fun addManualInputButton() {
//    val sendCircleNumber = createCircleNumberTextView((buttonContainer.childCount + 1).toString(), true)
//    val buttonManual = createButton("Додати опитування вручну (ID)", R.drawable.ic_manual_input, true)
//
//    val sendLayout = createButtonLayout(sendCircleNumber, buttonManual)
//    buttonContainer.addView(sendLayout)
//
//    buttonManual.setOnClickListener {
//        showManualInputDialog()
//    }
//}
//    private fun showManualInputDialog() {
//        val input = EditText(this).apply {
//            hint = "Введіть ID опитування"
//            inputType = InputType.TYPE_CLASS_TEXT
//            setPadding(40, 30, 40, 30)
//        }
//
//        AlertDialog.Builder(this)
//            .setTitle("Завантажити опитування за ID")
//            .setView(input)
//            .setPositiveButton("Завантажити") { _, _ ->
//                val headerId = input.text.toString().trim()
//                if (headerId.isNotEmpty()) {
//                    downloadSurveyById(headerId)
//                } else {
//                    Toast.makeText(this, "❌ Введіть ID", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton("Скасувати", null)
//            .show()
//    }
//
//    private fun downloadSurveyById(headerId: String) {
//        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
//        dialog.setContentView(R.layout.layout_loading_state)
//        dialog.setCancelable(false)
//        dialog.show()
//
//        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
//        val successMessageTextView = dialog.findViewById<TextView>(R.id.successMessageTextView)
//        val successImageView = dialog.findViewById<ImageView>(R.id.successImageView)
//        val retryButton = dialog.findViewById<Button>(R.id.retryButton)
//
//        // Начальное состояние
//        progressBar.visibility = View.VISIBLE
//        successMessageTextView.visibility = View.GONE
//        successImageView.visibility = View.GONE
//        retryButton.visibility = View.GONE
//
//        lifecycleScope.launch {
//            try {
//                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                val token = sharedPreferences.getString("auth_token", null)
//
//                if (token.isNullOrEmpty()) {
//                    dialog.dismiss()
//                    Toast.makeText(this@StoreActivity, "❌ Немає токена", Toast.LENGTH_SHORT).show()
//                    return@launch
//                }
//
//                val retrofit = NetWorkProvider.provideRetrofit(link = "${NetWorkProvider.BASE_URL}/")
//                val api = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//                val db = (application as App).database
//                val repo = SurveyRepository(api, db.surveyDao())
//
//                val response = api.getSurveysByHeaderId("$token", headerId)
//                if (response.isSuccessful) {
//                    val surveys = response.body().orEmpty()
//                    if (surveys.isNotEmpty()) {
//                        repo.addSurveysToDatabase(surveys)
//                        val titles = surveys.joinToString(", ") { it.title }
//
//                        AppLogger.log("StoreActivity", "✅ Додано ${surveys.size} (${titles}) через ручне введення ID", this@StoreActivity)
//
//                        progressBar.visibility = View.GONE
//                        successImageView.visibility = View.VISIBLE
//                        successMessageTextView.text = "✅ Додано: $titles"
//                        successMessageTextView.visibility = View.VISIBLE
//
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            dialog.dismiss()
//                            recreate()
//                        }, 1500)
//                    } else {
//                        showManualError(dialog, progressBar, successMessageTextView, retryButton, "⚠️ Опитування відсутні", headerId)
//                    }
//                } else {
//                    showManualError(dialog, progressBar, successMessageTextView, retryButton, "❌ Помилка сервера: ${response.code()}", headerId)
//                }
//            } catch (e: Exception) {
//                showManualError(dialog, progressBar, successMessageTextView, retryButton, "⚠️ Помилка: ${e.message}", headerId)
//            }
//        }
//    }
//
//
//    private fun showManualError(
//        dialog: Dialog,
//        progressBar: ProgressBar,
//        messageView: TextView,
//        retryButton: Button,
//        message: String,
//        headerId: String
//    ) {
//        progressBar.visibility = View.GONE
//        messageView.text = message
//        messageView.visibility = View.VISIBLE
//        retryButton.visibility = View.VISIBLE
//
//        retryButton.setOnClickListener {
//            dialog.dismiss()
//            downloadSurveyById(headerId)
//        }
//    }

//

//    private fun addTasksButton(tasksCount: Int) {
//
//        // 🔢 Номер кнопки (как у тебя)
//        val circleNumber = createCircleNumberTextView(
//            (buttonContainer.childCount + 1).toString(),
//            true
//        )
//
//        // 🔘 Основная кнопка
//        val buttonTasks = createButton(
//            "МОЇ ЗАВДАННЯ",
//            R.drawable.ic_tasks,
//            true
//        )
//
//        // 📦 Layout кнопки
//        val buttonLayout = createButtonLayout(circleNumber, buttonTasks)
//
//        // 🔴 Badge (красный кружок с числом)
//        if (tasksCount > 0) {
//            val badge = createBadge(tasksCount)
//            buttonLayout.addView(badge)
//        }
//
//        buttonContainer.addView(buttonLayout)
//
//        // 👉 Клик
//        buttonTasks.setOnClickListener {
////            val intent = Intent(this, TasksActivity::class.java)
//            startActivity(intent)
//        }
//    }
//
//    private fun createBadge(count: Int): TextView {
//        return TextView(this).apply {
//            text = count.toString()
//            setTextColor(Color.WHITE)
//            textSize = 12f
//            typeface = Typeface.DEFAULT_BOLD
//            gravity = Gravity.CENTER
//
//            setPadding(12, 4, 12, 4)
//
//            background = ContextCompat.getDrawable(
//                context,
//                R.drawable.bg_badge_red
//            )
//
//            // 📍 Позиция поверх кнопки
//            val params = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT,
//                FrameLayout.LayoutParams.WRAP_CONTENT
//            )
//            params.gravity = Gravity.END or Gravity.TOP
//            params.setMargins(0, 8, 8, 0)
//
//            layoutParams = params
//        }
//    }

//    private fun addTasksButton(tasksCount: Int) {
//
//        if (tasksCount <= 0) return // ❗ не показываем кнопку
//
//        val circleNumber = createCircleNumberTextView(
//            (buttonContainer.childCount + 1).toString(),
//            true
//        )
//
//        val button = createButton("МОЇ ЗАВДАННЯ", R.drawable.ic_tasks, true)
//
//        // 👉 ДОБАВЛЯЕМ badge внутрь кнопки
//        addBadgeToButton(button, tasksCount)
//
//        val layout = createButtonLayout(circleNumber, button)
//        layout.tag = "TASKS_BUTTON"
//
//        // 👉 ВСТАВКА ПЕРЕД ПОСЛЕДНЕЙ КНОПКОЙ
//        val insertIndex = if (buttonContainer.childCount > 0)
//            buttonContainer.childCount - 1
//        else
//            0
//
//        buttonContainer.addView(layout, insertIndex)
//
//        button.setOnClickListener {
//            startActivity(Intent(this, TasksActivity::class.java))
//        }
//    }
private fun addTasksButton(tasksCount: Int) {

    val isActive = tasksCount > 0

    val circleNumber = createCircleNumberTextView(
        (buttonContainer.childCount + 1).toString(),
        isActive
    )

    val button = createButton(
        "МОЇ ЗАВДАННЯ",
        R.drawable.ic_tasks,
        isActive
    )

    // 👉 badge всегда добавляем
    addBadgeToButton(button, tasksCount, isActive)

    val layout = createButtonLayout(circleNumber, button)
    layout.tag = "TASKS_BUTTON"

    val insertIndex = if (buttonContainer.childCount > 0)
        buttonContainer.childCount - 1
    else
        0

    buttonContainer.addView(layout, insertIndex)

    // 👉 если нет задач — не кликается
    if (isActive) {
        button.setOnClickListener {
            startActivity(Intent(this, TasksActivity::class.java))
        }
    } else {
        button.isEnabled = false
        circleNumber.isActivated = false
//        button.alpha = 0.5f // 👈 визуально "задизейблена"
    }
}

private fun addInstructionButtons(
    instructions: List<InstructionEntity>,
    resultsById: Map<String, com.example.check_911.data.db.entity.InstructionResultEntity>
) {
    instructions.forEach { instruction ->
        val status = resultsById[instruction.id]?.status ?: "draft"
        val isSent = status == "sent"
        val isReady = status == "ready"

        val circleNumber = createCircleNumberTextView(
            (buttonContainer.childCount + 1).toString(),
            true
        )

        val buttonText = when {
            isSent -> "??????????: ${instruction.title}\n???????????"
            isReady -> "??????????: ${instruction.title}\n?????? ?? ?????????"
            else -> "??????????: ${instruction.title}\n? ??????? ???????????"
        }
        val button = createButton(buttonText, R.drawable.ic_assignment, true)

        val layout = createButtonLayout(circleNumber, button)
        layout.tag = "INSTRUCTION_BUTTON"

        val insertIndex = if (buttonContainer.childCount > 0) buttonContainer.childCount - 1 else 0
        buttonContainer.addView(layout, insertIndex)

        button.setOnClickListener {
            if (isSent) {
                AlertDialog.Builder(this@StoreActivity)
                    .setTitle("?????????? ??? ???????????")
                    .setMessage("?? ?????? ???????? ????????? ?? ?????????? ???????????")
                    .setPositiveButton("???????? ?????????") { _, _ ->
                        lifecycleScope.launch {
                            database.instructionResultDao().updateResultStatus(instruction.id, "ready", null)
                            uploadAllSurveyResults()
                        }
                    }
                    .setNegativeButton("??????????") { _, _ ->
                        lifecycleScope.launch {
                            database.instructionResultDao().updateResultStatus(instruction.id, "draft", null)
                            val intent = Intent(this@StoreActivity, InstructionsActivity::class.java).apply {
                                putExtra(InstructionsActivity.EXTRA_INSTRUCTION_ID, instruction.id)
                                putExtra(InstructionsActivity.EXTRA_INSTRUCTION_TITLE, instruction.title)
                            }
                            startActivity(intent)
                        }
                    }
                    .setNeutralButton("?????????", null)
                    .show()
            } else {
                val intent = Intent(this, InstructionsActivity::class.java).apply {
                    putExtra(InstructionsActivity.EXTRA_INSTRUCTION_ID, instruction.id)
                    putExtra(InstructionsActivity.EXTRA_INSTRUCTION_TITLE, instruction.title)
                }
                startActivity(intent)
            }
        }
    }
}

//    private fun addBadgeToButton(button: Button, count: Int) {
//
//        val text = "МОЇ ЗАВДАННЯ"
//
//        // 👉 используем placeholder-символ, а не пробел
//        val spannable = SpannableString("$text  x")
//
//        val badgeView = createInlineBadge(count)
//        val drawable = drawViewToDrawable(badgeView)
//
//        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
//
//        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
//
//        spannable.setSpan(
//            imageSpan,
//            spannable.length - 1,
//            spannable.length,
//            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//        )
//
//        button.text = spannable
//
//        Log.d("BADGE", "width=${drawable.intrinsicWidth}, height=${drawable.intrinsicHeight}")
//    }
private fun addBadgeToButton(button: Button, count: Int, isActive: Boolean) {

    val text = "МОЇ ЗАВДАННЯ"
    val spannable = SpannableString("$text  x")

    val badgeView = createInlineBadge(count, isActive)
    val drawable = drawViewToDrawable(badgeView)

    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

    val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)

    spannable.setSpan(
        imageSpan,
        spannable.length - 1,
        spannable.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    button.text = spannable
}

    private fun drawViewToDrawable(view: View): Drawable {
        view.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return BitmapDrawable(resources, bitmap)
    }

//    private fun createInlineBadge(count: Int): TextView {
//        return TextView(this).apply {
//            text = if (count > 99) "99+" else count.toString()
//            setTextColor(Color.WHITE)
//            textSize = 12f
//            typeface = Typeface.DEFAULT_BOLD
//            gravity = Gravity.CENTER
//
//            setPadding(12, 4, 12, 4)
//
//            background = ContextCompat.getDrawable(
//                context,
//                R.drawable.bg_badge_red
//            )
//        }
//    }
private fun createInlineBadge(count: Int, isActive: Boolean): TextView {
    return TextView(this).apply {
        text = if (count > 99) "99+" else count.toString()
        setTextColor(Color.WHITE)
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER

        setPadding(12, 4, 12, 4)

        background = ContextCompat.getDrawable(
            context,
            if (isActive) R.drawable.bg_badge_red
            else R.drawable.bg_badge_gray // 👈 новый drawable
        )
    }
}

private fun removeTasksButtonIfExists() {
    for (i in 0 until buttonContainer.childCount) {
        val view = buttonContainer.getChildAt(i)

            if (view.tag == "TASKS_BUTTON") {
                buttonContainer.removeView(view)
                return
            }
    }
}

private fun removeInstructionButtonsIfExists() {
    for (i in buttonContainer.childCount - 1 downTo 0) {
        val view = buttonContainer.getChildAt(i)
        if (view.tag == "INSTRUCTION_BUTTON") {
            buttonContainer.removeViewAt(i)
        }
    }
}

    private fun updateButtonNumbers() {
        for (i in 0 until buttonContainer.childCount) {
            val layout = buttonContainer.getChildAt(i) as? ViewGroup ?: continue
            val circle = layout.getChildAt(0) as? TextView ?: continue
            circle.text = (i + 1).toString()
        }
    }

    private fun createCircleNumberTextView(number: String, isActive: Boolean): TextView {
        return TextView(this).apply {
            text = number
            textSize = 16f
            gravity = View.TEXT_ALIGNMENT_CENTER
            background = ContextCompat.getDrawable(this@StoreActivity, if (isActive) R.drawable.active_circle_background else R.drawable.inactive_circle_background)
            setTextColor(ContextCompat.getColor(this@StoreActivity, R.color.white))
            setPadding(35, 20, 35, 20) // внутренний отступ
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 16, 0) // отступ справа от кружка
            }
        }
    }

    private fun createButton(text: CharSequence, icon: Int, isEnabled: Boolean): Button {
        return Button(this).apply {
            setText(text)
            setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
            setEnabled(isEnabled)
            setTextColor(ContextCompat.getColor(this@StoreActivity, if (isEnabled) R.color.purple_500 else R.color.colorInactive))
            background = ContextCompat.getDrawable(this@StoreActivity, R.drawable.button_background) // ваш фоновый ресурс
            setPadding(32, 16, 32, 16) // внутренние отступы
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f // вес для растяжения кнопок
            ).apply {
                setMargins(0, 10, 0, 10) // отступы вокруг кнопки
            }
        }
    }

    private fun createButtonLayout(circleNumber: TextView, button: Button): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(circleNumber)
            addView(button)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16) // отступы между кнопками
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun setActiveButton(button: Button, circle: TextView, isActive: Boolean) {
        button.isEnabled = isActive
        val color = if (isActive) R.color.purple_500 else R.color.colorInactive
        button.setTextColor(ContextCompat.getColor(this, color))
        val circleBackground = if (isActive) R.drawable.active_circle_background else R.drawable.inactive_circle_background
        circle.setBackgroundResource(circleBackground)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // Активировать следующую кнопку
            val nextButtonLayout = buttonContainer.getChildAt(1) as LinearLayout
            val nextButton = nextButtonLayout.getChildAt(1) as Button
            val nextCircle = nextButtonLayout.getChildAt(0) as TextView
            setActiveButton(nextButton, nextCircle, true)
        }
    }

    private fun showReportSelectionDialog() {
        AppLogger.log("StoreActivity", "Відкриття діалогового вікна для вибору типів опитування", this@StoreActivity)
        val db = (application as App).database
        lifecycleScope.launch {
            val allPeriods = db.surveyDao().getUniquePeriodDescriptions()
            val periodList = mutableListOf("Додати всі види звітів")
            periodList.addAll(allPeriods.map { if (it == "ручна активація") "Опитування" else it })

            val selectedItems = BooleanArray(periodList.size) { false }

            val builder = AlertDialog.Builder(this@StoreActivity)
            builder.setTitle("Оберіть види звітів")

            builder.setMultiChoiceItems(periodList.toTypedArray(), selectedItems) { dialog, index, isChecked ->
                val alertDialog = dialog as AlertDialog
                val listView = alertDialog.listView

                if (index == 0) {
                    selectedItems.fill(isChecked)
                    for (i in selectedItems.indices) {
                        listView.setItemChecked(i, isChecked)
                    }
                } else if (selectedItems[0]) {
                    selectedItems[0] = false
                    listView.setItemChecked(0, false)
                }
            }

            builder.setPositiveButton("Вибрати", null) // кнопка создается, но обработчик добавим позже

            val dialog = builder.create()
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)

            dialog.setOnShowListener {
                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val selectedPeriods = periodList
                        .filterIndexed { index, _ -> selectedItems[index] }
                        .map { if (it == "Опитування") "ручна активація" else it }

                    if (selectedPeriods.isEmpty()) {
                        Toast.makeText(
                            this@StoreActivity,
                            "Будь ласка, оберіть хоча б один вид звіту",
                            Toast.LENGTH_SHORT
                        ).show()
                        // 🚫 не закрываем диалог
                        return@setOnClickListener
                    }

                    lastSelectedPeriods = selectedPeriods
                    fetchSurveysFromDatabase(selectedPeriods)
                    dialog.dismiss() // ✅ закрываем вручную только при успешном выборе
                }
            }

            dialog.show()
        }
    }


    private suspend fun convertAnswerToUpload(answer: SurveyAnswerEntity): SurveyAnswerUpload {
//        текущий 03.06.25
//        val base64Image = answer.photoPath?.let { path ->
//            val file = File(path)
//            if (file.exists()) {
//                val bytes = file.readBytes()
//                Base64.encodeToString(bytes, Base64.NO_WRAP)
//            } else null
//        }

        //вариант 03.06.25
//        val base64Image = withContext(Dispatchers.IO) {
//            answer.photoPath?.let { path ->
//                try {
//                    val file = File(path)
//                    if (file.exists()) {
//                        val bytes = file.readBytes()
//                        Base64.encodeToString(bytes, Base64.NO_WRAP)
//                    } else {
//                        AppLogger.log("SelectionActivity", "Файл не существует: $path", this@StoreActivity)
//                        Log.w("PhotoEncoding", "Файл не существует: $path")
//                        null
//                    }
//                } catch (e: Exception) {
//                    AppLogger.log("SelectionActivity", "Помилка при читанні фото: ${e.message}", this@StoreActivity)
//                    Log.e("PhotoEncoding", "Помилка при читанні фото: ${e.message}")
//                    null
//                }
//            }
//        }
        val base64Image = withContext(Dispatchers.IO) {
            answer.photoPath?.let { path ->
                try {
                    val file = File(path)
                    if (!file.exists()) {
                        AppLogger.log("SelectionActivity", "Файл не существует: $path", this@StoreActivity)
                        Log.w("PhotoEncoding", "Файл не существует: $path")
                        return@withContext null
                    }

                    // Размер файла до сжатия
                    val sizeKB = file.length() / 1024
                    Log.d("PhotoEncoding", "Розмір фото до стиснення: ${sizeKB} KB")

                    // Сжимаем изображение
                    val bitmap = BitmapFactory.decodeFile(path)
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream) // 60% качества

                    val byteArray = outputStream.toByteArray()
                    Log.d("PhotoEncoding", "Розмір після стиснення: ${byteArray.size / 1024} KB")
                    AppLogger.log("PhotoEncoding", "Розмір після стиснення: ${byteArray.size / 1024} KB", this@StoreActivity)

                    Base64.encodeToString(byteArray, Base64.NO_WRAP)
                } catch (e: Exception) {
                    AppLogger.log("SelectionActivity", "Помилка при стисканні фото: ${e.message}", this@StoreActivity)
                    Log.e("PhotoEncoding", "Помилка при стисканні фото: ${e.message}")
                    null
                }
            }
        }



        val multiAnswers = answer.selectedMultiAnswers
            ?.split(",")
            ?.mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            ?.toList() ?: emptyList()

        val singleAnswer = if (multiAnswers.size == 1) multiAnswers.first() else null

        return SurveyAnswerUpload(
            questionId = answer.questionId,
            imageData = base64Image?: null,
            text = answer.textAnswer?: null,
            number = answer.numberAnswer?: null,
            percent = answer.percentAnswer?: null,
            comment = answer.comment?: null,
            single = singleAnswer,
            multi = if (multiAnswers.size > 1) multiAnswers else emptyList()
        )
    }


private fun uploadAllSurveyResults() {
    uploadJob = lifecycleScope.launch(Dispatchers.IO) {
        try {
            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", null) ?: return@launch
            val gammaId = sharedPreferences.getLong("selected_user_idGamma", -1L)
            val pharmacyId = sharedPreferences.getLong("pharmacy_Id", -1L)

            val surveys = database.resultsSurveyDao().getAllSurveyResults()
            val readySurveys = surveys.filter { it.status == "ready" }
            val readyInstructions = database.instructionResultDao().getAllResults().filter { it.status == "ready" }

            if (readySurveys.isEmpty() && readyInstructions.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@StoreActivity,
                        "Немає завершених опитувань для надсилання",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

//            текуций 03.06.25
//            readySurveys.forEach { survey ->
//                uploadSurveyResults(survey.surveyId, survey.surveyTitle, token, pharmacyId, gammaId)
//
//                // После успешной отправки помечаем опрос как "sent"
//                database.resultsSurveyDao().updateSurveyStatus(survey.surveyId, "sent")
//            }
            //вариант 03.06.25
            for (survey in readySurveys) {
                withContext(Dispatchers.IO) {
//                    if (BuildConfig.DEBUG_VERSION) {
                        uploadSurveyResults(survey.surveyId, survey.surveyTitle, token, pharmacyId, gammaId)
////                    database.resultsSurveyDao().updateSurveyStatus(survey.surveyId, "sent")
//                    } else {
//                    if (NetworkUtils.isWifiGammaActive(this@StoreActivity)) {
//                        // Ок, выполняем запросы
//                        uploadSurveyResults(survey.surveyId, survey.surveyTitle, token, pharmacyId, gammaId)
////                        database.resultsSurveyDao().updateSurveyStatus(survey.surveyId, "sent")
//                        withContext(Dispatchers.Main) {
//                            AppLogger.log(
//                                "StoreActivity",
//                                "Відправляння опитувальника ${survey.surveyTitle}, ID: ${survey.surveyId}",
//                                this@StoreActivity
//                            )
////                            Toast.makeText(
////                                this@StoreActivity,
////                                "Опитувальник ${survey.surveyTitle} надіслано",
////                                Toast.LENGTH_SHORT
////                            ).show()
//                        }
//                    } else {
//                        Toast.makeText(this@StoreActivity, "Підключіться до Wi-Fi GAMMA. Також перевірте, що ви не використовуєте мобільну мережу.", Toast.LENGTH_SHORT).show()
//                    }
//                    }
                }
            }

            if (readyInstructions.isNotEmpty()) {
                val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
                val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
                val uploader = InstructionUploadRepository(apiService, database.instructionResultDao())
                uploader.uploadReadyInstructions(
                    token = token,
                    pharmacyId = pharmacyId,
                    gammaId = gammaId,
                    dateVersion = BuildConfig.DATE_VERSION
                )
            }

//            02.09.25
//            for (survey in readySurveys) {
//                withContext(Dispatchers.IO) {
//                    if (NetworkUtils.isWifiGammaActive(this@StoreActivity)) {
//                        // Ок, выполняем запросы
//                        uploadSurveyResults(survey.surveyId, survey.surveyTitle, token, pharmacyId, gammaId)
//                        database.resultsSurveyDao().updateSurveyStatus(survey.surveyId, "sent")
//
//                        withContext(Dispatchers.Main) {
//                            AppLogger.log(
//                                "StoreActivity",
//                                "Відправляння опитувальника ${survey.surveyTitle}, ID: ${survey.surveyId}",
//                                this@StoreActivity
//                            )
//                            Toast.makeText(
//                                this@StoreActivity,
//                                "Опитувальник ${survey.surveyTitle} надіслано",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@StoreActivity,
//                                "Підключіться до Wi-Fi GAMMA",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//                }
//            }


            // Перерисовать кнопки (UI!)
            withContext(Dispatchers.Main) {
                fetchSurveysFromDatabase(getSelectedPeriods())
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                AppLogger.log("SelectionActivity", "Помилка при відправці результатів прохождення опитувань: ${e.message}", this@StoreActivity)
                Toast.makeText(
                    this@StoreActivity,
                    "Помилка при відправці: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

    suspend fun fetchToken(): String? {
        val tokenRetrofit = NetWorkProvider.provideRetrofit(
            link = "http://10.128.233.15:3302" // токен идёт на 3302
        )
        val tokenService = NetWorkProvider.provideApiService(tokenRetrofit, ApiServiceData::class.java)
        return try {
            tokenService.getToken().token
        } catch (e: Exception) {
            Log.e("TokenFetch", "Ошибка получения токена: ${e.message}")
            null
        }
    }

// текущая версия 03.06.25
//    private suspend fun uploadSurveyResults(
//        surveyId: String,
//        surveyTitle: String,
//        token: String,
//        pharmacyId: Long,
//        gammaId: Long
//    ) {
//        val answers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//        val answerUploads = answers.map { convertAnswerToUpload(it) }
//
//        val request = SurveyUploadRequest(
//            surveyId = surveyId,
//            pharmacyId = pharmacyId,
//            gammaId = gammaId,
//            answers = answerUploads
//        )
//
//        val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
//        val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
////        сохранения json-a запроса
//        saveRequestToFile(this@StoreActivity, request)
//
//        val response = apiService.uploadSurvey("$token", request)
//
//        val db = (application as App).database
//
//        if (response.isSuccessful) {
//            AppLogger.log("SelectionActivity", "Вивантаження результатів опитування: ${surveyId} успішне", this@StoreActivity)
//            Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
////            db.resultsSurveyDao().deleteAllSurvey()
//        } else {
//            AppLogger.log("SelectionActivity", "Вивантаження результатів опитування: ${surveyId} невдале. Помилка: ${response.code()}", this@StoreActivity)
//            Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//        }
//    }

//    //обновленная версия 03.06.25
//private suspend fun uploadSurveyResults(
//    surveyId: String,
//    surveyTitle: String,
//    token: String,
//    pharmacyId: Long,
//    gammaId: Long
//) {
//    isUploading = true
//    withContext(Dispatchers.Main) { showLoadingDialog() }
//
//    try {
//        val answers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//        val answerUploads = answers.map { convertAnswerToUpload(it) }
//
//        val request = SurveyUploadRequest(
//            surveyId = surveyId,
//            pharmacyId = pharmacyId,
//            gammaId = gammaId,
//            answers = answerUploads
//        )
//
//        saveRequestToFile(this@StoreActivity, request)
//
//        val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
//        val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
//        val response = apiService.uploadSurvey(token, request)
//
////        if (response.isSuccessful) {
////            AppLogger.log("StoreActivity", "Вивантаження ${surveyId} успішне", this@StoreActivity)
////            Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
////        } else {
////            AppLogger.log("StoreActivity", "Вивантаження ${surveyId} невдале: ${response.code()}", this@StoreActivity)
////            Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
////        }
//        when {
//            response.isSuccessful -> {
//                withContext(Dispatchers.Main) {
//                    AppLogger.log(
//                        "StoreActivity",
//                        "Вивантаження ${surveyId} успішне",
//                        this@StoreActivity
//                    )
//                Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
//            }
//            }
//
//            response.code() == 401 -> {
//                withContext(Dispatchers.Main) {
//                    AppLogger.log("StoreActivity", "Помилка 401 — токен недійсний", this@StoreActivity)
//                    Toast.makeText(this@StoreActivity, "Помилка 401 — токен недійсний. Авторизуйтеся знову", Toast.LENGTH_LONG).show()
//                    redirectToLogin()
//                }
//            }
//
//            else -> {
//                withContext(Dispatchers.Main) {
//                    AppLogger.log("StoreActivity", "Вивантаження ${surveyId} невдале: ${response.code()}", this@StoreActivity)
//                    Toast.makeText(
//                        this@StoreActivity,
//                        "Помилка: ${response.code()}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//
//    } catch (e: Exception) {
//        withContext(Dispatchers.Main) {
//            AppLogger.log("StoreActivity", "Виняток при вивантаженні $surveyId: ${e.localizedMessage}", this@StoreActivity)
//            Toast.makeText(
//                this@StoreActivity,
//                "Помилка при вивантаженні: ${e.localizedMessage}",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    } finally {
//        isUploading = false
//        withContext(Dispatchers.Main) { hideLoadingDialog() }
//    }
//}

//    24.06.25
//    private suspend fun uploadSurveyResults(
//        surveyId: String,
//        surveyTitle: String,
//        token: String,
//        pharmacyId: Long,
//        gammaId: Long
//    ) {
//        isUploading = true
//        withContext(Dispatchers.Main) { showLoadingDialog() }
//
//        try {
//            val allAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//
//            // Отправляем ТЕКСТОВЫЕ ответы
//            val textOnlyAnswers = allAnswers.map {
//                SurveyAnswerUpload(
//                    questionId = it.questionId,
//                    imageData = null, // ← убираем фото
//                    text = it.textAnswer,
//                    number = it.numberAnswer,
//                    percent = it.percentAnswer,
//                    comment = it.comment,
//                    single = it.selectedAnswers?.takeIf { s -> !s.contains(",") },
//                    multi = it.selectedAnswers?.split(",")?.map { s -> s.trim() } ?: emptyList()
//                )
//            }
//
//            val request = SurveyUploadRequest(
//                surveyId = surveyId,
//                pharmacyId = pharmacyId,
//                gammaId = gammaId,
//                answers = textOnlyAnswers
//            )
//
////            val retrofit = NetWorkProvider.provideRetrofit(NetWorkProvider.BASE_URL)
//            val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
//            val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
//            // Отправка основной части (без фото)
//            saveRequestToFile(this@StoreActivity, request)
//            val response = apiService.uploadSurvey(token, request)
//
//            if (!response.isSuccessful) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//                }
//                return
//            }
//
//            // Отправляем ФОТО отдельно
//            allAnswers.filter { it.photoPath != null }.forEach { answer ->
//                val photoFile = File(answer.photoPath!!)
//                if (!photoFile.exists()) return@forEach
//
//                val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
//                val photoPart = MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)
//                val surveyIdPart = surveyId.toRequestBody("text/plain".toMediaTypeOrNull())
//                val questionIdPart = answer.questionId.toRequestBody("text/plain".toMediaTypeOrNull())
//
//                val photoResponse = apiService.uploadPhoto(
//                    token = token,
//                    photo = photoPart,
//                    surveyId = surveyIdPart,
//                    questionId = questionIdPart
//                )
//
//                if (!photoResponse.isSuccessful) {
//                    AppLogger.log("StoreActivity", "Фото для ${answer.questionId} не вдалося: ${photoResponse.code()}", this@StoreActivity)
//                }
//            }
//
//            withContext(Dispatchers.Main) {
//                AppLogger.log("StoreActivity", "Вивантаження $surveyId успішне", this@StoreActivity)
//                Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
//            }
//
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                AppLogger.log("StoreActivity", "Помилка при вивантаженні $surveyId: ${e.message}", this@StoreActivity)
//                Toast.makeText(this@StoreActivity, "Помилка при вивантаженні: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        } finally {
//            isUploading = false
//            withContext(Dispatchers.Main) { hideLoadingDialog() }
//        }
//    }

//    24.06.25 для нового метода отправки фото
//    private suspend fun uploadSurveyResults(
//        surveyId: String,
//        surveyTitle: String,
//        token: String,
//        pharmacyId: Long,
//        gammaId: Long,
//    ) {
//        isUploading = true
//        withContext(Dispatchers.Main) { showLoadingDialog() }
//
//        try {
//            val surveyEntity = database.surveyDao().getSurveyById(surveyId)
//            val allAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//            val questions = database.surveyDao().getQuestionsForSurvey(surveyId)
//
//            // Подготавливаем только текстовые ответы (без фото)
//            val textOnlyAnswers = allAnswers.map {
//                SurveyAnswerUpload(
//                    questionId = it.questionId,
//                    imageData = null,
//                    text = it.textAnswer,
//                    number = it.numberAnswer,
//                    percent = it.percentAnswer,
//                    comment = it.comment,
//                    single = it.selectedAnswers?.takeIf { s -> !s.contains(",") },
//                    multi = it.selectedAnswers?.split(",")?.map { s -> s.trim() } ?: emptyList()
//                )
//            }
//
//            val versionDateStr = BuildConfig.DATE_VERSION
//            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//            val parsedDate: Date? = try {
//                dateFormat.parse(versionDateStr)
//            } catch (e: Exception) {
//                null
//            }
//
//
//            val request = SurveyUploadRequest(
//                surveyId = surveyId,
//                pharmacyId = pharmacyId,
//                gammaId = gammaId,
////                dateVersion = BuildConfig.DATE_VERSION,
//                dateVersion = parsedDate,
//                answers = textOnlyAnswers
//            )
//
//            val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
//            val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
//            // Отправляем ТЕКСТОВЫЕ ДАННЫЕ
////            saveRequestToFile(this@StoreActivity, request)
//            val response = apiService.uploadSurvey(token, request)
//
////            отправляем ответы в телерам чат
//            // Проверяем флаг "отправлять ли в Telegram"
//            if (surveyEntity?.sendToTelegram == true) {
//                AppLogger.log("TelegramNotifier", "Найден опросник для отправки в телеграм", this@StoreActivity)
//                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                val storeName = sharedPreferences.getString("pharmacyName", null)
//                val userName = sharedPreferences.getString("selected_user_name", null)
//
//                //23.09.25
////                val telegramAnswers = allAnswers.mapNotNull { ans ->
////                    val q = questions.find { it.id == ans.questionId }
////                    if (q?.sendToTelegram == true) {
////                        AppLogger.log("TelegramNotifier", "Найдены вопросы для отправки в телеграм", this@StoreActivity)
////                        val answerText = when {
////                            !ans.selectedAnswersText.isNullOrBlank() -> ans.selectedAnswersText
////                            !ans.textAnswer.isNullOrBlank() -> ans.textAnswer
////                            ans.numberAnswer != null -> ans.numberAnswer.toString()
////                            ans.percentAnswer != null -> "${ans.percentAnswer}%"
////                            !ans.comment.isNullOrBlank() -> "Коментар: ${ans.comment}"
////                            ans.skipped -> "Без відповіді"
////                            else -> ""
////                        }
////                        ans.questionText to answerText
////                    } else null
////                }
//                // Получаем категории и сортируем их
//                val categories = database.surveyDao().getCategoriesForSurvey(surveyId)
//                    .sortedBy { it.orderNumber }
//
//// Собираем ответы в правильном порядке
//                val telegramAnswers = mutableListOf<Pair<String, String>>()
//
//                for (category in categories) {
//                    val categoryQuestions = questions
//                        .filter { it.categoryId == category.id && it.sendToTelegram }
//                        .sortedBy { it.orderNumber }
//
//                    for (q in categoryQuestions) {
//                        val ans = allAnswers.find { it.questionId == q.id }
//                        if (ans != null) {
//                            val answerText = when {
//                                !ans.selectedAnswersText.isNullOrBlank() -> ans.selectedAnswersText
//                                !ans.textAnswer.isNullOrBlank() -> ans.textAnswer
//                                ans.numberAnswer != null -> ans.numberAnswer.toString()
//                                ans.percentAnswer != null -> "${ans.percentAnswer}%"
//                                !ans.comment.isNullOrBlank() -> "Коментар: ${ans.comment}"
//                                ans.skipped -> "Без відповіді"
//                                else -> ""
//                            }
//                            telegramAnswers.add(q.text to answerText)
//                        }
//                    }
//                }
//
//
//                if (telegramAnswers.isNotEmpty()) {
//                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
//                    val message = buildTelegramMessage(
//                        surveyTitle = surveyEntity.title,
////                        surveyTitle = surveyTitle,
//                        pharmacyName = storeName ?: "",
//                        userName = userName ?: "",
//                        date = dateStr,
//                        answers = telegramAnswers
//                    )
//                    TelegramNotifier.sendMessage(message)
//                    AppLogger.log("TelegramNotifier", "отправлены вопросы для отправки в телеграм", this@StoreActivity)
//                }
//            }
//
//
//            // Обработка 401 ошибки (неверный токен)
//            if (response.code() == 401) {
//                withContext(Dispatchers.Main) {
//                    AppLogger.log("StoreActivity", "Помилка 401 — токен недійсний", this@StoreActivity)
//                    Toast.makeText(this@StoreActivity, "Помилка 401 токен недійсний — авторизуйтеся знову", Toast.LENGTH_LONG).show()
//                    redirectToLogin()
//                }
//                return
//            }
//
//
//            if (!response.isSuccessful) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//                }
//                return
//            }
//
//            // Получаем logHeaderId из ответа
//            val responseBody = response.body()
//            val logHeaderId = responseBody?.logHeaderId
//            if (logHeaderId.isNullOrBlank()) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@StoreActivity, "Помилка: відсутній logHeaderId", Toast.LENGTH_LONG).show()
//                }
//                return
//            }
//
//            // Отправляем ФОТО отдельно по одному через PUT /survey_log/accept/file?pharmacyId=...
//            allAnswers.filter { it.photoPath != null }.forEach { answer ->
//                val photoFile = File(answer.photoPath!!)
//                if (!photoFile.exists()) return@forEach
//
////                // для разового использование!!! сжатие фото перед отправкой
////                // ⚡ Сжимаем перед отправкой
////                val compressedFile = compressImageFile(photoFile, 70)
////                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
////                val filePart = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
////                //
//
//
//                val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
//                val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
//                val logHeaderIdPart = logHeaderId.toRequestBody("text/plain".toMediaTypeOrNull())
//                val questionIdPart = answer.questionId.toRequestBody("text/plain".toMediaTypeOrNull())
//
//                val photoResponse = apiService.uploadPhoto(
//                    token = token,
//                    pharmacyId = pharmacyId,
//                    logHeaderId = logHeaderIdPart,
//                    questionId = questionIdPart,
//                    file = filePart
//                )
//
//                // Обработка 401 ошибки (неверный токен)
//                if (photoResponse.code() == 401) {
//                    withContext(Dispatchers.Main) {
//                        AppLogger.log("StoreActivity", "Помилка 401 при завантаженні фото — токен недійсний", this@StoreActivity)
//                        Toast.makeText(this@StoreActivity, "Помилка 401 при завантаженні фото — авторизуйтеся знову", Toast.LENGTH_LONG).show()
//                        redirectToLogin()
//                    }
//                    return@forEach
//                }
//
//                if (!photoResponse.isSuccessful) {
//                    AppLogger.log("StoreActivity", "Фото ${answer.questionId} не вдалося: ${photoResponse.code()}", this@StoreActivity)
//                }
//            }
//
//            withContext(Dispatchers.Main) {
////                database.resultsSurveyDao().updateSurveyStatus(surveyId, "sent")
//                val today = LocalDate.now().toString() // например, "2025-09-26"
//                database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "sent", today)
//                AppLogger.log("StoreActivity", "Вивантаження $surveyId успішне", this@StoreActivity)
//                Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
//            }
//
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                AppLogger.log("StoreActivity", "Помилка при вивантаженні $surveyId: ${e.message}", this@StoreActivity)
//                Toast.makeText(this@StoreActivity, "Помилка при вивантаженні: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        } finally {
//            isUploading = false
//            withContext(Dispatchers.Main) { hideLoadingDialog() }
//        }
//    }


//    31.10.25
//private suspend fun uploadSurveyResults(
//    surveyId: String,
//    surveyTitle: String,
//    token: String,
//    pharmacyId: Long,
//    gammaId: Long,
//) {
//    isUploading = true
//    withContext(Dispatchers.Main) { showLoadingDialog() }
//
//    try {
//        val surveyEntity = database.surveyDao().getSurveyById(surveyId)
//        val allAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
//        val questions = database.surveyDao().getQuestionsForSurvey(surveyId)
//
//        // Подготавливаем только текстовые ответы (без фото)
//        val textOnlyAnswers = allAnswers.map {
//            SurveyAnswerUpload(
//                questionId = it.questionId,
//                imageData = null,
//                text = it.textAnswer,
//                number = it.numberAnswer,
//                percent = it.percentAnswer,
//                comment = it.comment,
//                single = it.selectedAnswers?.takeIf { s -> !s.contains(",") },
//                multi = it.selectedAnswers?.split(",")?.map { s -> s.trim() } ?: emptyList()
//            )
//        }
//
//        val versionDateStr = BuildConfig.DATE_VERSION
//        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//        val parsedDate: Date? = try {
//            dateFormat.parse(versionDateStr)
//        } catch (e: Exception) {
//            null
//        }
//
//        val request = SurveyUploadRequest(
//            surveyId = surveyId,
//            pharmacyId = pharmacyId,
//            gammaId = gammaId,
//            dateVersion = parsedDate,
//            answers = textOnlyAnswers
//        )
//
//        val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
//        val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
//        // 👉 Подсчёт шагов для прогресса
//        val photoAnswers = allAnswers.filter { it.photoPath != null }
//        val hasTelegram = surveyEntity?.sendToTelegram == true
//        val totalSteps = 1 + photoAnswers.size + if (hasTelegram) 1 else 0
//        var currentStep = 0
//
//        suspend fun step(message: String) {
//            currentStep++
//            updateProgress(currentStep, totalSteps, message)
//        }
//
//        // 🔹 1. Отправляем текстовые данные
//        step("Відправка текстових даних...")
//        val response = apiService.uploadSurvey(token, request)
//
//        if (response.code() == 401) {
//            withContext(Dispatchers.Main) {
//                AppLogger.log("StoreActivity", "Помилка 401 — токен недійсний", this@StoreActivity)
//                Toast.makeText(
//                    this@StoreActivity,
//                    "Помилка 401 токен недійсний — авторизуйтеся знову",
//                    Toast.LENGTH_LONG
//                ).show()
//                redirectToLogin()
//            }
//            return
//        }
//
//        if (!response.isSuccessful) {
//            withContext(Dispatchers.Main) {
//                Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//            }
//            return
//        }
//
//        val responseBody = response.body()
//        val logHeaderId = responseBody?.logHeaderId
//        if (logHeaderId.isNullOrBlank()) {
//            withContext(Dispatchers.Main) {
//                Toast.makeText(this@StoreActivity, "Помилка: відсутній logHeaderId", Toast.LENGTH_LONG).show()
//            }
//            return
//        }
//
//        // 🔹 2. Отправляем в Telegram (если нужно)
//        if (hasTelegram) {
//            step("Відправка у Telegram...")
//
//            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//            val storeName = sharedPreferences.getString("pharmacyName", null)
//            val userName = sharedPreferences.getString("selected_user_name", null)
//
//            val categories = database.surveyDao().getCategoriesForSurvey(surveyId)
//                .sortedBy { it.orderNumber }
//
//            val telegramAnswers = mutableListOf<Pair<String, String>>()
//
//            for (category in categories) {
//                val categoryQuestions = questions
//                    .filter { it.categoryId == category.id && it.sendToTelegram }
//                    .sortedBy { it.orderNumber }
//
//                for (q in categoryQuestions) {
//                    val ans = allAnswers.find { it.questionId == q.id }
//                    if (ans != null) {
//                        val answerText = when {
//                            !ans.selectedAnswersText.isNullOrBlank() -> ans.selectedAnswersText
//                            !ans.textAnswer.isNullOrBlank() -> ans.textAnswer
//                            ans.numberAnswer != null -> ans.numberAnswer.toString()
//                            ans.percentAnswer != null -> "${ans.percentAnswer}%"
//                            !ans.comment.isNullOrBlank() -> "Коментар: ${ans.comment}"
//                            ans.skipped -> "Без відповіді"
//                            else -> ""
//                        }
//                        telegramAnswers.add(q.text to answerText)
//                    }
//                }
//            }
//
//            if (telegramAnswers.isNotEmpty()) {
//                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
//                val message = buildTelegramMessage(
////                    surveyTitle = surveyEntity.title,
//                    surveyTitle = surveyEntity?.title ?: surveyTitle, // ✅ если null — использовать аргумент из функции
//                    pharmacyName = storeName ?: "",
//                    userName = userName ?: "",
//                    date = dateStr,
//                    answers = telegramAnswers
//                )
//                TelegramNotifier.sendMessage(message)
//                AppLogger.log("TelegramNotifier", "Відправлено у телеграм", this@StoreActivity)
//            }
//        }
//
////        // 🔹 3. Отправляем фото
////        for (answer in photoAnswers) {
////            step("Відправка фото...")
////
////            val photoFile = File(answer.photoPath!!)
////            if (!photoFile.exists()) continue
////
////            val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
////            val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
////            val logHeaderIdPart = logHeaderId.toRequestBody("text/plain".toMediaTypeOrNull())
////            val questionIdPart = answer.questionId.toRequestBody("text/plain".toMediaTypeOrNull())
////
////            val photoResponse = apiService.uploadPhoto(
////                token = token,
////                pharmacyId = pharmacyId,
////                logHeaderId = logHeaderIdPart,
////                questionId = questionIdPart,
////                file = filePart
////            )
////
////            if (photoResponse.code() == 401) {
////                withContext(Dispatchers.Main) {
////                    AppLogger.log("StoreActivity", "Помилка 401 при фото", this@StoreActivity)
////                    Toast.makeText(
////                        this@StoreActivity,
////                        "Помилка 401 при фото — авторизуйтеся знову",
////                        Toast.LENGTH_LONG
////                    ).show()
////                    redirectToLogin()
////                }
////                return
////            }
////
////            if (!photoResponse.isSuccessful) {
////                AppLogger.log("StoreActivity", "Фото ${answer.questionId} не вдалося: ${photoResponse.code()}", this@StoreActivity)
////            }
////        }
//        // 🔹 3. Отправляем фото
//        for ((index, answer) in photoAnswers.withIndex()) {
//            step("Відправка фото ${index + 1} з ${photoAnswers.size}...")
//
//            try {
//                val photoPath = answer.photoPath
//                if (photoPath.isNullOrEmpty()) {
//                    AppLogger.log("StoreActivity", "⚠️ Фото для питання ${answer.questionId} відсутнє (photoPath=null)", this@StoreActivity)
//                    continue
//                }
//
//                val photoFile = File(photoPath)
//
//                if (!photoFile.exists()) {
//                    AppLogger.log("StoreActivity", "⚠️ Фото ${photoFile.name} для питання ${answer.questionId} не знайдено. Шлях: $photoPath", this@StoreActivity)
//                    continue
//                }
//
//                val fileSizeKb = photoFile.length() / 1024
//                AppLogger.log("StoreActivity", "📸 Підготовка фото '${photoFile.name}' (${fileSizeKb} KB) до відправки...", this@StoreActivity)
//
//                // Проверка пустого файла
//                if (photoFile.length() == 0L) {
//                    AppLogger.log("StoreActivity", "⚠️ Фото '${photoFile.name}' має нульовий розмір — пропуск відправки", this@StoreActivity)
//                    continue
//                }
//
//                val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
//                val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)
//                val logHeaderIdPart = logHeaderId.toRequestBody("text/plain".toMediaTypeOrNull())
//                val questionIdPart = answer.questionId.toRequestBody("text/plain".toMediaTypeOrNull())
//
//                val photoResponse = apiService.uploadPhoto(
//                    token = token,
//                    pharmacyId = pharmacyId,
//                    logHeaderId = logHeaderIdPart,
//                    questionId = questionIdPart,
//                    file = filePart
//                )
//
//                when {
//                    photoResponse.code() == 401 -> {
//                        withContext(Dispatchers.Main) {
//                            AppLogger.log("StoreActivity", "❌ Помилка 401 при фото '${photoFile.name}'", this@StoreActivity)
//                            Toast.makeText(
//                                this@StoreActivity,
//                                "Помилка 401 при фото — авторизуйтеся знову",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            redirectToLogin()
//                        }
//                        return
//                    }
//
//                    photoResponse.isSuccessful -> {
//                        AppLogger.log("StoreActivity", "✅ Фото '${photoFile.name}' (${fileSizeKb} KB) успішно відправлено (${photoResponse.code()})", this@StoreActivity)
//                    }
//
//                    else -> {
//                        AppLogger.log(
//                            "StoreActivity",
//                            "❌ Не вдалося відправити фото '${photoFile.name}' (${fileSizeKb} KB): код ${photoResponse.code()}, повідомлення: ${photoResponse.message()}",
//                            this@StoreActivity
//                        )
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@StoreActivity, "❌ Не вдалося відправити фото '${photoFile.name}' (${fileSizeKb} KB): код ${photoResponse.code()}, повідомлення: ${photoResponse.message()}", Toast.LENGTH_LONG).show()
//                        }
//                        return
//                    }
//                }
//            } catch (e: Exception) {
//                AppLogger.log("StoreActivity", "⚠️ Помилка при відправці фото (${answer.questionId}): ${e.message}", this@StoreActivity)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@StoreActivity, "Помилка при вивантаженні: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//                return
//            }
//        }
//
//
//
//        // ✅ 4. Финал — всё успешно
//        withContext(Dispatchers.Main) {
//            updateProgress(totalSteps, totalSteps, "Готово!")
//            progressBar?.progressDrawable?.setTint(Color.parseColor("#4CAF50")) // зелёный цвет
//            progressTextView?.setTextColor(Color.parseColor("#4CAF50"))
//            progressCountView?.setTextColor(Color.parseColor("#4CAF50"))
//        }
//
//        delay(800) // небольшая пауза для эффекта "завершения"
//
//        withContext(Dispatchers.Main) {
//            val today = LocalDate.now().toString()
//            database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "sent", today)
//            AppLogger.log("StoreActivity", "Вивантаження $surveyId успішне", this@StoreActivity)
//            Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
//        }
//
//    } catch (e: Exception) {
//        withContext(Dispatchers.Main) {
//            AppLogger.log("StoreActivity", "Помилка при вивантаженні $surveyId: ${e.message}", this@StoreActivity)
//            Toast.makeText(this@StoreActivity, "Помилка при вивантаженні: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    } finally {
//        isUploading = false
//        withContext(Dispatchers.Main) { hideLoadingDialog() }
//    }
//}

    private suspend fun uploadSurveyResults(
    surveyId: String,
    surveyTitle: String,
    token: String,
    pharmacyId: Long,
    gammaId: Long,
) {
    isUploading = true
    withContext(Dispatchers.Main) { showLoadingDialog() }

    try {
        val surveyEntity = database.surveyDao().getSurveyById(surveyId)
        val allAnswers = database.resultsSurveyDao().getAnswersForSurvey(surveyId)
        val questions = database.surveyDao().getQuestionsForSurvey(surveyId)

        // Подготавливаем только текстовые ответы (без фото)
        val textOnlyAnswers = allAnswers.map {
            SurveyAnswerUpload(
                questionId = it.questionId,
                imageData = null,
                text = it.textAnswer,
                number = it.numberAnswer,
                percent = it.percentAnswer,
                comment = it.comment,
                single = it.selectedSingleAnswer?.takeIf { s -> !s.contains(",") },
                multi = it.selectedMultiAnswers?.split(",")?.map { s -> s.trim() } ?: emptyList()
            )
        }

        val versionDateStr = BuildConfig.DATE_VERSION
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        val parsedDate: Date? = try {
            dateFormat.parse(versionDateStr)
        } catch (e: Exception) {
            null
        }

        val request = SurveyUploadRequest(
            surveyId = surveyId,
            pharmacyId = pharmacyId,
            gammaId = gammaId,
            dateVersion = parsedDate,
            answers = textOnlyAnswers
        )

        val retrofit = NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL)
        val apiService = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)

//        для запроса подтвержения завершения отправки результатов, так как порт другой
        val finalizeUrl = NetWorkProvider.BASE_URL.replace(":3350", ":3449")
        val retrofitFinalize = NetWorkProvider.provideRetrofit(link = finalizeUrl)
        val apiFinalize = NetWorkProvider.provideApiService(retrofitFinalize, ApiServiceData::class.java)

        // 👉 Подсчёт шагов для прогресса
        val photoAnswers = allAnswers.filter { it.photoPath != null }
        val hasTelegram = surveyEntity?.sendToTelegram == true
//        val totalSteps = 1 + photoAnswers.size + if (hasTelegram) 1 else 0
        val totalSteps = 1 + photoAnswers.size + if (hasTelegram) 1 else 0 + 1 //добавлен еще один шаг для подтвержения окончания передачи (выполнятеся запрос)
        var currentStep = 0

        suspend fun step(message: String) {
            currentStep++
            updateProgress(currentStep, totalSteps, message)
        }

        // 🔹 1. Отправляем текстовые данные
        step("Відправка текстових даних...")
        val response = apiService.uploadSurvey(token, request)

        if (response.code() == 401) {
//            withContext(Dispatchers.Main) {
            withContext(Dispatchers.Main.immediate) {
                AppLogger.log("StoreActivity", "Помилка 401 — токен недійсний", this@StoreActivity)
                Toast.makeText(
                    this@StoreActivity,
                    "Помилка 401 токен недійсний — авторизуйтеся знову",
                    Toast.LENGTH_LONG
                ).show()
                redirectToLogin()
            }
//            return
            return@uploadSurveyResults
        }

        if (!response.isSuccessful) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StoreActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
            }
//            return
            return@uploadSurveyResults
        }

        val responseBody = response.body()
        val logHeaderId = responseBody?.logHeaderId
//        val logHeaderId = "7E1E3317-7F54-4755-BF7D-AB2992E2A972"
            if (logHeaderId.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@StoreActivity, "Помилка: відсутній logHeaderId", Toast.LENGTH_LONG).show()
            }
//            return
            return@uploadSurveyResults
        }

        // 🔹 2. Отправляем в Telegram (если нужно)
        if (hasTelegram) {
            step("Відправка у Telegram...")

            val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val storeName = sharedPreferences.getString("pharmacyName", null)
            val userName = sharedPreferences.getString("selected_user_name", null)

            val categories = database.surveyDao().getCategoriesForSurvey(surveyId)
                .sortedBy { it.orderNumber }

            val telegramAnswers = mutableListOf<Pair<String, String>>()

            for (category in categories) {
                val categoryQuestions = questions
                    .filter { it.categoryId == category.id && it.sendToTelegram }
                    .sortedBy { it.orderNumber }

                for (q in categoryQuestions) {
                    val ans = allAnswers.find { it.questionId == q.id }
                    if (ans != null) {
                        val answerText = when {
                            !ans.selectedAnswersText.isNullOrBlank() -> ans.selectedAnswersText
                            !ans.textAnswer.isNullOrBlank() -> ans.textAnswer
                            ans.numberAnswer != null -> ans.numberAnswer.toString()
                            ans.percentAnswer != null -> "${ans.percentAnswer}%"
                            !ans.comment.isNullOrBlank() -> "Коментар: ${ans.comment}"
                            ans.skipped -> "Без відповіді"
                            else -> ""
                        }
                        telegramAnswers.add(q.text to answerText)
                    }
                }
            }

            if (telegramAnswers.isNotEmpty()) {
                val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
                val message = buildTelegramMessage(
//                    surveyTitle = surveyEntity.title,
                    surveyTitle = surveyEntity?.title ?: surveyTitle, // ✅ если null — использовать аргумент из функции
                    pharmacyName = storeName ?: "",
                    userName = userName ?: "",
                    date = dateStr,
                    answers = telegramAnswers
                )
                TelegramNotifier.sendMessage(message)
                AppLogger.log("TelegramNotifier", "Відправлено у телеграм", this@StoreActivity)
            }
        }

//24.11.25
//        // 🔹 3. Отправляем фото
//        var currentPhotoIndex = 0
//
//        for (index in currentPhotoIndex until photoAnswers.size) {
//            val answer = photoAnswers[index]
//            step("Відправка фото ${index + 1} з ${photoAnswers.size}...")
//
//            val success = uploadPhotoWithRetry(apiService, token, pharmacyId, logHeaderId, answer)
//
//            if (!success) {
//                withContext(Dispatchers.Main) {
//                    AlertDialog.Builder(this@StoreActivity)
//                        .setTitle("Помилка при відправці фото")
//                        .setMessage("Не вдалося відправити фото ${index + 1} з ${photoAnswers.size}. Спробувати ще раз?")
//                        .setCancelable(false)
//                        .setPositiveButton("Спробувати ще раз") { _, _ ->
//                            lifecycleScope.launch {
//                                uploadRemainingPhotos(apiService, token, pharmacyId, logHeaderId, photoAnswers, index)
//                            }
//                        }
//                        .setNegativeButton("Скасувати") { dialog, _ ->
//                            dialog.dismiss()
//                            Toast.makeText(this@StoreActivity, "Вивантаження перервано", Toast.LENGTH_SHORT).show()
//                        }
//                        .show()
//                }
//                return // останавливаем цикл до решения пользователя
//            }
//        }

// 🔹 3. Отправляем фото
        var startIndex = failedPhotoIndex ?: 0        // 👉 Если был сбой ранее — продолжаем с него
        failedPhotoIndex = null                       // Сбрасываем, т.к. начинаем новую серию

        for (index in startIndex until photoAnswers.size) {
            val answer = photoAnswers[index]

            step("Відправка фото ${index + 1} з ${photoAnswers.size}...")

            val success = uploadPhotoWithRetry(
                apiService, token, pharmacyId, logHeaderId, answer,
                index = index + 1,
                total = photoAnswers.size
            )

            if (!success) {
                failedPhotoIndex = index   // 👉 Запоминаем фото, которое упало

                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@StoreActivity)
                        .setTitle("Помилка при відправці фото")
                        .setMessage("Фото ${index + 1} з ${photoAnswers.size} не відправлено. Спробувати ще раз?")
                        .setCancelable(false)
                        .setPositiveButton("Спробувати ще раз") { _, _ ->
                            lifecycleScope.launch {
                                uploadRemainingPhotos(
                                    apiService, token, pharmacyId,
                                    logHeaderId,
                                    photoAnswers,
                                    index
                                )
                            }
                        }
                        .setNegativeButton("Скасувати") { dialog, _ ->
                            dialog.dismiss()
                            Toast.makeText(
                                this@StoreActivity,
                                "Вивантаження перервано",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .show()
                }
//                return
                return@uploadSurveyResults
            }
        }

        // 🔹 4. Підтвердження завершення передачі
        step("Підтвердження завершення...")

        AppLogger.log(
            "StoreActivity",
            "📡 finalize upload logHeaderId=$logHeaderId pharmacyId=$pharmacyId",
            this@StoreActivity
        )

        val finalizeResponse = apiService.finalizeSurveyUpload(
            token = token,
            logHeaderId = logHeaderId,
//            pharmacyId = pharmacyId
        )

        if (!finalizeResponse.isSuccessful) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@StoreActivity,
                    "Помилка підтвердження передачі: ${finalizeResponse.code()}",
                    Toast.LENGTH_LONG
                ).show()
            }

            AppLogger.log(
                "StoreActivity",
                "❌ отправка запроса про завершения передачи результатов вернуло ошибку: error ${finalizeResponse.code()}",
                this@StoreActivity
            )

            return@uploadSurveyResults
        }

        AppLogger.log(
            "StoreActivity",
            "✅ отправлен запрос для подтверджения завершения отравки результатов",
            this@StoreActivity
        )

        // ✅ 5. Финал — всё успешно
        withContext(Dispatchers.Main) {
            updateProgress(totalSteps, totalSteps, "Готово!")
            progressBar?.progressDrawable?.setTint(Color.parseColor("#4CAF50")) // зелёный цвет
            progressTextView?.setTextColor(Color.parseColor("#4CAF50"))
            progressCountView?.setTextColor(Color.parseColor("#4CAF50"))
        }

        delay(800) // небольшая пауза для эффекта "завершения"

        withContext(Dispatchers.Main) {
            val today = LocalDate.now().toString()
            database.resultsSurveyDao().updateSurveyStatusWithDate(surveyId, "sent", today)
            AppLogger.log("StoreActivity", "Вивантаження $surveyId успішне", this@StoreActivity)
            Toast.makeText(this@StoreActivity, "Опитування $surveyTitle відправлено!", Toast.LENGTH_SHORT).show()
        }

    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            AppLogger.log("StoreActivity", "Помилка при вивантаженні $surveyId: ${e.message}", this@StoreActivity)
            Toast.makeText(this@StoreActivity, "Помилка при вивантаженні: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } finally {
        isUploading = false
        withContext(Dispatchers.Main) { hideLoadingDialog() }
    }
}

    private suspend fun uploadPhotoWithRetry(
        apiService: ApiServiceData,
        token: String,
        pharmacyId: Long,
        logHeaderId: String,
        answer: SurveyAnswerEntity,
        maxRetries: Int = 3,
        index: Int,
        total: Int
    ): Boolean {

        val photoPath = answer.photoPath ?: return true
        val photoFile = File(photoPath)
        if (!photoFile.exists() || photoFile.length() == 0L) return true

        val filePart = MultipartBody.Part.createFormData(
            "file", photoFile.name,
            photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
        val logHeaderIdPart = logHeaderId.toRequestBody("text/plain".toMediaTypeOrNull())
        val questionIdPart = answer.questionId.toRequestBody("text/plain".toMediaTypeOrNull())

        var attempt = 0

        while (attempt < maxRetries) {

            AppLogger.log(
                "StoreActivity",
                "📤 Відправка фото $index з $total — спроба ${attempt + 1}/$maxRetries (файл: ${photoFile.name})",
                this@StoreActivity
            )
            val networkInfo = NetworkUtils.getNetworkDescription(this@StoreActivity)
            AppLogger.log("StoreActivity", "🌐 Мережа під час відправки фото №$index '${photoFile.name}': $networkInfo", this@StoreActivity)

            try {
                val response = apiService.uploadPhoto(
                    token = token,
                    pharmacyId = pharmacyId,
                    logHeaderId = logHeaderIdPart,
                    questionId = questionIdPart,
                    file = filePart
                )

                if (response.isSuccessful) {
                    AppLogger.log(
                        "StoreActivity",
                        "✅ Фото $index з $total успішно відправлено",
                        this@StoreActivity
                    )
                    return true
                } else {
                    AppLogger.log(
                        "StoreActivity",
                        "❌ Фото $index з $total — помилка, код ${response.code()}",
                        this@StoreActivity
                    )
                }

            } catch (e: Exception) {
                AppLogger.log(
                    "StoreActivity",
                    "⚠️ Фото $index з $total — помилка: ${e.message}",
                    this@StoreActivity
                )
            }

            attempt++
            delay(1200)
        }

        return false
    }


    private fun uploadRemainingPhotos(
        apiService: ApiServiceData,
        token: String,
        pharmacyId: Long,
        logHeaderId: String,
        photoAnswers: List<SurveyAnswerEntity>,
        startIndex: Int
    ) {

        lifecycleScope.launch {

            for (index in startIndex until photoAnswers.size) {

                val answer = photoAnswers[index]

                val success = uploadPhotoWithRetry(
                    apiService, token, pharmacyId,
                    logHeaderId,
                    answer,
                    index = index + 1,
                    total = photoAnswers.size
                )

                if (!success) {
                    failedPhotoIndex = index   // 👉 Новая точка остановки

                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(this@StoreActivity)
                            .setTitle("Помилка при відправці фото")
                            .setMessage(
                                "Фото ${index + 1} з ${photoAnswers.size} не відправлено. Спробувати ще раз?"
                            )
                            .setCancelable(false)
                            .setPositiveButton("Спробувати ще раз") { _, _ ->
                                uploadRemainingPhotos(
                                    apiService, token, pharmacyId,
                                    logHeaderId,
                                    photoAnswers,
                                    index
                                )
                            }
                            .setNegativeButton("Скасувати") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@StoreActivity,
                    "✅ Всі фото успішно відправлено!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }



    private fun compressImageFile(inputFile: File, quality: Int = 70): File {
        val bitmap = BitmapFactory.decodeFile(inputFile.absolutePath)
        val compressedFile = File.createTempFile("compressed_", ".jpg", inputFile.parentFile)

        FileOutputStream(compressedFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        // Логи размеров
        val beforeKB = inputFile.length() / 1024
        val afterKB = compressedFile.length() / 1024
        AppLogger.log("PhotoUpload", "Сжатие фото: ${inputFile.name} до=$beforeKB KB → после=$afterKB KB", null)

        return compressedFile
    }


    fun buildTelegramMessage(
        surveyTitle: String,
        pharmacyName: String,
        userName: String,
        date: String,
        answers: List<Pair<String, String>>
    ): String {
        val builder = StringBuilder()
//        builder.appendLine("🏪 ТТ: $pharmacyName")
//        builder.appendLine("📋 Опитування: $surveyTitle")
//        builder.appendLine("👤 Користувач: $userName")
//        builder.appendLine("📅 Дата: $date")
//        builder.appendLine()
//
//        answers.forEach { (question, answer) ->
//            builder.appendLine("❓ $question")
//            builder.appendLine("✅ $answer")
//            builder.appendLine()
        builder.appendLine(" ТТ: $pharmacyName")
        builder.appendLine(" Опитування: $surveyTitle")
        builder.appendLine(" Користувач: $userName")
        builder.appendLine(" Дата: $date")
        builder.appendLine()

        answers.forEach { (question, answer) ->
            builder.appendLine(" $question")
            builder.appendLine(" $answer")
//            builder.appendLine()
        }

        return builder.toString()
    }



    private fun redirectToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }


//    private fun showLoadingDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setView(R.layout.dialog_progress) // создаешь layout с ProgressBar
//        builder.setCancelable(false)
//        progressDialog = builder.create()
//        progressDialog.show()
//    }

//    14.10.25
//private fun showLoadingDialog() {
//    if (::progressDialog.isInitialized && progressDialog.isShowing) return
//    progressDialog = AlertDialog.Builder(this)
//        .setView(R.layout.dialog_progress)
//        .setCancelable(false)
//        .create()
//    progressDialog.show()
//}
private fun showLoadingDialog() {
    if (::progressDialog.isInitialized && progressDialog.isShowing) return

    val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
    progressTextView = dialogView.findViewById(R.id.progressText)
    progressCountView = dialogView.findViewById(R.id.progressCount)
    progressBar = dialogView.findViewById(R.id.progressBar)

    progressDialog = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCancelable(false)
        .create()

    progressDialog.show()
}

    private fun hideLoadingDialog() {
        if (::progressDialog.isInitialized && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
    override fun onBackPressed() {
        if (isUploading) {
            Toast.makeText(this, "Йде вивантаження... Зачекайте", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }

    private suspend fun updateProgress(current: Int, total: Int, message: String) {
        withContext(Dispatchers.Main) {
            val percent = ((current.toFloat() / total) * 100).toInt()
            progressTextView?.text = message
            progressCountView?.text = "$current / $total"

            progressBar?.let { bar ->
                ObjectAnimator.ofInt(bar, "progress", bar.progress, percent).apply {
                    duration = 400
                    interpolator = DecelerateInterpolator()
                    start()
                }
            }
        }
    }

    private fun finishProgressSuccess() {
        progressTextView?.text = "✅ Готово!"
        progressCountView?.text = ""
        progressBar?.let { bar ->
            ObjectAnimator.ofInt(bar, "progress", bar.progress, 100).apply {
                duration = 600
                interpolator = DecelerateInterpolator()
                start()
            }
            bar.progressDrawable.setTint(Color.parseColor("#4CAF50")) // зелёный цвет
        }
    }


    private fun saveRequestToFile(context: Context, request: SurveyUploadRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(request)

                val fileName = "survey_request_${System.currentTimeMillis()}.json"
                AppLogger.log(
                    "SelectionActivity",
                    "Вивантаження результатів, json запиту збережено в файл: ${fileName}",
                    this@StoreActivity
                )
                val file = File(context.getExternalFilesDir(null), fileName)
                file.writeText(json)
                Log.d("RequestLog", "Запрос сохранен в файл: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e("RequestLog", "Ошибка при сохранении запроса: ${e.message}")
            }
        }
    }
}




