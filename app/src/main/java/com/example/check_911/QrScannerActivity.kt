package com.example.check_911

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.check_911.data.db.repository.SurveyRepository
import com.example.check_911.data.utils.AppLogger
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.room.Room
import com.example.check_911.data.db.MainDb
import android.os.Handler
import com.example.check_911.ui.theme.OverlayView


class QrScannerActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var qrFrame: View
    private var cameraProvider: ProcessCameraProvider? = null
    private var isFrameGreen = false
    private lateinit var overlayView: OverlayView
    private var isProcessing = false

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    private lateinit var loadingContainer: View
    private lateinit var progressBar: ProgressBar
    private lateinit var successMessageTextView: TextView
    private lateinit var successImageView: ImageView
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        previewView = findViewById(R.id.previewView)
//        qrFrame = findViewById(R.id.qrFrame)
        overlayView = findViewById(R.id.overlayView)

        initLoadingViews()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }


    private fun initLoadingViews() {
        loadingContainer = findViewById(R.id.loadingContainer)
        progressBar = findViewById(R.id.progressBar)
        successMessageTextView = findViewById(R.id.successMessageTextView)
        successImageView = findViewById(R.id.successImageView)
        retryButton = findViewById(R.id.retryButton)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImageProxy(imageProxy)
            }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e("QrScanner", "Camera binding failed", e)
                AppLogger.log("QrAnalyzer", "❌ Camera binding failed: ${e}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            handleQrCode(qrValue)
                            imageProxy.close()
                            return@addOnSuccessListener
                        }
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

//    private fun handleQrCode(qrValue: String) {
//
//
//
//        runOnUiThread {
////            if (!isFrameGreen) {
////                isFrameGreen = true
////                val drawable = qrFrame.background.mutate()
////                (drawable as? GradientDrawable)?.setStroke(6, Color.GREEN)
////                qrFrame.background = drawable
////
////                qrFrame.animate().setDuration(600).alpha(1f).withEndAction {
////                    Handler(Looper.getMainLooper()).postDelayed({
////                        (qrFrame.background as? GradientDrawable)?.setStroke(4, Color.WHITE)
////                        isFrameGreen = false
////                    }, 1500)
////                }.start()
////            }
//            overlayView.highlightSuccess()
//        }
//
//        Log.d("QrScanner", "QR знайдено: $qrValue")
////        Toast.makeText(this@QrScannerActivity, "QR знайдено: $qrValue", Toast.LENGTH_SHORT).show()
//
//        // Тут вызываешь репозиторий и догружаешь опросники
//        showLoading()
//
//        lifecycleScope.launch {
//            try {
//                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                val token = sharedPreferences.getString("auth_token", null)
//
//                if (token.isNullOrEmpty()) {
//                    Toast.makeText(this@QrScannerActivity, "❌ Немає токена", Toast.LENGTH_SHORT).show()
//                    return@launch
//                }
//
//                val retrofit = NetWorkProvider.provideRetrofit(
//                    link = "${NetWorkProvider.BASE_URL}/"
//                )
//                val api = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//
//                val db = Room.databaseBuilder(
//                    applicationContext,
//                    MainDb::class.java,
//                    "survey_db"   // название твоей БД
//                ).build()
//
//
//                val response = api.getSurveysByHeaderId("$token", qrValue.trim())
//                if (response.isSuccessful) {
//                    val surveys = response.body().orEmpty()
//                    if (surveys.isNotEmpty()) {
//                        val repo = SurveyRepository(api, db.surveyDao())
//                        repo.addSurveysToDatabase(surveys) // добавление к сучествующим
////                        repo.saveSurveyToDatabase(surveys) //перезапись (удаленгие все опросников и добавление только qr)
//
//                        val titles = surveys.joinToString(", ") { it.title }
////                        Toast.makeText(this@QrScannerActivity, "✅ Додано: $titles", Toast.LENGTH_LONG).show()
//                        AppLogger.log("QrScanner", "✅ Додано ${surveys.size} (${titles})", this@QrScannerActivity)
////                        finish()
//                        showSuccess("✅ Додано: ${surveys.joinToString { it.title }}")
//
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            startActivity(Intent(this@QrScannerActivity, StoreActivity::class.java))
//                            finish()
//                        }, 1500)
//                    } else {
////                        Toast.makeText(this@QrScannerActivity, "⚠️ Опитування відсутнє", Toast.LENGTH_SHORT).show()
//                        showError("⚠️ Опитування відсутні")
//                    }
//                } else {
////                    Toast.makeText(this@QrScannerActivity, "❌ Помилка: ${response.code()}", Toast.LENGTH_LONG).show()
//                    showError("❌ Помилка сервера: ${response.code()}")
//                }
//            } catch (e: Exception) {
////                Toast.makeText(this@QrScannerActivity, "❌ ${e.message}", Toast.LENGTH_LONG).show()
//                showError("⚠️ Помилка: ${e.message}")
//            }
//        }
//    }

//    private fun handleQrCode(qrValue: String) {
//        if (isProcessing) return  // 🚫 Уже обрабатываем — не запускаем повторно
//        isProcessing = true
//
//        runOnUiThread {
//            overlayView.highlightSuccess()
//        }
//
//        Log.d("QrScanner", "QR знайдено: $qrValue")
//        showLoading()
//
//        lifecycleScope.launch {
//            try {
//                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//                val token = sharedPreferences.getString("auth_token", null)
//
//                if (token.isNullOrEmpty()) {
//                    showError("❌ Немає токена")
//                    isProcessing = false
//                    return@launch
//                }
//
//                val retrofit = NetWorkProvider.provideRetrofit( link = "${NetWorkProvider.BASE_URL}/" )
//                val api = NetWorkProvider.provideApiService(retrofit, ApiServiceData::class.java)
//                val db = Room.databaseBuilder(applicationContext, MainDb::class.java, "survey_db").build()
//
//                val response = api.getSurveysByHeaderId("$token", qrValue.trim())
//                if (response.isSuccessful) {
//                    val surveys = response.body().orEmpty()
//                    if (surveys.isNotEmpty()) {
//                        val repo = SurveyRepository(api, db.surveyDao())
//                        repo.addSurveysToDatabase(surveys)
//
//                        val titles = surveys.joinToString(", ") { it.title }
//                        AppLogger.log("QrScanner", "✅ Додано ${surveys.size} (${titles})", this@QrScannerActivity)
//                        showSuccess("✅ Додано: ${titles}")
//
//                        // 🧩 Закрываем камеру и возвращаемся через паузу
//                        cameraProvider?.unbindAll()
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            val intent = Intent(this@QrScannerActivity, StoreActivity::class.java)
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                            startActivity(intent)
//                            isProcessing = false
//                            finish()
//                        }, 1500)
//                    } else {
//                        showError("⚠️ Опитування відсутні")
//                        isProcessing = false
//                    }
//                } else {
//                    showError("❌ Помилка сервера: ${response.code()}")
//                    isProcessing = false
//                }
//            } catch (e: Exception) {
//                showError("⚠️ Помилка: ${e.message}")
//                isProcessing = false
//            }
//        }
//    }

    private fun handleQrCode(qrValue: String) {
        if (isProcessing) return
        isProcessing = true

        runOnUiThread { overlayView.highlightSuccess() }

        Log.d("QrScanner", "QR знайдено: $qrValue")
        Toast.makeText(this, "QR знайдено: $qrValue", Toast.LENGTH_LONG).show()
        AppLogger.log("QrScanner", "QR знайдено: $qrValue")

        // 🧩 Переход в LoadingQRActivity
        val intent = Intent(this, LoadingQRActivity::class.java)
        intent.putExtra("headerId", qrValue)
        startActivity(intent)

        // Закрываем сканер
        finish()
    }


    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Камера не дозволена", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private fun showLoading() {
        loadingContainer.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        successMessageTextView.visibility = View.GONE
        successImageView.visibility = View.GONE
        retryButton.visibility = View.GONE
    }

    private fun showSuccess(message: String) {
        loadingContainer.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        successImageView.visibility = View.VISIBLE
        successMessageTextView.text = message
        successMessageTextView.visibility = View.VISIBLE
        retryButton.visibility = View.GONE
    }

    private fun showError(message: String) {
        loadingContainer.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        successImageView.visibility = View.GONE
        successMessageTextView.text = message
        successMessageTextView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE

        retryButton.setOnClickListener {
            restartCamera() // 📷 снова открыть камеру
        }
    }

    private fun restartCamera() {
        // показываем превью камеры
        previewView.visibility = View.VISIBLE
        loadingContainer.visibility = View.GONE

        // перезапускаем сканер
        startCamera()
    }


}

