package com.example.check_911.data

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager


class TestNotificationActivity : AppCompatActivity() {

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

        // Проверка и запрос разрешения на Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContentView(
            Button(this).apply {
                text = "Запустити тестове повідомлення"
                setOnClickListener { launchTestWorker() }
            }
        )
    }

    private fun launchTestWorker() {
        val request = OneTimeWorkRequestBuilder<TestNotificationWorker>().build()
        WorkManager.getInstance(this).enqueue(request)

        Log.d("WorkManagerDebug", "✅ TestNotificationWorker запущен вручную")
    }
}

