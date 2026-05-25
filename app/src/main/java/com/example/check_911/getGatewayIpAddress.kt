package com.example.check_911

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.check_911.data.utils.AppLogger
import java.net.InetAddress
import java.net.UnknownHostException


fun getGatewayIpAddress(context: Context): String? {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Проверка разрешений (начиная с Android 10 и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("IP_CHECK", "⚠️ Нет разрешения ACCESS_WIFI_STATE")
            AppLogger.log("IP_CHECK", "⚠\uFE0F Нет разрешения ACCESS_WIFI_STATE", context)
            return null
        }

        val dhcpInfo = wifiManager.dhcpInfo
        if (dhcpInfo == null) {
            Log.e("IP_CHECK", "❌ dhcpInfo = null")
            AppLogger.log("IP_CHECK", "❌ dhcpInfo = null", context)
            return null
        }

        val ipAddressInt = dhcpInfo.gateway
        if (ipAddressInt == 0) {
            Log.e("IP_CHECK", "❌ Gateway IP = 0 (возможно нет подключения к Wi-Fi)")
            AppLogger.log("IP_CHECK", "❌ Gateway IP = 0 (возможно нет подключения к Wi-Fi)", context)
            return null
        }

        val ipBytes = byteArrayOf(
            (ipAddressInt and 0xFF).toByte(),
            (ipAddressInt shr 8 and 0xFF).toByte(),
            (ipAddressInt shr 16 and 0xFF).toByte(),
            (ipAddressInt shr 24 and 0xFF).toByte()
        )

        val inetAddress = InetAddress.getByAddress(ipBytes)
        val gatewayIp = inetAddress.hostAddress
        Log.d("IP_CHECK", "✅ Gateway IP: $gatewayIp")
        AppLogger.log("IP_CHECK", "✅ Gateway IP: $gatewayIp", context)
        return gatewayIp

    } catch (e: Exception) {
        Log.e("IP_CHECK", "❗Ошибка получения IP: ${e.message}", e)
        AppLogger.log("IP_CHECK", "❗Ошибка получения IP: ${e.message}", context)
        return null
    }
}
//fun getDeviceIpAddress(context: Context): String? {
//    try {
//        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
//            Log.e("IP_CHECK", "⚠️ Нет разрешения ACCESS_WIFI_STATE")
//            return null
//        }
//
//        val wifiInfo = wifiManager.connectionInfo
//        if (wifiInfo == null) {
//            Log.e("IP_CHECK", "❌ wifiInfo = null")
//            return null
//        }
//
//        val ipAddress = wifiInfo.ipAddress
//        if (ipAddress == 0) {
//            Log.e("IP_CHECK", "❌ IP адрес устройства = 0")
//            return null
//        }
//
//        val ip = String.format(
//            "%d.%d.%d.%d",
//            ipAddress and 0xff,
//            ipAddress shr 8 and 0xff,
//            ipAddress shr 16 and 0xff,
//            ipAddress shr 24 and 0xff
//        )
//
//        Log.d("IP_CHECK", "📱 IP устройства: $ip")
//        return ip
//
//    } catch (e: Exception) {
//        Log.e("IP_CHECK", "❗Ошибка получения IP устройства: ${e.message}", e)
//        return null
//    }
//}
