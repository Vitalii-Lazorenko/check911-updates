package com.example.check_911

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
//import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val storeLocation = LatLng(50.4501, 30.5234) // Замените на координаты вашей торговой точки
    private lateinit var button_confirm: Button

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        button_confirm = findViewById(R.id.button_confirm)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        button_confirm.setOnClickListener {
            val userLocation = mMap.myLocation
            if (userLocation != null) {
                val distance = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    storeLocation.latitude, storeLocation.longitude,
                    distance
                )
                if (distance[0] > 300) {
                    AlertDialog.Builder(this)
                        .setTitle("Відстань до торгової точки")
                        .setMessage("Відстань до торгової точки дорівнює ${distance[0].toInt()} м. Максимальна відстань 300 м.")
                        .setPositiveButton("Підтвердити") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Відстань до торгової точки")
                        .setMessage("Відстань до торгової точки дорівнює ${distance[0].toInt()} м.")
                        .setPositiveButton("Підтвердити") { dialog, _ ->
                            dialog.dismiss()
                            finish() // Закрыть MapActivity
                        }
                        .show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Добавление маркеров
        mMap.addMarker(MarkerOptions().position(storeLocation).title("Торгова точка"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(storeLocation, 15f))

        // Проверка и запрос разрешений
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            enableMyLocation()
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.setOnMyLocationChangeListener { location ->
                val userLocation = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(userLocation).title("Ваше місце розташування"))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            } else {
                // Разрешение отклонено, обработайте этот случай
            }
        }
    }
}
