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
import android.text.TextWatcher
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
import com.example.check_911.data.networking.networking.models.TaskAnswerRequest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class InstructionTaskDetailsActivity : AppCompatActivity() {

    private lateinit var textTask: TextView
    private lateinit var textDate: TextView
    private lateinit var imageProblem: ImageView
    private lateinit var editComment: EditText
    private lateinit var textCounter: TextView
    private lateinit var imageResult: ImageView
    private lateinit var btnCamera: Button
    private lateinit var btnSend: Button

    private val viewModel: InstructionTaskDetailsViewModel by viewModel()
    private val database by lazy { (application as App).database }
    private val api by lazy {
        NetWorkProvider.provideApiService(
            NetWorkProvider.provideRetrofit(link = NetWorkProvider.BASE_URL),
            ApiServiceData::class.java
        )
    }

    private var taskId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction_task_details)

        taskId = intent.getStringExtra("task_id")
        if (taskId == null) {
            finish()
            return
        }
        viewModel.init(taskId!!)

        initViews()
        setupObservers()
        setupListeners()
        loadData()
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
        textCounter.text = "0/600"
    }

    private fun setupObservers() {
        editComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                textCounter.text = "${text.length}/600"
                viewModel.saveComment(text)
            }
        })
    }

    private fun setupListeners() {
        btnCamera.setOnClickListener { checkCameraPermissionAndOpen() }
        btnSend.setOnClickListener { sendTask() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val id = taskId ?: return@launch
            val task = database.instructionTaskDao().getTaskById(id) ?: return@launch
            textTask.text = task.taskText
            textDate.text = "Дата створення задачі: ${formatDate(task.createdAt)}"
            if (!task.imgUrl.isNullOrBlank()) {
                Glide.with(this@InstructionTaskDetailsActivity).load(task.imgUrl).into(imageProblem)
            }
            val saved = database.instructionTaskResultDao().getResult(id)
            if (saved != null) {
                editComment.setText(saved.comment.orEmpty())
                if (!saved.photoPath.isNullOrBlank()) {
                    Glide.with(this@InstructionTaskDetailsActivity).load(File(saved.photoPath)).into(imageResult)
                }
            }
        }
    }

    private fun formatDate(date: String?): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            formatter.format(parser.parse(date ?: "")!!)
        } catch (_: Exception) {
            date ?: ""
        }
    }

    private fun sendTask() {
        val comment = editComment.text.toString()
        val taskIdValue = taskId ?: return
        val photoPath = viewModel.currentPhotoPath.value
        if (comment.isBlank() && photoPath.isNullOrBlank()) {
            Toast.makeText(this, "Додайте опис або фото", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val token = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
                val gammaId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("selected_user_idGamma", -1L)
                val base64Image = photoPath?.let { encodeToBase64(it) }
                val response = api.sendInstructionTaskAnswer(
                    token = token,
                    taskId = taskIdValue,
                    body = TaskAnswerRequest(comment, base64Image, gammaId)
                )
                if (response.isSuccessful) {
                    database.instructionTaskResultDao().deleteByTaskId(taskIdValue)
                    database.instructionTaskDao().deleteTaskById(taskIdValue)
                    Toast.makeText(this@InstructionTaskDetailsActivity, "Завдання надіслано", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@InstructionTaskDetailsActivity, "Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InstructionTaskDetailsActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun encodeToBase64(path: String): String {
        val bytes = File(path).readBytes()
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    private fun openCamera() {
        if (shouldUseCustomCamera()) {
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
            return
        }
        val file = createImageFile() ?: return
        viewModel.setPhotoPath(file.absolutePath)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        takePictureLauncher.launch(intent)
    }

    private fun shouldUseCustomCamera(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        return m.contains("xiaomi") || model.contains("redmi")
    }

    private fun createImageFile(): File? {
        return runCatching {
            File.createTempFile(
                "instruction_task_${System.currentTimeMillis()}",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
        }.getOrNull()
    }

    private fun compressImage(path: String): String {
        return try {
            val file = File(path)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return path
            val compressed = File(file.parent, "compressed_${file.name}")
            FileOutputStream(compressed).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it) }
            compressed.absolutePath
        } catch (_: Exception) {
            path
        }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val rawPath = viewModel.currentPhotoPath.value ?: return@registerForActivityResult
            val compressed = compressImage(rawPath)
            viewModel.setPhotoPath(compressed)
            Glide.with(this).load(File(compressed)).into(imageResult)
            viewModel.confirmPhoto(editComment.text.toString())
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val rawPath = result.data?.getStringExtra("photoPath") ?: return@registerForActivityResult
            val compressed = compressImage(rawPath)
            viewModel.setPhotoPath(compressed)
            Glide.with(this).load(File(compressed)).into(imageResult)
            viewModel.confirmPhoto(editComment.text.toString())
        }
}
