package com.example.check_911

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.check_911.data.utils.AppLogger
import java.io.File
import androidx.camera.core.Camera
import android.view.View

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
//    private var imageCapture: ImageCapture? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraProvider: ProcessCameraProvider
    private var flashMode = ImageCapture.FLASH_MODE_AUTO
    private lateinit var camera: Camera
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var btnFlash: ImageButton
    private lateinit var flashModesLayout: LinearLayout
    private lateinit var btnFlashAuto: ImageButton
    private lateinit var btnFlashOn: ImageButton
    private lateinit var btnFlashOff: ImageButton

    private var imageCapture: ImageCapture? = null

    private val prefs by lazy {
        getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)
    }

    private var currentFlashMode = ImageCapture.FLASH_MODE_AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)

        val btnTakePhoto: ImageButton = findViewById(R.id.btnTakePhoto)
        val btnCloseCamera: ImageButton = findViewById(R.id.btnCloseCamera)
        val btnSwitchCamera: ImageButton = findViewById(R.id.switchCameraButton)

        btnFlash = findViewById(R.id.btnFlash)
        flashModesLayout = findViewById(R.id.flashModesLayout)

        // Загружаем сохранённый режим
        currentFlashMode = prefs.getInt("flash_mode", ImageCapture.FLASH_MODE_AUTO)

        updateFlashIcon()
        applyFlashMode()

        btnFlash.setOnClickListener {
            toggleFlashOptions()
        }

        btnTakePhoto.setOnClickListener { takePhoto() }

        btnCloseCamera.setOnClickListener {
            AppLogger.log("PhotoDebug", "Користувач закрив камеру", this)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        btnSwitchCamera.setOnClickListener {
            toggleCamera()
        }

        startCamera()
    }



    private fun toggleFlashOptions() {
        if (flashModesLayout.visibility == View.GONE) {
            showOtherFlashModes()
            flashModesLayout.visibility = View.VISIBLE
            flashModesLayout.alpha = 0f
            flashModesLayout.translationY = -20f // чуть выше перед появлением
            flashModesLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        } else {
            flashModesLayout.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(200)
                .withEndAction { flashModesLayout.visibility = View.GONE }
                .start()
        }
    }


    private fun showOtherFlashModes() {
        flashModesLayout.removeAllViews()

        val allModes = listOf(
            ImageCapture.FLASH_MODE_AUTO to R.drawable.ic_flash_auto,
            ImageCapture.FLASH_MODE_ON to R.drawable.ic_flash_on,
            ImageCapture.FLASH_MODE_OFF to R.drawable.ic_flash_off
        )

        val otherModes = allModes.filter { it.first != currentFlashMode }

        otherModes.forEach { (mode, iconRes) ->
            val btn = ImageButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(96, 96).apply {
                    topMargin = 16
                }
                setBackgroundResource(R.drawable.bg_close_button)
                setImageResource(iconRes)
                setOnClickListener { selectFlashMode(mode) }
            }
            flashModesLayout.addView(btn)
        }
    }

    private fun selectFlashMode(mode: Int) {
        currentFlashMode = mode
        prefs.edit().putInt("flash_mode", mode).apply()
        applyFlashMode()
        updateFlashIcon()
        toggleFlashOptions()
    }

    private fun applyFlashMode() {
        imageCapture?.flashMode = currentFlashMode
    }

    private fun updateFlashIcon() {
        val icon = when (currentFlashMode) {
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
            ImageCapture.FLASH_MODE_OFF -> R.drawable.ic_flash_off
            else -> R.drawable.ic_flash_auto
        }
        btnFlash.setImageResource(icon)
    }

    private fun setupZoomButtons() {
        val btnZoomIn = findViewById<ImageButton>(R.id.btnZoomIn)
        val btnZoomOut = findViewById<ImageButton>(R.id.btnZoomOut)

        btnZoomIn.setOnClickListener {
//            val currentZoom = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
//            camera.cameraControl.setZoomRatio((currentZoom + 0.2f).coerceAtMost(5f))
            val currentZoom = camera.cameraInfo.zoomState.value?.linearZoom ?: 0f
            val newZoom = (currentZoom + 0.1f).coerceAtMost(1f)
            camera.cameraControl.setLinearZoom(newZoom)
        }

        btnZoomOut.setOnClickListener {
//            val currentZoom = camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
//            camera.cameraControl.setZoomRatio((currentZoom - 0.2f).coerceAtLeast(1f))
            val currentZoom = camera.cameraInfo.zoomState.value?.linearZoom ?: 0f
            val newZoom = (currentZoom - 0.1f).coerceAtLeast(0f)
            camera.cameraControl.setLinearZoom(newZoom)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

//            imageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .build()
            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this,
//                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
//                    preview,
//                    imageCapture
//                )

//                // Отвязываем все юзкейсы перед повторной привязкой
                cameraProvider.unbindAll()

                // Привязываем превью и захват к камере
                camera = cameraProvider.bindToLifecycle(
                    this,
//                    cameraSelector, // теперь он есть
                    CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    preview,
                    imageCapture
                )

//                setupFlashButtons()
                setupZoomButtons()
            } catch (exc: Exception) {
                AppLogger.log("PhotoDebug", "❌ CameraX запуск неудачный: ${exc.message}", this)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    AppLogger.log("PhotoDebug", "❌ Ошибка сохранения фото: ${exc.message}", this@CameraActivity)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    AppLogger.log("PhotoDebug", "✅ Фото сохранено: ${photoFile.absolutePath}", this@CameraActivity)

                    val intent = Intent().apply {
                        putExtra("photoPath", photoFile.absolutePath)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }
        )
    }
}

