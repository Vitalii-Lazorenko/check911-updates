package com.example.check_911.data

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.check_911.BuildConfig
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.text.HtmlCompat
import com.example.check_911.R
import com.example.check_911.data.utils.AppLogger


//object UpdateChecker {
//    private const val VERSION_URL = "https://github.com/Vitalii-Lazorenko/check911-updates/releases/download/global/version.json"
//    private const val FILE_NAME = "check911_latest.apk"
//
//    private var progressDialog: AlertDialog? = null
//    private var downloadId: Long = -1L
//    var apkFile: File? = null
//    private var downloadReceiver: BroadcastReceiver? = null
//
//    private var pendingDownloadUrl: String? = null // Для хранения URL при запросе разрешения
//
//    fun checkForUpdate(activity: Activity, installPermissionLauncher: ActivityResultLauncher<Intent>, showNoUpdateMessage: Boolean = false) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val client = OkHttpClient()
//                val response = client.newCall(Request.Builder().url(VERSION_URL).build()).execute()
//                if (!response.isSuccessful) return@launch
//
//                val json = response.body?.string() ?: return@launch
//                val data = JSONObject(json)
//
//                val remoteCode = data.getInt("versionCode")
//                val remoteName = data.getString("versionName")
//                val apkUrl = data.getString("apkUrl")
//                val changelog = data.optString("changelog", "Немає опису")
//
//                if (remoteCode > BuildConfig.VERSION_CODE) {
//                    withContext(Dispatchers.Main) {
//                        showUpdateDialog(activity, remoteName, changelog, apkUrl, installPermissionLauncher)
//                    }
//                } else {
//                    if (showNoUpdateMessage) {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                activity,
//                                "У вас встановлена остання версія",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("UpdateChecker", "Ошибка проверки обновления: ${e.message}", e)
//            }
//        }
//    }
//
//    private fun showUpdateDialog(
//        context: Context,
//        version: String,
//        changelog: String,
//        apkUrl: String,
//        launcher: ActivityResultLauncher<Intent>
//    ) {
//        AlertDialog.Builder(context)
//            .setTitle("Доступне оновлення v$version")
//            .setMessage("Зміни:\n$changelog\n\nБажаєте оновити?")
//            .setPositiveButton("Оновити") { _, _ -> downloadApk(context, apkUrl, launcher) }
//            .setNegativeButton("Скасувати", null)
//            .show()
//    }
//
//    private fun downloadApk(context: Context, url: String, launcher: ActivityResultLauncher<Intent>) {
//        showLoadingDialog(context)
//
//        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
//        if (file.exists()) file.delete()
//
//        apkFile = file
//
//        val request = DownloadManager.Request(Uri.parse(url)).apply {
//            setTitle("Завантаження оновлення")
//            setDescription("Очікуйте завершення")
//            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
//            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            setMimeType("application/vnd.android.package-archive")
//            setAllowedOverMetered(true)
//            setAllowedOverRoaming(true)
//        }
//
//        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        downloadId = dm.enqueue(request)
//
//        Log.d("UpdateChecker", "Завантаження запущено. ID: $downloadId, файл: ${file.absolutePath}")
//
//        // Если Receiver уже зарегистрирован - отписываем
//        if (downloadReceiver != null) {
//            try {
//                context.unregisterReceiver(downloadReceiver)
//                Log.d("UpdateChecker", "BroadcastReceiver отписан (если был зарегистрирован)")
//            } catch (e: Exception) {
//                Log.e("UpdateChecker", "Receiver уже был отписан или не зарегистрирован: ${e.message}")
//            }
//            downloadReceiver = null
//        }
//
//        // Регистрируем новый BroadcastReceiver
//        downloadReceiver = object : BroadcastReceiver() {
//            override fun onReceive(ctx: Context?, intent: Intent?) {
//                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
//                if (id == downloadId) {
//                    Log.d("UpdateChecker", "Завантаження завершено (BroadcastReceiver). ID: $id")
//                    try {
//                        ctx?.unregisterReceiver(this)
//                    } catch (e: Exception) {
//                        Log.e("UpdateChecker", "Ошибка отписки от Receiver: ${e.message}")
//                    }
//                    downloadReceiver = null
//                    handleDownloadCompletion(context, launcher)
//                }
//            }
//        }
//
//        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        } else {
//            context.registerReceiver(downloadReceiver, filter)
//        }
//        Log.d("UpdateChecker", "BroadcastReceiver зарегистрирован для ACTION_DOWNLOAD_COMPLETE")
//
//        // Запускаем Polling параллельно с BroadcastReceiver
//        monitorDownload(context, dm, launcher)
//    }
//
//    private fun monitorDownload(context: Context, dm: DownloadManager, launcher: ActivityResultLauncher<Intent>) {
//        CoroutineScope(Dispatchers.IO).launch {
//            while (true) {
//                delay(1000)
//                val query = DownloadManager.Query().setFilterById(downloadId)
//                val cursor = dm.query(query)
//
//                if (cursor != null && cursor.moveToFirst()) {
//                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
//
//                    // количество скачанных байтов и общий размер файла
//                    val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
//                    val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
//
//                    // процент загрузки
//                    val progress = if (totalBytes > 0) ((downloadedBytes * 100L) / totalBytes).toInt() else 0
//
//                    // Обновляем ProgressBar
//                    withContext(Dispatchers.Main) {
//                        updateProgress(progress)
//                    }
//
//
//                    when (status) {
//                        DownloadManager.STATUS_SUCCESSFUL -> {
//                            Log.d("UpdateChecker", "Завантаження завершено (Polling).")
//                            withContext(Dispatchers.Main) {
//                                handleDownloadCompletion(context, launcher)
//                            }
//                            cursor.close()
//                            break
//                        }
//                        DownloadManager.STATUS_FAILED -> {
//                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
//                            Log.e("UpdateChecker", "Помилка завантаження. Код: $reason")
//                            withContext(Dispatchers.Main) {
//                                hideLoadingDialog()
//                                Toast.makeText(context, "Помилка завантаження ($reason)", Toast.LENGTH_LONG).show()
//                            }
//                            cursor.close()
//                            break
//                        }
//                        else -> {
//                            Log.d("UpdateChecker", "Статус завантаження: $status")
//                        }
//                    }
//                } else {
//                    Log.e("UpdateChecker", "DownloadManager не повернув результат для ID $downloadId")
//                    break
//                }
//                cursor?.close()
//            }
//        }
//    }
//
//    private fun handleDownloadCompletion(context: Context, launcher: ActivityResultLauncher<Intent>) {
//        hideLoadingDialog()
//        if (apkFile?.exists() == true) {
//            Log.d("UpdateChecker", "APK знайдено: ${apkFile!!.absolutePath}")
//            checkInstallPermissionAndInstall(context, apkFile!!, launcher)
//        } else {
//            Log.e("UpdateChecker", "Файл APK не знайдено після завантаження!")
//            Toast.makeText(context, "Файл APK не знайдено після завантаження.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//
//    private fun checkInstallPermissionAndInstall(context: Context, file: File, launcher: ActivityResultLauncher<Intent>) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (context.packageManager.canRequestPackageInstalls()) {
//                installApk(context, file)
//            } else {
//                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
//                    .setData(Uri.parse("package:${context.packageName}"))
//                launcher.launch(intent)
//            }
//        } else {
//            installApk(context, file)
//        }
//    }
//
////    fun installApk(context: Context, file: File) {
////        if (!file.exists()) {
////            Log.e("UpdateChecker", "APK файл не знайдено для установки!")
////            return
////        }
////
////        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
////        Log.d("UpdateChecker", "URI для APK: $apkUri")
////
////        val intent = Intent(Intent.ACTION_VIEW).apply {
////            setDataAndType(apkUri, "application/vnd.android.package-archive")
////            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
////        }
////        context.startActivity(intent)
////    }
//
//    fun installApk(context: Context, file: File) {
//        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        val uri = dm.getUriForDownloadedFile(downloadId)
//
//        if (uri == null) {
//            Log.e("UpdateChecker", "Не вдалося отримати URI через DownloadManager, використовую FileProvider")
//            // Резервный путь через FileProvider (если getUriForDownloadedFile() не сработает)
//            val fallbackUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
//            Log.d("UpdateChecker", "Резервний URI через FileProvider: $fallbackUri")
//
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                setDataAndType(fallbackUri, "application/vnd.android.package-archive")
//                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
//            }
//            context.startActivity(intent)
//            return
//        }
//
//        Log.d("UpdateChecker", "URI для APK через DownloadManager: $uri")
//
//        val intent = Intent(Intent.ACTION_VIEW).apply {
//            setDataAndType(uri, "application/vnd.android.package-archive")
//            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
//        }
//        context.startActivity(intent)
//    }
//
//
//    private fun showLoadingDialog(context: Context) {
//        if (progressDialog != null) return
//        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_update, null)
//        progressDialog = AlertDialog.Builder(context)
//            .setView(view)
//            .setCancelable(false)
//            .create()
//        progressDialog?.show()
//    }
//
//    private fun hideLoadingDialog() {
//        progressDialog?.dismiss()
//        progressDialog = null
//    }
//
//    private fun updateProgress(progress: Int) {
//        progressDialog?.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
//        progressDialog?.findViewById<TextView>(R.id.textProgress)?.text = "$progress%"
//    }
//}

object UpdateChecker {
    private const val VERSION_URL = "https://github.com/Vitalii-Lazorenko/check911-updates/releases/download/global/version.json"
    private const val FILE_NAME = "check911_latest.apk"

    private var progressDialog: AlertDialog? = null
    private var downloadId: Long = -1L
    private var downloadReceiver: BroadcastReceiver? = null
    var apkFile: File? = null

    var isUpdating = false

    private var pendingDownloadUrl: String? = null // Для хранения URL при запросе разрешения

    fun checkForUpdate(activity: Activity, installPermissionLauncher: ActivityResultLauncher<Intent>, showNoUpdateMessage: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val response = client.newCall(Request.Builder().url(VERSION_URL).build()).execute()
                if (!response.isSuccessful) return@launch

                val json = response.body?.string() ?: return@launch
                val data = JSONObject(json)

                val remoteCode = data.getInt("versionCode")
                val remoteName = data.getString("versionName")
                val apkUrl = data.getString("apkUrl")
                val changelog = data.optString("changelog", "Немає опису")
//val changelog = "• Покращення стабільності<br>• Новий дизайн<br><a href='https://github.com/Vitalii-Lazorenko/Rediscount-updates/releases/download/global/Rediscount.apk'>Завантажити APK вручну</a>"

                if (remoteCode > BuildConfig.VERSION_CODE) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(activity, remoteName, changelog, apkUrl, installPermissionLauncher)
                    }
                } else if (showNoUpdateMessage) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "У вас встановлена остання версія", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Ошибка проверки обновления: ${e.message}", e)
            }
        }
    }

//    31.10.25
//    private fun showUpdateDialog(context: Context, version: String, changelog: String, apkUrl: String, launcher: ActivityResultLauncher<Intent>) {
//        AppLogger.log("UpdateChecker", "➡️ Користувачеві показано діалог оновлення v$version", context)
//        AlertDialog.Builder(context)
//            .setTitle("Доступне оновлення v$version")
//            .setMessage("Зміни:\n$changelog\n\nБажаєте оновити?")
//            .setPositiveButton("Оновити") { _, _ ->
//                AppLogger.log("UpdateChecker", "✅ Користувач погодився на оновлення, URL: $apkUrl", context)
//                startUpdateFlow(context as Activity, apkUrl, launcher)
//            }
//            .setNegativeButton("Скасувати") { _, _ ->
//                AppLogger.log("UpdateChecker", "❌ Користувач скасував оновлення", context)
//            }
//            .show()
//    }

    private fun showUpdateDialog(context: Context, version: String, changelog: String, apkUrl: String, launcher: ActivityResultLauncher<Intent>) {
        AppLogger.log("UpdateChecker", "➡️ Користувачеві показано діалог оновлення v$version", context)

        // Разрешаем HTML в changelog (например, если там есть <a href="...">)
        val message = HtmlCompat.fromHtml(
            "Зміни:<br>$changelog<br><br>Бажаєте оновити?",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        val dialog = AlertDialog.Builder(context)
            .setTitle("Доступне оновлення v$version")
            .setMessage(message)
            .setPositiveButton("Оновити") { _, _ ->
                AppLogger.log("UpdateChecker", "✅ Користувач погодився на оновлення, URL: $apkUrl", context)
                startUpdateFlow(context as Activity, apkUrl, launcher)
            }
            .setNegativeButton("Скасувати") { _, _ ->
                AppLogger.log("UpdateChecker", "❌ Користувач скасував оновлення", context)
            }
            .create()

        dialog.show()

        // Делаем ссылки кликабельными
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod =
            LinkMovementMethod.getInstance()
    }

    private fun startUpdateFlow(activity: Activity, apkUrl: String, launcher: ActivityResultLauncher<Intent>) {
        AppLogger.log("UpdateChecker", "🔍 Перевірка дозволу на встановлення APK (SDK=${Build.VERSION.SDK_INT})", activity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activity.packageManager.canRequestPackageInstalls()) {
                AppLogger.log("UpdateChecker", "✅ Дозвіл на встановлення є — запускаємо завантаження", activity)
                downloadApk(activity, apkUrl)
            } else {
                AppLogger.log("UpdateChecker", "⚠️ Немає дозволу на встановлення APK — відкриваємо налаштування", activity)
                isUpdating = true
                pendingDownloadUrl = apkUrl
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${activity.packageName}"))
                launcher.launch(intent)
            }
        } else {
            AppLogger.log("UpdateChecker", "⬇️ Старий SDK (<26) — одразу завантажуємо", activity)
            downloadApk(activity, apkUrl)
        }
    }

    fun continueUpdateIfPermissionGranted(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls()) {
            pendingDownloadUrl?.let {
                downloadApk(context, it)
                pendingDownloadUrl = null
            }
        }
    }

    //25.09.25
    private fun downloadApk(context: Context, url: String) {
        showLoadingDialog(context)

        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        if (file.exists()) {
            AppLogger.log("UpdateChecker", "🗑 Старий APK видалено: ${file.absolutePath}", context)
            file.delete()
        }
        apkFile = file

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Завантаження оновлення")
            setDescription("Очікуйте завершення")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        AppLogger.log("UpdateChecker", "⬇️ Завантаження запущено. ID=$downloadId, файл=${file.absolutePath}", context)

        Log.d("UpdateChecker", "Завантаження запущено. ID: $downloadId, файл: ${file.absolutePath}")

        // Если Receiver уже зарегистрирован - отписываем
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver)
                AppLogger.log("UpdateChecker", "ℹ️ Старий receiver відписано", context)
            } catch (e: Exception) {
                AppLogger.log("UpdateChecker", "⚠️ Receiver не вдалося відписати: ${e.message}", context)
            }
            downloadReceiver = null
        }

        // Регистрируем новый BroadcastReceiver
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                if (id == downloadId) {
                    try {
                        ctx?.unregisterReceiver(this)
                        AppLogger.log("UpdateChecker", "📩 Завантаження завершено (ID=$id)", context)
                    } catch (e: Exception) {
                        AppLogger.log("UpdateChecker", "⚠️ Не вдалося відписати receiver: ${e.message}", context)
                    }
                    downloadReceiver = null
                    handleDownloadCompletion(context)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(downloadReceiver, filter)
            }
            AppLogger.log("UpdateChecker", "✅ Receiver зареєстровано", context)
        } catch (e: Exception) {
            AppLogger.log("UpdateChecker", "❌ Помилка реєстрації receiver: ${e.message}", context)
        }

        monitorDownload(context, dm)
    }

    private fun monitorDownload(context: Context, dm: DownloadManager) {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(1000)
                val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))

                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                    val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (totalBytes > 0) ((downloadedBytes * 100L) / totalBytes).toInt() else 0

                    withContext(Dispatchers.Main) {
                        updateProgress(progress)
                    }

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            withContext(Dispatchers.Main) {
                                handleDownloadCompletion(context)
                            }
                            cursor.close()
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            withContext(Dispatchers.Main) {
                                hideLoadingDialog()
                                Toast.makeText(context, "Помилка завантаження", Toast.LENGTH_LONG).show()
                            }
                            cursor.close()
                            break
                        }
                    }
                } else break
                cursor?.close()
            }
        }
    }

    private fun handleDownloadCompletion(context: Context) {
        hideLoadingDialog()
        if (apkFile?.exists() == true) {
            installApk(context, apkFile!!)
        } else {
            Toast.makeText(context, "Файл APK не знайдено після завантаження.", Toast.LENGTH_LONG).show()
        }
    }

    fun installApk(context: Context, file: File) {
        isUpdating = false
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId) ?: FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun showLoadingDialog(context: Context) {
        if (progressDialog != null) return
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_update, null)
        progressDialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun hideLoadingDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun updateProgress(progress: Int) {
        progressDialog?.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
        progressDialog?.findViewById<TextView>(R.id.textProgress)?.text = "$progress%"
    }
}
