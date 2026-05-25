package com.example.check_911

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.check_911.data.db.MainDb
import com.example.check_911.data.networking.networking.models.TaskAnswerRequest
import com.example.check_911.data.utils.AppLogger
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.text.TextWatcher
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.FileOutputStream
import kotlin.getValue
import android.app.AlertDialog
import android.graphics.drawable.Drawable

import com.bumptech.glide.load.engine.DiskCacheStrategy

import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.DataSource


class TaskDetailsActivity : AppCompatActivity() {

    private lateinit var textTask: TextView
    private lateinit var textDate: TextView
    private lateinit var imageProblem: ImageView
    private lateinit var editComment: EditText
    private lateinit var textCounter: TextView
    private lateinit var imageResult: ImageView
    private lateinit var btnCamera: Button
    private lateinit var btnSend: Button

    private val viewModel: TaskDetailsViewModel by viewModel()
    private val database by lazy { (application as App).database }

    private var currentPhotoPath: String? = null
    private var taskId: String? = null

    private var loadingDialog: AlertDialog? = null

    private val api by lazy {
        NetWorkProvider.provideApiService(
            NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL),
            ApiServiceData::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)

        taskId = intent.getStringExtra("task_id")

        if (taskId == null) {
            finish()
            return
        }

        viewModel.init(taskId!!)

        initViews()
        setupObservers()
        setupTextCounter()
        loadData()
        setupListeners()
    }

    private fun initViews() {
        textTask = findViewById(R.id.textTask)
        textDate = findViewById(R.id.textDate)
        imageProblem = findViewById(R.id.imageProblem)
        editComment = findViewById(R.id.editComment)
        textCounter = findViewById(R.id.textCounter)
        imageResult = findViewById(R.id.imageResult)
        btnCamera = findViewById(R.id.btnCamera)
        btnSend = findViewById(R.id.btnSend)

        imageResult.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    override fun onDestroy() {
        super.onDestroy()
        hideLoading() // 🔥 гарантированно убираем диалог
    }

    private fun setupTextCounter() {
        editComment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()
                textCounter.text = "${text?.length ?: 0}/600"

                viewModel.saveComment(text)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.currentPhotoPath.collect { path ->
                if (!path.isNullOrBlank()) {
                    currentPhotoPath = path

                    imageResult.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(this@TaskDetailsActivity)
                        .load(File(path))
                        .into(imageResult)
                }
            }
        }
    }

//    private fun loadData() {
//        taskId = intent.getStringExtra("task_id") ?: return
//
//        lifecycleScope.launch {
//            val task = database.taskDao().getTaskById(taskId!!) ?: return@launch
//
//            textTask.text = task.taskText ?: ""
//            textDate.text = "Дата створення задачі: ${formatDate(task.createdAt)}"
//
//            Glide.with(this@TaskDetailsActivity)
//                .load(task.imgUrl)
//                .into(imageProblem)
//
////            editComment.setText(task.taskAnswer ?: "")
//            lifecycleScope.launch {
//                val result = database.taskResultDao().getResult(taskId!!)
//                editComment.setText(result?.comment ?: "")
//            }
//
//            if (!task.localPhotoPath.isNullOrBlank()) {
//                currentPhotoPath = task.localPhotoPath
//
//                imageResult.scaleType = ImageView.ScaleType.CENTER_CROP
//
//                Glide.with(this@TaskDetailsActivity)
//                    .load(File(task.localPhotoPath))
//                    .error(R.drawable.ic_add_foto)
//                    .into(imageResult)
//            }
//        }
//    }
private fun loadData() {
    taskId = intent.getStringExtra("task_id")

    if (taskId == null) {
        AppLogger.log("TaskDetails", "❌ taskId == null")
        finish()
        return
    }

    // 👉 инициализируем ViewModel
    viewModel.init(taskId!!)

    lifecycleScope.launch {
        try {
            val task = database.taskDao().getTaskById(taskId!!)

            if (task == null) {
                AppLogger.log("TaskDetails", "❌ Задача не найдена")
                finish()
                return@launch
            }

            // ================= UI =================

            textTask.text = task.taskText ?: ""
            textDate.text = "Дата створення задачі: ${formatDate(task.createdAt)}"

            // ================= ФОТО ПРОБЛЕМЫ =================

            if (!task.imgUrl.isNullOrBlank()) {

                AppLogger.log("IMG_DEBUG", "🌐 URL: ${task.imgUrl}")

                Glide.with(this@TaskDetailsActivity)
                    .asBitmap() // 🔥 фикс: не даем Glide думать что это видео
                    .load(task.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
//                    .listener(object : RequestListener<Bitmap> {
//
//                        override fun onLoadFailed(
//                            e: GlideException?,
//                            model: Any?,
//                            target: Target<Bitmap>?,
//                            isFirstResource: Boolean
//                        ): Boolean {
//                            AppLogger.log("IMG_ERROR", "❌ Ошибка загрузки: ${e?.message}")
//                            e?.logRootCauses("IMG_ERROR_FULL")
//                            return false
//                        }
//
//                        override fun onResourceReady(
//                            resource: Bitmap,
//                            model: Any,
//                            target: Target<Bitmap>?,
//                            dataSource: DataSource,
//                            isFirstResource: Boolean
//                        ): Boolean {
//                            AppLogger.log("IMG_SUCCESS", "✅ Фото проблемы загружено")
//                            return false
//                        }
//                    })
                    .error(R.drawable.ic_add_foto)
                    .into(imageProblem)

            } else {
                AppLogger.log("IMG_DEBUG", "⚠️ imgUrl пустой")
            }

            // ================= ВОССТАНОВЛЕНИЕ ДАННЫХ =================

            val result = database.taskResultDao().getResult(taskId!!)

            editComment.setText(result?.comment ?: "")

            if (!result?.photoPath.isNullOrBlank()) {
                currentPhotoPath = result?.photoPath

                imageResult.scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(this@TaskDetailsActivity)
                    .load(File(result!!.photoPath))
                    .error(R.drawable.ic_add_foto)
                    .into(imageResult)

                AppLogger.log("TaskDetails", "📷 Восстановлено фото: ${result.photoPath}")
            } else {
                AppLogger.log("TaskDetails", "ℹ️ Фото отсутствует")
            }

        } catch (e: Exception) {
            AppLogger.log("TaskDetails", "❌ loadData error: ${e.message}")
        }
    }
}
    private fun setupListeners() {
        btnCamera.setOnClickListener { checkPermissionsAndOpenCamera() }
        btnSend.setOnClickListener { sendTask() }
    }

    private fun formatDate(date: String?): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            formatter.format(parser.parse(date)!!)
        } catch (e: Exception) {
            date ?: ""
        }
    }

    // ================== ОТПРАВКА ==================

    private fun sendTask() {
        val comment = editComment.text.toString()
        val photoPath = viewModel.currentPhotoPath.value
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val gammaId = sharedPreferences.getLong("selected_user_idGamma", -1L)

        if (comment.isBlank() && currentPhotoPath == null) {
            Toast.makeText(this, "Додайте опис або фото", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading() // ✅ ПОКАЗАЛИ ЛОАДЕР

        lifecycleScope.launch {
            try {
//                val base64Image = currentPhotoPath?.let { encodeToBase64(it) }
                val base64Image = photoPath?.let { encodeToBase64(it) }

                val response = api.sendTaskAnswer(
                    taskId = taskId!!,
                    token = getToken(),
                    body = TaskAnswerRequest(comment, base64Image, gammaId)
                )

                showLoading() // ✅ ПОКАЗАЛИ ЛОАДЕР

                if (response.isSuccessful) {
//                    database.taskDao().clearTaskProgress(taskId!!)
                    // 🧹 1. удаляем результат выполнения
                    database.taskResultDao().deleteByTaskId(taskId!!)

                    // 🧹 2. удаляем саму задачу
                    database.taskDao().deleteTaskById(taskId!!)

                    // 🧹 удаляем файл фото
                    deletePhotoIfExists(photoPath)

                    hideLoading()

                    Toast.makeText(this@TaskDetailsActivity, "Завдання надіслано", Toast.LENGTH_SHORT).show()

                    // 🔙 Возврат с результатом
                    setResult(RESULT_OK)
                    finish()
                } else {
                    hideLoading()
                    Toast.makeText(this@TaskDetailsActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                hideLoading()

                Toast.makeText(this@TaskDetailsActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getToken(): String {
        return getSharedPreferences("user_prefs", MODE_PRIVATE)
            .getString("auth_token", "") ?: ""
    }

    private fun encodeToBase64(path: String): String {
        val bytes = File(path).readBytes()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }



    private fun showLoading() {
        if (loadingDialog == null) {
            val view = layoutInflater.inflate(R.layout.dialog_loading, null)

            loadingDialog = AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false) // ❗ нельзя закрыть
                .create()
        }

        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    private fun deletePhotoIfExists(path: String?) {
        if (path.isNullOrBlank()) return

        try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                AppLogger.log(
                    "PhotoDelete",
                    if (deleted) "🗑️ Фото удалено: $path" else "⚠️ Не удалось удалить: $path",
                    this
                )
            }
        } catch (e: Exception) {
            AppLogger.log("PhotoDelete", "❌ Ошибка удаления: ${e.message}", this)
        }
    }

    // ================== КАМЕРА ==================

    private fun checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) openCamera()
        else requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
    }

    private fun openCamera() {

        if (shouldUseCustomCamera()) {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
            return
        }

        val file = createImageFile() ?: return
        currentPhotoPath = file.absolutePath
        viewModel.setPhotoPath(file.absolutePath)

        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        takePictureLauncher.launch(intent)
    }

    private fun shouldUseCustomCamera(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return m.contains("xiaomi") || model.contains("redmi")
    }

    private fun createImageFile(): File? {
        return try {
            val file = File.createTempFile(
                "task_${System.currentTimeMillis()}",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun compressImage(path: String): String {
        return try {
            val file = File(path)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            val compressed = File(file.parent, "compressed_${file.name}")

            FileOutputStream(compressed).use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
            }

            compressed.absolutePath
        } catch (e: Exception) {
            path
        }
    }

    // ====== SYSTEM CAMERA ======

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val path = viewModel.currentPhotoPath.value ?: return@registerForActivityResult
            val file = File(path)

            var exists = file.exists()
            var size = file.length()

            // fallback
            if ((!exists || size == 0L) && result.data?.extras?.get("data") is Bitmap) {
                val bitmap = result.data!!.extras?.get("data") as Bitmap
                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
                exists = true
                size = file.length()
            }

            if (!exists || size == 0L) {
                Toast.makeText(this, "Не вдалося зберегти фото", Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            val compressed = compressImage(path)
            showPreview(compressed)
        }

    // ====== CUSTOM CAMERA ======

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val path = result.data?.getStringExtra("photoPath") ?: return@registerForActivityResult
            val compressed = compressImage(path)

            showPreview(compressed)
        }

    // ====== PREVIEW ======

    private fun showPreview(path: String) {

        val file = File(path)
        if (!file.exists()) return

        val bitmap = BitmapFactory.decodeFile(path)

        val image = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
        }

        AlertDialog.Builder(this)
            .setView(image)
            .setPositiveButton("Підтвердити") { _, _ ->

                currentPhotoPath = path
                viewModel.setPhotoPath(path)

//                lifecycleScope.launch {
//                    database.taskDao().saveTaskProgress(
//                        taskId!!,
//                        editComment.text.toString(),
//                        path
//                    )
//                }
                viewModel.confirmPhoto(editComment.text.toString())

                imageResult.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(this)
                    .load(File(path))
                    .into(imageResult)
            }
            .setNegativeButton("Скасувати") { _, _ ->
                file.delete()
                openCamera()
            }
            .setCancelable(false)
            .show()
    }
}
//class TaskDetailsActivity : AppCompatActivity() {
//
//    private lateinit var textTask: TextView
//    private lateinit var textDate: TextView
//    private lateinit var imageProblem: ImageView
//    private lateinit var editComment: EditText
//    private lateinit var textCounter: TextView
//    private lateinit var imageResult: ImageView
//    private lateinit var btnCamera: Button
//    private lateinit var btnSend: Button
//
//    private val viewModel: TaskDetailsViewModel by viewModel()
//
//    private val database by lazy { (application as App).database }
//
//    private var currentPhotoPath: String? = null
//    private var taskId: String? = null
//
//    private val api by lazy {
//        NetWorkProvider.provideApiService(
//            NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL),
//            ApiServiceData::class.java
//        )
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_task_details)
//
//        initViews()
//        setupTextCounter()
//        loadData()
//        setupListeners()
//    }
//
//    private fun initViews() {
//        textTask = findViewById(R.id.textTask)
//        textDate = findViewById(R.id.textDate)
//        imageProblem = findViewById(R.id.imageProblem)
//        editComment = findViewById(R.id.editComment)
//        textCounter = findViewById(R.id.textCounter)
//        imageResult = findViewById(R.id.imageResult)
//        btnCamera = findViewById(R.id.btnCamera)
//        btnSend = findViewById(R.id.btnSend)
//
//        // 👉 placeholder не обрезается
//        imageResult.scaleType = ImageView.ScaleType.FIT_CENTER
//    }
//
//    private fun setupTextCounter() {
//        editComment.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val text = s?.toString()
//
//                textCounter.text = "${text?.length ?: 0}/600"
//
//                lifecycleScope.launch {
//                    taskId?.let {
//                        database.taskDao().saveTaskProgress(
//                            it,
//                            text,
//                            currentPhotoPath
//                        )
//                    }
//                }
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//    }
//
//    private fun loadData() {
//        taskId = intent.getStringExtra("task_id")
//
//        if (taskId == null) {
//            Toast.makeText(this, "Помилка: task_id відсутній", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        lifecycleScope.launch {
//            val task = database.taskDao().getTaskById(taskId!!)
//
//            if (task == null) {
//                Toast.makeText(this@TaskDetailsActivity, "Завдання не знайдено", Toast.LENGTH_SHORT).show()
//                finish()
//                return@launch
//            }
//
//            textTask.text = task.taskText ?: ""
//            textDate.text = "Дата створення задачі: ${formatDate(task.createdAt)}"
//
//            // 👉 Фото проблемы
//            if (!task.imgUrl.isNullOrBlank()) {
//                Glide.with(this@TaskDetailsActivity)
//                    .load(task.imgUrl)
//                    .into(imageProblem)
//            }
//
//            // 👉 восстановление сохранённого состояния
//            editComment.setText(task.taskAnswer ?: "")
//
//            if (!task.localPhotoPath.isNullOrBlank()) {
//                currentPhotoPath = task.localPhotoPath
//                imageResult.scaleType = ImageView.ScaleType.CENTER_CROP
//
//                Glide.with(this@TaskDetailsActivity)
//                    .load(File(task.localPhotoPath))
//                    .into(imageResult)
//            }
//        }
//    }
//
//    private fun setupListeners() {
//        btnCamera.setOnClickListener {
//            checkAllPermissionsAndLaunchCamera()
//        }
//
//        btnSend.setOnClickListener {
//            sendTask()
//        }
//    }
//
//    private fun formatDate(date: String?): String {
//        return try {
//            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//            formatter.format(parser.parse(date)!!)
//        } catch (e: Exception) {
//            date ?: ""
//        }
//    }
//
//    // ================== ОТПРАВКА ==================
//
//    private fun sendTask() {
//        val comment = editComment.text.toString()
//
//        if (comment.isBlank() && currentPhotoPath == null) {
//            Toast.makeText(this, "Додайте опис або фото", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        lifecycleScope.launch {
//            try {
//                val base64Image = currentPhotoPath?.let { encodeToBase64(it) }
//
//                val request = TaskAnswerRequest(
//                    text = comment,
//                    img = base64Image
//                )
//
//                val token = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                    .getString("auth_token", "") ?: ""
//
//                val response = api.sendTaskAnswer(
//                    taskId = taskId!!,
//                    token = token,
//                    body = request
//                )
//
//                if (response.isSuccessful) {
//
//                    // 👉 очищаем локально
//                    database.taskDao().clearTaskProgress(taskId!!)
//
//                    Toast.makeText(this@TaskDetailsActivity, "Завдання надіслано", Toast.LENGTH_SHORT).show()
//                    finish()
//                } else {
//                    Toast.makeText(this@TaskDetailsActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//                }
//
//            } catch (e: Exception) {
//                Toast.makeText(this@TaskDetailsActivity, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun encodeToBase64(path: String): String {
//        val bytes = File(path).readBytes()
//        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
//    }
//
//    // ================== КАМЕРА ==================
//
//    private fun checkAllPermissionsAndLaunchCamera() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            openCamera()
//        } else {
//            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
//        }
//    }
//
////    private fun openCamera() {
////        val file = createImageFile() ?: return
////
////        val uri = FileProvider.getUriForFile(
////            this,
////            "$packageName.provider",
////            file
////        )
////
////        currentPhotoPath = file.absolutePath
////
////        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
////            putExtra(MediaStore.EXTRA_OUTPUT, uri)
////        }
////
////        takePictureLauncher.launch(intent)
////    }
//
//    private fun openCamera() {
//
//        if (shouldUseCustomCamera()) {
//            val intent = Intent(this, CameraActivity::class.java)
//            cameraLauncher.launch(intent)
//            return
//        }
//
//        val file = createImageFile() ?: return
//
//        val uri = FileProvider.getUriForFile(
//            this,
//            "$packageName.provider",
//            file
//        )
//
//        viewModel.setPhotoPath(file.absolutePath)
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, uri)
//            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//        }
//
//        takePictureLauncher.launch(intent)
//    }
//
//    private fun createImageFile(): File? {
//        return try {
//            val fileName = "task_${System.currentTimeMillis()}"
//            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            File.createTempFile(fileName, ".jpg", storageDir)
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    // 👉 СЖАТИЕ
//    private fun compressImageIfNeeded(path: String): String {
//        return try {
//            val file = File(path)
//            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//
//            val compressedFile = File(file.parent, "compressed_${file.name}")
//
//            FileOutputStream(compressedFile).use { out ->
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
//            }
//
//            AppLogger.log(
//                "PhotoCompress",
//                "📉 До=${file.length()} після=${compressedFile.length()}",
//                this
//            )
//
//            compressedFile.absolutePath
//
//        } catch (e: Exception) {
//            path
//        }
//    }
//
////    private val takePictureLauncher =
////        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
////
////            if (result.resultCode == RESULT_OK) {
////
////                var path = currentPhotoPath ?: return@registerForActivityResult
////                val file = File(path)
////
////                var exists = file.exists()
////                var size = file.length()
////
////                // 👉 Xiaomi fallback
////                if ((!exists || size == 0L) && result.data?.extras?.get("data") is Bitmap) {
////                    val bitmap = result.data!!.extras?.get("data") as Bitmap
////
////                    try {
////                        FileOutputStream(file).use {
////                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
////                        }
////                        exists = true
////                        size = file.length()
////                    } catch (e: Exception) {
////                        AppLogger.log("Camera", "❌ fallback error ${e.message}", this)
////                    }
////                }
////
////                if (exists && size > 0) {
////
////                    val compressedPath = compressImageIfNeeded(path)
////                    currentPhotoPath = compressedPath
////
////                    Glide.with(this)
////                        .load(File(compressedPath))
////                        .into(imageResult)
////
////                    // 👉 сохраняем
////                    lifecycleScope.launch {
////                        taskId?.let {
////                            database.taskDao().saveTaskProgress(
////                                it,
////                                editComment.text.toString(),
////                                compressedPath
////                            )
////                        }
////                    }
////
////                } else {
////                    Toast.makeText(this, "Не вдалося зберегти фото", Toast.LENGTH_SHORT).show()
////                }
////            }
////        }
//
//    private val takePictureLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//
//            if (result.resultCode != RESULT_OK) return@registerForActivityResult
//
//            val path = viewModel.currentPhotoPath.value ?: return@registerForActivityResult
//            val file = File(path)
//
//            var exists = file.exists()
//            var size = file.length()
//
//            // Xiaomi fallback
//            if ((!exists || size == 0L) && result.data?.extras?.get("data") is Bitmap) {
//                val bitmap = result.data?.extras?.get("data") as Bitmap
//
//                FileOutputStream(file).use {
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//                }
//
//                exists = true
//                size = file.length()
//            }
//
//            if (!exists || size == 0L) {
//                Toast.makeText(this, "Не вдалося зберегти фото", Toast.LENGTH_LONG).show()
//                return@registerForActivityResult
//            }
//
//            val compressed = compressImageIfNeeded(path)
//
//            viewModel.setPhotoPath(compressed)
//
//            showPhotoPreviewDialog(compressed)
//        }
//
//    private val cameraLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//
//            if (result.resultCode == RESULT_OK) {
//
//                val path = result.data?.getStringExtra("photoPath") ?: return@registerForActivityResult
//
//                val compressed = compressImageIfNeeded(path)
//
//                viewModel.setPhotoPath(compressed)
//
//                showPhotoPreviewDialog(compressed)
//            }
//        }
//}
//class TaskDetailsActivity : AppCompatActivity() {
//
//    private lateinit var textTask: TextView
//    private lateinit var textDate: TextView
//    private lateinit var imageProblem: ImageView
//    private lateinit var editComment: EditText
//    private lateinit var textCounter: TextView
//    private lateinit var imageResult: ImageView
//    private lateinit var btnCamera: Button
//    private lateinit var btnSend: Button
//
//    private val database by lazy { (application as App).database }
//
//    private var currentPhotoPath: String? = null
//    private var taskId: String? = null
//
//    private val api by lazy {
//        NetWorkProvider.provideApiService(
//            NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL),
//            ApiServiceData::class.java
//        )
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_task_details)
//
//
//        initViews()
//        setupTextCounter()
//        loadData()
//        setupListeners()
//    }
//
//    private fun initViews() {
//        textTask = findViewById(R.id.textTask)
//        textDate = findViewById(R.id.textDate)
//        imageProblem = findViewById(R.id.imageProblem)
//        editComment = findViewById(R.id.editComment)
//        textCounter = findViewById(R.id.textCounter)
//        imageResult = findViewById(R.id.imageResult)
//        btnCamera = findViewById(R.id.btnCamera)
//        btnSend = findViewById(R.id.btnSend)
//    }
//
//    private fun setupTextCounter() {
//        editComment.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val text = s?.toString()
//
//                lifecycleScope.launch {
//                    database.taskDao().saveTaskProgress(
//                        taskId!!,
//                        text,
//                        currentPhotoPath
//                    )
//                }
//
//                textCounter.text = "${text?.length ?: 0}/600"
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//    }
//
//    private fun loadData() {
//        taskId = intent.getStringExtra("task_id")
//
//        if (taskId == null) {
//            Toast.makeText(this, "Помилка: task_id відсутній", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        lifecycleScope.launch {
//            val task = database.taskDao().getTaskById(taskId!!)
//
//            if (task == null) {
//                Toast.makeText(this@TaskDetailsActivity, "Завдання не знайдено", Toast.LENGTH_SHORT).show()
//                finish()
//                return@launch
//            }
//
//            textTask.text = task.taskText ?: ""
//            textDate.text = "Дата створення задачі: ${formatDate(task.createdAt)}"
//
//            if (!task.imgUrl.isNullOrBlank()) {
//                Glide.with(this@TaskDetailsActivity)
//                    .load(task.imgUrl)
//                    .into(imageProblem)
//            } else {
//                AppLogger.log("TaskDetails", "⚠️ imgUrl пустий", this@TaskDetailsActivity)
//            }
//        }
//    }
//
//    private fun setupListeners() {
//        btnCamera.setOnClickListener {
//            checkAllPermissionsAndLaunchCamera()
//        }
//
//        btnSend.setOnClickListener {
//            sendTask()
//        }
//    }
//
//    private fun formatDate(date: String?): String {
//        return try {
//            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
//            formatter.format(parser.parse(date)!!)
//        } catch (e: Exception) {
//            date ?: ""
//        }
//    }
//
//    private fun sendTask() {
//        val comment = editComment.text.toString()
//
//        if (comment.isBlank() && currentPhotoPath == null) {
//            Toast.makeText(this, "Додайте опис або фото", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        lifecycleScope.launch {
//            try {
//                val base64Image = currentPhotoPath?.let { encodeToBase64(it) }
//
//                val request = TaskAnswerRequest(
//                    text = comment,
//                    img = base64Image
//                )
//
//                val token = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                    .getString("auth_token", "") ?: ""
//
//                val response = api.sendTaskAnswer(
//                    taskId = taskId!!,
//                    token = token,
//                    body = request
//                )
//
//                if (response.isSuccessful) {
//                    Toast.makeText(this@TaskDetailsActivity, "Завдання надіслано", Toast.LENGTH_SHORT).show()
//                    finish()
//                } else {
//                    Toast.makeText(this@TaskDetailsActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//                }
//
//            } catch (e: Exception) {
//                Toast.makeText(this@TaskDetailsActivity, "Помилка: ${e.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun encodeToBase64(path: String): String {
//        val bytes = File(path).readBytes()
//        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
//    }
//
//    // ===== КАМЕРА =====
//
//    private fun checkAllPermissionsAndLaunchCamera() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            openCamera()
//        } else {
//            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
//        }
//    }
//
//    private fun openCamera() {
//        val file = createImageFile() ?: return
//
//        val uri = FileProvider.getUriForFile(
//            this,
//            "$packageName.provider",
//            file
//        )
//
//        currentPhotoPath = file.absolutePath
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, uri)
//        }
//
//        takePictureLauncher.launch(intent)
//    }
//
//    private fun createImageFile(): File? {
//        return try {
//            val fileName = "task_${System.currentTimeMillis()}"
//            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//            File.createTempFile(fileName, ".jpg", storageDir)
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun compressImageIfNeeded(path: String): String {
//        return try {
//            val file = File(path)
//
//            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//
//            val compressedFile = File(
//                file.parent,
//                "compressed_${file.name}"
//            )
//
//            FileOutputStream(compressedFile).use { out ->
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // 🔥 70% — оптимально
//            }
//
//            AppLogger.log(
//                "PhotoCompress",
//                "📉 До=${file.length()} байт, після=${compressedFile.length()} байт",
//                this
//            )
//
//            compressedFile.absolutePath
//
//        } catch (e: Exception) {
//            AppLogger.log("PhotoCompress", "❌ ${e.message}", this)
//            path // fallback
//        }
//    }
//
//    private val takePictureLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (result.resultCode == RESULT_OK) {
//                currentPhotoPath?.let {
//                    Glide.with(this)
//                        .load(File(it))
//                        .into(imageResult)
//                }
//            }
//        }
//}