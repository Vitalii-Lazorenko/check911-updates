package com.example.check_911

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri

class PromoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promo)

        findViewById<ImageView>(R.id.promoIcon).setOnClickListener {
//            val intent = Intent(Intent.ACTION_VIEW).apply {
//                data = Uri.parse("https://help.apteka911.com.ua/files/?type=apk&name=ekka-mini-debug")
//                setPackage("com.android.vending")
//            }
//            try {
//                startActivity(intent)
//            } catch (e: ActivityNotFoundException) {
//                Toast.makeText(this, "Не вдалося відкрити осилання", Toast.LENGTH_SHORT).show()
//            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://help.apteka911.com.ua/files/?type=apk&name=ekka-mini-debug")
            }
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivity(Intent(this, SelectionActivity::class.java))
            finish()
        }
    }
}

