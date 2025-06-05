package com.spendubaya.edutest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess // Import untuk exitProcess

class ErrorPage2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_page2)

        val btnExitApp2: Button = findViewById(R.id.btnExitApp2)
        btnExitApp2.setOnClickListener {
            // Menutup semua aktivitas dan keluar dari aplikasi
            finishAffinity()
            exitProcess(0)
        }
    }
}