package com.spendubaya.edutest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess // Import untuk exitProcess

class ErrorPage3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_page3)

        val btnExitApp3: Button = findViewById(R.id.btnExitApp3)
        btnExitApp3.setOnClickListener {
            // Menutup semua aktivitas dan keluar dari aplikasi
            finishAffinity()
            exitProcess(0)
        }
    }
}