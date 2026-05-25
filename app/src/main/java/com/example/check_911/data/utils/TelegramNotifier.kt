package com.example.check_911.data.utils

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

object TelegramNotifier {
    private const val BOT_TOKEN = "8020841397:AAG-qJhTQ8WJvDiki-7k4AHMxNIv--6GCKU"
    private const val CHAT_ID = "-1003002903848" // чат "КРО переучеты
//    private const val CHAT_ID = "611501620" // bot check911_support

    fun sendMessage(
        message: String,
        onComplete: (() -> Unit)? = null
    ) {
        val url = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"

        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("chat_id", CHAT_ID)
            .add("text", message)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("TelegramNotifier", "Помилка відправки повідомлення: ${e.message}")
                onComplete?.invoke()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("TelegramNotifier", "Повідомлення успішно надіслано")
                } else {
                    val body = response.body?.string()
                    Log.e("TelegramNotifier", "Помилка відповіді Telegram: ${response.code} ${response.message}")
                    Log.e("TelegramNotifier", "Тіло відповіді Telegram: $body")
                }
                onComplete?.invoke()
            }
        })
    }
}
