package com.example.check_911.data.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices

object LocationChecker {

    private const val TAG = "GPS_CHECK"

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, callback: (Double?, Double?) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        fused.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    callback(location.latitude, location.longitude)
                } else {
                    callback(null, null)
                }
            }
            .addOnFailureListener {
                callback(null, null)
            }
    }


    /** Возвращает расстояние между точками в метрах */
    fun calculateDistance(
        userLat: Double,
        userLon: Double,
        ttLat: Double,
        ttLon: Double
    ): Float {

        val results = FloatArray(1)

        Location.distanceBetween(
            userLat, userLon,
            ttLat, ttLon,
            results
        )

        return results[0]
    }
}
