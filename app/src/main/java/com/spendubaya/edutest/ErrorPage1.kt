package com.spendubaya.edutest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess // Import untuk exitProcess

class ErrorPage1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error_page1)

        val btnExitApp1: Button = findViewById(R.id.btnExitApp1)
        btnExitApp1.setOnClickListener {
            // Menutup semua aktivitas dan keluar dari aplikasi
            finishAffinity() // Menutup semua activity yang terkait dengan aplikasi ini
            exitProcess(0)   // Menghentikan proses aplikasi
        }
    }
}