package com.example.check_911.data.utils

import android.content.Context
import android.util.Log
import com.example.check_911.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val LOG_FILE_NAME = "user_logs.txt"
    private const val MAX_LOG_LINES = 1000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Ring Buffer
    private val ringBuffer = Array<String?>(MAX_LOG_LINES) { null }
    private var writeIndex = 0
    private var isBufferFilled = false
    private var initialized = false

    /**
     * Инициализация буфера при первом вызове log()
     */
    private fun initBufferIfNeeded(context: Context) {
        if (initialized) return

        val file = getLogFile(context)
        if (file.exists()) {
            try {
                val lines = file.readLines()
                val count = minOf(lines.size, MAX_LOG_LINES)

                // последние MAX_LOG_LINES
                val recent = lines.takeLast(count)

                for (i in 0 until count) {
                    ringBuffer[i] = recent[i]
                }

                writeIndex = count % MAX_LOG_LINES
                isBufferFilled = (count == MAX_LOG_LINES)

            } catch (e: Exception) {
                Log.e("AppLogger", "Ошибка инициализации лог-файла: ${e.message}")
            }
        }

        initialized = true
    }

    /**
     * Основная функция логирования
     */
    fun log(tag: String, message: String, context: Context? = null) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] [$tag] $message"

        // вывод в Logcat
        Log.d(tag, message)

        val ctx = App.getAppContext() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                initBufferIfNeeded(ctx)

                // записываем в ring-buffer
                ringBuffer[writeIndex] = entry
                writeIndex = (writeIndex + 1) % MAX_LOG_LINES

                if (writeIndex == 0) {
                    isBufferFilled = true
                }

                // сохраняем буфер в файл
                val file = getLogFile(ctx)

                val orderedLogs = if (isBufferFilled) {
                    // полный буфер → начинаем с writeIndex
                    (0 until MAX_LOG_LINES).map { ringBuffer[(writeIndex + it) % MAX_LOG_LINES] }
                } else {
                    ringBuffer.filterNotNull()
                }

                file.writeText(orderedLogs.joinToString("\n"))

            } catch (e: Exception) {
                Log.e("AppLogger", "Ошибка при записи в лог-файл: ${e.message}")
            }
        }
    }

    /**
     * Файл логов
     */
    fun getLogFile(context: Context): File {
        val dir = context.getExternalFilesDir(null)
        return File(dir, LOG_FILE_NAME)
    }

    fun getLogFilePath(context: Context): String {
        return getLogFile(context).absolutePath
    }
}


//01.12.2025
//object AppLogger {
//    private const val LOG_FILE_NAME = "user_logs.txt"
//    private const val MAX_LOG_LINES = 1000
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//
//    fun log(tag: String, message: String, context: Context? = null) {
////fun log(tag: String, message: String) {
//        val timestamp = dateFormat.format(Date())
//        val entry = "[$timestamp] [$tag] $message"
//
//        Log.d(tag, message)
//
//        val context = App.getAppContext()
//
//        context?.let { ctx ->
//            CoroutineScope(Dispatchers.IO).launch {
//                try {
//                    val file = getLogFile(ctx)
//
//                    val logLines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
//
//                    if (logLines.size >= MAX_LOG_LINES) {
//                        val excess = logLines.size - MAX_LOG_LINES + 1
//                        repeat(excess) { logLines.removeAt(0) }
//                    }
//
//                    logLines.add(entry)
//                    file.writeText(logLines.joinToString("\n"))
//
//                } catch (e: Exception) {
//                    Log.e("AppLogger", "Ошибка при записи в лог-файл: ${e.message}")
//                }
//            }
//        }
//    }
//
//    fun getLogFile(context: Context): File {
//        val dir = context.getExternalFilesDir(null)
//        return File(dir, LOG_FILE_NAME)
//    }
//
//    fun getLogFilePath(context: Context): String {
//        return getLogFile(context).absolutePath
//    }
//}


//текущее на 03.06.2025
//object AppLogger {
//    private const val LOG_FILE_NAME = "user_logs.txt"
//    private const val MAX_LOG_LINES = 100
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//
//    fun log(tag: String, message: String, context: Context? = null) {
//        val timestamp = dateFormat.format(Date())
//        val entry = "[$timestamp] [$tag] $message"
//
//        Log.d(tag, message)
//
//        context?.let {
//            try {
//                val file = getLogFile(it)
//
//                val logLines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
//
//                if (logLines.size >= MAX_LOG_LINES) {
//                    // Удаляем самые старые строки
//                    val excess = logLines.size - MAX_LOG_LINES + 1
//                    repeat(excess) { logLines.removeAt(0) }
//                }
//
//                logLines.add(entry)
//
//                file.writeText(logLines.joinToString("\n"))
//
//            } catch (e: Exception) {
//                Log.e("AppLogger", "Ошибка при записи в лог-файл", e)
//            }
//        }
//    }
//
//    // Новый путь — во внешнем хранилище
//    fun getLogFile(context: Context): File {
//        val dir = context.getExternalFilesDir(null)
//        return File(dir, LOG_FILE_NAME)
//    }
//
//    fun getLogFilePath(context: Context): String {
//        return getLogFile(context).absolutePath
//    }
//}


//object AppLogger {
//
//    private const val LOG_FILE_NAME = "user_logs.txt"
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
//
//    fun log(tag: String, message: String, context: Context? = null) {
//        val timestamp = dateFormat.format(Date())
//        val entry = "[$timestamp] [$tag] $message\n"
//
//        // Пишем в обычный logcat для дебага
//        Log.d(tag, message)
//
//        // Пишем в файл
//        context?.let {
//            try {
//                val file = File(it.filesDir, LOG_FILE_NAME)
//                file.appendText(entry)
//            } catch (e: Exception) {
//                Log.e("AppLogger", "Ошибка при записи в лог-файл", e)
//            }
//        }
//    }
//
//    fun getLogFile(context: Context): File {
//        return File(context.filesDir, LOG_FILE_NAME)
//    }
//
//    fun clearLogs(context: Context) {
//        getLogFile(context).writeText("")
//    }
//}