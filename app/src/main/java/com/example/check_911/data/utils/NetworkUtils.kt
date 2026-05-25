package com.example.check_911.data.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

object NetworkUtils {

    /**
     * Проверяет, что устройство подключено именно к Wi-Fi с SSID "gamma"
     */
    @SuppressLint("MissingPermission")
    fun isWifiGammaActive(context: Context): Boolean {
        // Проверяем, выдано ли разрешение на геолокацию (без него SSID может быть <unknown ssid>)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        // Проверяем, что активный транспорт — Wi-Fi
        if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return false
        }

        // Получаем имя Wi-Fi
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo.ssid.removePrefix("\"").removeSuffix("\"")

        return ssid == "GAMMA"
    }

    /**
     * Возвращает строку с информацией о текущем типе сети:
     * "Wi-Fi (GAMMA)", "Wi-Fi (MyHome)", "Мобільна мережа (4G)", "Без підключення" и т.д.
     */
    @SuppressLint("MissingPermission")
    fun getNetworkDescription(context: Context): String {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "❌ Немає з’єднання"
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return "❌ Немає з’єднання"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    val wifiManager =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val ssid = wifiManager.connectionInfo?.ssid
                        ?.removePrefix("\"")
                        ?.removeSuffix("\"")
                        ?: "невідомий Wi-Fi"
                    "Wi-Fi ($ssid)"
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    "Мобільна мережа"
                }

                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    "Ethernet"
                }

                else -> "Невідоме з’єднання"
            }
        } catch (e: Exception) {
            "⚠️ Помилка при визначенні мережі: ${e.localizedMessage}"
        }
    }
}



