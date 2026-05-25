package com.example.check_911

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.check_911.data.db.entity.CategoryQuestionsEntity
import com.example.check_911.data.db.entity.OptionForQuestionsEntity
import com.example.check_911.data.db.entity.QuestionEntity
import com.example.check_911.data.db.entity.SurveyEntity
import com.example.check_911.data.utils.AppLogger
import com.example.check_911.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import android.Manifest
import android.text.method.DigitsKeyListener
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.check_911.data.UpdateChecker
import com.example.check_911.data.db.dao.DaoIpAddress
import com.example.check_911.data.db.entity.IpAddressEntity
import com.example.check_911.data.utils.TelegramLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.check_911.data.SurveyReminderScheduler
import com.example.check_911.data.utils.NetworkUtils


class MainActivity : ComponentActivity() {
    private val authViewModel: AuthorizationViewModel by viewModel()
    private val ipAddressViewModel: IpAddressViewModel by viewModel()
    lateinit var binding: ActivityMainBinding
//    private val dateVersion = "24.07.25 global"
//    private val dateVersion = BuildConfig.DATE_VERSION
    private val dateVersion = BuildConfig.DATE_VERSION + if (BuildConfig.DEBUG_VERSION) "\n(тестова збірка)" else ""
    private lateinit var textIp: TextView
    private lateinit var ipTextView: TextView
    private lateinit var installPermissionLauncher: ActivityResultLauncher<Intent>

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "✅ Дозвіл на сповіщення надано", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Сповіщення можуть не відображатися", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        // 🔔 Проверка и запрос разрешения для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        SurveyReminderScheduler.scheduleAll(this)


//        installPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if (packageManager.canRequestPackageInstalls()) {
//                    UpdateChecker.apkFile?.let { UpdateChecker.installApk(this, it) }
//                } else {
//                    Toast.makeText(this, "Дозвіл INSTALL_UNKNOWN_APPS не надано!", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        }


        installPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            UpdateChecker.continueUpdateIfPermissionGranted(this)
        }

        AppLogger.log("MainActivity", "Запуск cheack-911, версия: ${dateVersion}", this@MainActivity)

        // Логируем марку и модель устройства
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        AppLogger.log("DeviceDebug", "Запуск на устройстве: $manufacturer $model", this)

        val usernameEditText: EditText = findViewById(R.id.usernameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val passwordToggle: ImageView = findViewById(R.id.passwordToggle)
        val rememberMeCheckBox: CheckBox = findViewById(R.id.rememberMeCheckBox)
        val loginButton: Button = findViewById(R.id.loginButton)

        val checkIp: Button = findViewById(R.id.buttonCheckIp)
        textIp = findViewById(R.id.textView2)

        val sendLog: Button = findViewById(R.id.button2)

        val enterIpButton: Button = findViewById(R.id.enterIpButton)
        ipTextView = findViewById<TextView>(R.id.ipTextView)

        var textDateVersion = findViewById<TextView>(R.id.textDateVersion)

        textDateVersion.text = dateVersion

        var isPasswordVisible = false

        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setColorFilter(Color.BLACK) // Глаз становится чёрным
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setColorFilter(Color.GRAY) // Глаз становится серым
            }
            passwordEditText.setSelection(passwordEditText.text.length) // Оставить курсор в конце текста
        }

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Загрузка сохраненных данных
        val savedUsername = sharedPreferences.getString("username", "")
        val savedPassword = sharedPreferences.getString("password", "")
        val isRemembered = sharedPreferences.getBoolean("remember_me", false)

        if (isRemembered) {
            usernameEditText.setText(savedUsername)
            passwordEditText.setText(savedPassword)
            AppLogger.log("MainActivity", "Вставка збережених логіна і пароля (логін: ${savedUsername}, пароль: ${savedPassword}", this@MainActivity)
            rememberMeCheckBox.isChecked = true
        }

//        GlobalScope.launch {// для теста
//            insertTestData()
//        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val rememberMe = rememberMeCheckBox.isChecked

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val dao = (application as App).database.daoIpAddress()

                lifecycleScope.launch {
////                    saveDeviceIpToDb(this@MainActivity, dao) // Сохраняем IP

//                    if (BuildConfig.DEBUG_VERSION) {
                        authViewModel.login(username, password)
                        AppLogger.log(
                            "MainActivity",
                            "Вхід за логіном: ${username} і паролем: ${password}",
                            this@MainActivity
                        )
//                    } else {
//                        if (NetworkUtils.isWifiGammaActive(this@MainActivity)) {
//                        // Ок, выполняем запросы
//                        authViewModel.login(username, password)
//                        AppLogger.log(
//                            "MainActivity",
//                            "Вхід за логіном: ${username} і паролем: ${password}",
//                            this@MainActivity
//                        )
//                    } else {
//                        Toast.makeText(this@MainActivity, "Підключіться до Wi-Fi GAMMA. Також перевірте, що ви не використовуєте мобільну мережу.", Toast.LENGTH_LONG).show()
//                    }
//                    }
                }
            } else {
                Toast.makeText(this, "Будь ласка, введіть логін та пароль", Toast.LENGTH_SHORT).show()
            }

        }

        authViewModel.authState.observe(this) { result ->
            result.onSuccess { response ->
                val editor = sharedPreferences.edit()
                if (rememberMeCheckBox.isChecked) {
                    editor.putString("username", usernameEditText.text.toString())
                    editor.putString("password", passwordEditText.text.toString())
                    editor.putBoolean("remember_me", true)
                } else {
                    editor.clear()
                }

//                очиска при смене тт
                val oldPharmacyId = sharedPreferences.getLong("pharmacy_Id", -1L)
                val newPharmacyId = response.pharmacyId

                if (oldPharmacyId != -1L && oldPharmacyId != newPharmacyId) {
                    AppLogger.log("MainActivity", "Зміна торгової точки: з ID $oldPharmacyId на $newPharmacyId. Очищення даних...", this)

                    val db = (application as App).database
                    lifecycleScope.launch {


                        val allAnswers = db.resultsSurveyDao().getAllSurveyAnswers()
                        allAnswers.forEach { answer ->
                            answer.photoPath?.let {
                                val file = File(it)
                                if (file.exists()) {
                                    val deleted = file.delete()
                                    if (deleted) {
                                        AppLogger.log("MainActivity", "Видалено фото: ${file.name}", this@MainActivity)
                                    }
                                }
                            }
                        }

                        db.resultsSurveyDao().deleteAllSurvey()
                        AppLogger.log("MainActivity", "Очищено всі відповіді та результати опитувань", this@MainActivity)
                    }



                    val dir = getExternalFilesDir(null)
                    dir?.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".json")) {
                            val deleted = file.delete()
                            if (deleted) {
                                AppLogger.log("MainActivity", "Видалено файл: ${file.name}", this)
                            }
                        }
                    }
                }

                editor.putString("auth_token", response.token)
                editor.putString("pharmacyName", response.pharmacyName)
                editor.putString("pharmacyAddress", response.pharmacyAddress)
                editor.putLong("pharmacy_Id", response.pharmacyId)
                editor.apply()

                AppLogger.log("MainActivity", "Авторизація успішна", this@MainActivity)
                AppLogger.log("MainActivity", "Отримано auth_token: ${response.token}", this@MainActivity)
                AppLogger.log("MainActivity", "Отримано pharmacyName: ${response.pharmacyName}", this@MainActivity)
                AppLogger.log("MainActivity", "Отримано pharmacyAddress: ${response.pharmacyAddress}", this@MainActivity)
                AppLogger.log("MainActivity", "Отримано pharmacy_Id: ${response.pharmacyId}", this@MainActivity)
                Toast.makeText(this, "Вхід успішний", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoadingActivity::class.java))
                finish()
            }

            result.onFailure { e ->
                if (e.message == "Невірний логін або пароль!") {
                    AppLogger.log("MainActivity", "Авторизація невдала. Невірний логін або пароль!")
                    Toast.makeText(this, "Невірний логін або пароль!", Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
//                deleteIpAddress()
                        ipAddressViewModel.deleteIpAddress()
                    }
                } else if (e.message?.contains("Unable to resolve host") == true) {
                    if (BuildConfig.DEBUG_VERSION) {
                        showServerIpInputDialog()
                    }
                } else {
                    if (BuildConfig.DEBUG_VERSION) {
                        showServerIpInputDialog()
                    }
                        AppLogger.log("MainActivity", "Авторизація невдала. Помилка: ${e.message}")
                        Toast.makeText(this, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }


        }




        checkIp.setOnClickListener{
//            val ip = getGatewayIpAddress(this)
//            textIp.setText(ip ?: "IP не визначено")
//            lifecycleScope.launch {
//                val dao = (application as App).database.daoIpAddress()
//                dao.setIpAddress(IpAddressEntity(ip?:"")) // ← используй свою Entity
//            }

            val intent = Intent(this, QrScannerActivity::class.java)
            startActivity(intent)


        }

        sendLog.setOnClickListener{
            sendLogsToTelegram()
//            lifecycleScope.launch {
//                val dao = (application as App).database.daoIpAddress()
//                dao.deleteIpAddress()
//            }
//            deleteIpAddress()
        }

        enterIpButton.setOnClickListener {
            showServerIpInputDialog()
        }
    }

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onResume() {
        super.onResume()
////        UpdateChecker.checkForUpdates(this)
//
//        UpdateChecker.checkForUpdate(this, installPermissionLauncher, false)
//        Log.d("UpdateChecker", "запуск onResume")
        if (!UpdateChecker.isUpdating) {
            UpdateChecker.checkForUpdate(this, installPermissionLauncher, false)
            Log.d("UpdateChecker", "запуск onResume")
        } else {
            Log.d("UpdateChecker", "Проверка обновлений пропущена — уже идет обновление")
        }

        val networkInfo = NetworkUtils.getNetworkDescription(this@MainActivity)
//        Toast.makeText(this, "🌐 Мережа: $networkInfo", Toast.LENGTH_LONG).show()
        AppLogger.log("StoreActivity", "🌐 Мережа: $networkInfo", this@MainActivity)

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = (application as App).database.daoIpAddress()
            val savedIp = dao.getIpAddress() ?: ""
            withContext(Dispatchers.Main) {
                ipTextView.text = "IP: $savedIp"
            }
        }
    }

    private suspend fun deleteIpAddress() {
        val dao = (application as App).database.daoIpAddress()
        dao.deleteIpAddress()
    }

    private fun showServerIpInputDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = (application as App).database.daoIpAddress()
            val savedIp = dao.getIpAddress()

            withContext(Dispatchers.Main) {
                val editText = EditText(this@MainActivity).apply {
                    hint = "Введіть аптеки IP"
//                    inputType = InputType.TYPE_CLASS_TEXT
//                    inputType = InputType.TYPE_CLASS_NUMBER
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    keyListener = DigitsKeyListener.getInstance("0123456789.")
                    setText(savedIp)
                }

                AlertDialog.Builder(this@MainActivity)
//                    .setTitle("IP-адресу аптеки не вдалося отримати автоматично.\n Введіть IP аптеки вручну.")
                    .setTitle("Введіть IP аптеки вручну.")
                    .setView(editText)
                    .setPositiveButton("Зберегти") { _, _ ->
                        val ip = editText.text.toString().trim()
                        if (ip.isNotEmpty()) {
                            lifecycleScope.launch(Dispatchers.IO) {
//                                dao.setIpAddress(IpAddressEntity(ipaddress = ip))
                                ipAddressViewModel.setIpAddress(ip)
                            }
                            lifecycleScope.launch(Dispatchers.Main) {
                                ipTextView.text = "IP: $ip"
                            }
                            Toast.makeText(this@MainActivity, "IP-адреса збережена: $ip", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Скасувати", null)
                    .show()
            }
        }
    }


    private fun requestWifiPermissionsIfNeeded() {
        val permissions = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun sendLogsToTelegram() {
        val dir = getExternalFilesDir(null)
        Log.d("TelegramLogger", "вызван sendLogsToTelegram() ")

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val storeName = sharedPreferences.getString("pharmacyName", null)
        val storeId = sharedPreferences.getLong("pharmacy_Id", 1L)

        dir?.listFiles()?.forEach { file ->
            if (file.name.endsWith(".txt") || file.name.endsWith(".json")) {
                TelegramLogger.sendFileToTelegram(
                    context = this,
                    file = file,
                    caption = "ID: ${storeId}, ${storeName} Файл: ${file.name}"
                )
            }
        }
    }

    suspend fun saveDeviceIpToDb(context: Context, dao: DaoIpAddress) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo?.ipAddress ?: 0

        if (ipInt != 0) {
            val ip = String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )

            Log.d("IP_INIT", "Сохранение IP в БД: $ip")
            AppLogger.log(
                "MainActivity",
                "Збереження IP в БД: $ip",
                this@MainActivity
            )
            textIp.setText(ip ?: "IP не визначено")
            dao.setIpAddress(IpAddressEntity(ip)) // ← используй свою Entity
        } else {
            textIp.setText("IP не визначено")
            Log.w("IP_INIT", "Не удалось определить IP устройства")
            AppLogger.log("MainActivity", "Невдалося встановити ІР")
        }
    }


//    private suspend fun insertTestData() {
//        val db = (application as App).database
//        lifecycleScope.launch {
//            db.surveyDao().clearSurveys() // Очистка таблиц перед добавлением новых данных
//            db.surveyDao().clearCategories()
//            db.surveyDao().clearQuestions()
//            db.surveyDao().clearOptions()
////            db.resultsSurveyDao().deleteAllSurvey()
//            // Опросник №1
//            val survey1 = SurveyEntity(
//                id = "9B8CE320-DE73-45E9-82C6-E6DB728EEA2F",
//                createdAt = "2025-03-21 15:57:36",
//                createdBy = 24,
//                title = "Оцінка викладки товару",
//                periodId = 1,
//                periodDescription = "щоденно",
//                typeId = 1,
//                typeDescription = "звітність",
//                isVisible = true
//            )
//            db.surveyDao().insertSurveys(listOf(survey1))
//
//            // Категория 1
//            val category1 = CategoryQuestionsEntity(
//                id = "cat1",
//                surveyId = survey1.id,
//                description = "Загальний вигляд"
//            )
//            db.surveyDao().insertCategories(listOf(category1))
//
//            val question1 = QuestionEntity(
//                id = "q1",
//                categoryId = category1.id,
//                text = "Чи присутній товар на полиці?",
//                requiredIfYes = false,
//                requiredIfNo = false,
//                alwaysRequired = true,
//                numberInput = false,
//                percentInput = false,
//                textInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = true,
//                isImportant = true
//            )
//            db.surveyDao().insertQuestions(listOf(question1))
//
//            val options1 = listOf(
//                OptionForQuestionsEntity("opt1", question1.id, "Так", true),
//                OptionForQuestionsEntity("opt2", question1.id, "Ні", false),
//                OptionForQuestionsEntity("opt3", question1.id, "Можливо", false)
//            )
//            db.surveyDao().insertOptions(options1)
//
//            val question2 = QuestionEntity(
//                id = "q2",
//                categoryId = category1.id,
//                text = "Вкажіть % заповнення полиці",
//                percentInput = true,
//                numberInput = false,
//                textInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = false,
//                alwaysRequired = false,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                isImportant = false
//            )
//            db.surveyDao().insertQuestions(listOf(question2))
//
//            // Категория 2
//            val category2 = CategoryQuestionsEntity(
//                id = "cat2",
//                surveyId = survey1.id,
//                description = "Додаткове оформлення"
//            )
//            db.surveyDao().insertCategories(listOf(category2))
//
//            val question3 = QuestionEntity(
//                id = "q3",
//                categoryId = category2.id,
//                text = "Який вигляд має додаткове оформлення?",
//                textInput = true,
//                numberInput = false,
//                percentInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = false,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                alwaysRequired = false,
//                isImportant = false
//            )
//            db.surveyDao().insertQuestions(listOf(question3))
//
//            val question4 = QuestionEntity(
//                id = "q4",
//                categoryId = category2.id,
//                text = "Скільки промо-матеріалів використано?",
//                numberInput = true,
//                percentInput = false,
//                textInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = false,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                alwaysRequired = false,
//                isImportant = true
//            )
//            db.surveyDao().insertQuestions(listOf(question4))
//
//
//            // Опросник №2
//            val survey2 = SurveyEntity(
//                id = "F4231802-BC81-4D41-8D9B-4BC0A3E77AF4",
//                createdAt = "2025-03-21 17:21:53",
//                createdBy = 24,
//                title = "Якість товару",
//                periodId = 2,
//                periodDescription = "щотижнево",
//                typeId = 2,
//                typeDescription = "перевірка викладки",
//                isVisible = true
//            )
//            db.surveyDao().insertSurveys(listOf(survey2))
//
//            // Категория 1
//            val category3 = CategoryQuestionsEntity(
//                id = "cat3",
//                surveyId = survey2.id,
//                description = "Якість упаковки"
//            )
//            db.surveyDao().insertCategories(listOf(category3))
//
//            val question5 = QuestionEntity(
//                id = "q5",
//                categoryId = category3.id,
//                text = "Чи є пошкодження упаковки?",
//                singleChoiceInput = true,
//                multiChoiceInput = false,
//                textInput = false,
//                percentInput = false,
//                numberInput = false,
//                alwaysRequired = true,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                isImportant = true
//            )
//            db.surveyDao().insertQuestions(listOf(question5))
//
//            val options2 = listOf(
//                OptionForQuestionsEntity("opt4", question5.id, "Так", false),
//                OptionForQuestionsEntity("opt5", question5.id, "Ні", true)
//            )
//            db.surveyDao().insertOptions(options2)
//
//            val question6 = QuestionEntity(
//                id = "q6",
//                categoryId = category3.id,
//                text = "Опишіть виявлені дефекти",
//                textInput = true,
//                numberInput = false,
//                percentInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = false,
//                alwaysRequired = false,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                isImportant = false
//            )
//            db.surveyDao().insertQuestions(listOf(question6))
//
//            // Категория 2
//            val category4 = CategoryQuestionsEntity(
//                id = "cat4",
//                surveyId = survey2.id,
//                description = "Фотофіксація"
//            )
//            db.surveyDao().insertCategories(listOf(category4))
//
//            val question7 = QuestionEntity(
//                id = "q7",
//                categoryId = category4.id,
//                text = "Зробіть фото пошкодженої упаковки",
//                alwaysRequired = true,
//                requiredIfYes = false,
//                requiredIfNo = false,
//                numberInput = false,
//                percentInput = false,
//                textInput = false,
//                singleChoiceInput = false,
//                multiChoiceInput = false,
//                isImportant = false
//            )
//            db.surveyDao().insertQuestions(listOf(question7))
//        }
//    }
}

