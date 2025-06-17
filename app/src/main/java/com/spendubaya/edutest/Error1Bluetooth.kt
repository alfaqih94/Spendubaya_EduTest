package com.spendubaya.edutest

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess // Import untuk exitProcess

class Error1Bluetooth : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.error1_bluetooth)

        val btnExit: Button = findViewById(R.id.tombol_keluar)
        btnExit.setOnClickListener {
            finishAffinity()
            exitProcess(0)
        }
    }
}