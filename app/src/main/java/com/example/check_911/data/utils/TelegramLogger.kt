package com.example.check_911.data.utils

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody


object TelegramLogger {
    private const val BOT_TOKEN = "8020841397:AAG-qJhTQ8WJvDiki-7k4AHMxNIv--6GCKU"
    private const val CHAT_ID = "611501620"


fun sendFileToTelegram(
    context: Context,
    file: File,
    caption: String = "Лог-файл з додатку",
    onComplete: (() -> Unit)? = null
) {
    if (!file.exists()) {
        Log.e("TelegramLogger", "Файл не знайдено: ${file.absolutePath}")
        onComplete?.invoke()
        return
    }

    val url = "https://api.telegram.org/bot$BOT_TOKEN/sendDocument"

    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("chat_id", CHAT_ID)
        .addFormDataPart("caption", caption)
        .addFormDataPart(
            "document",
            file.name,
            file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        )
        .build()

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("TelegramLogger", "Помилка відправки файла: ${e.message}")
            onComplete?.invoke()
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            if (response.isSuccessful) {
                Log.d("TelegramLogger", "Файл ${file.name} успішно надіслано")
            } else {
                Log.e("TelegramLogger", "Помилка відповіді Telegram: ${response.code} ${response.message}")
                Log.e("TelegramLogger", "Тіло відповіді Telegram: $body")
            }
            onComplete?.invoke()
        }
    })
}
}
