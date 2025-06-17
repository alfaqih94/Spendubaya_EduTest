package com.spendubaya.edutest

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

class HomeLogin : AppCompatActivity() {

    private lateinit var tokenEditText: EditText
    private lateinit var loginButton: Button

    // Ganti dengan URL Web App Apps Script Anda
    private val googleSheetAPI = "https://script.google.com/macros/s/AKfycbwPjons8YZaMxYKG42QHF-NdnLP8w1pSV9r5eTbQyfA41GO48lG4XOkHfNh66nqd2WQUw/exec"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_login)

        // Mencegah layar off dan screenshoot
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        tokenEditText = findViewById(R.id.token_input)
        loginButton = findViewById(R.id.tombol_masuk_token)

        loginButton.setOnClickListener {
            val enteredToken = tokenEditText.text.toString()
            if (enteredToken.isNotEmpty()) {
                verifyToken(enteredToken)
            } else {
                Toast.makeText(this, "Mohon masukkan token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun verifyToken(enteredToken: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(googleSheetAPI)
                val connection = url.openConnection()
                val reader = connection.getInputStream().bufferedReader()
                val response = reader.use { it.readText() }

                val jsonArray = JSONArray(response)
                val validTokens = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    validTokens.add(jsonArray.getString(i))
                }

                launch(Dispatchers.Main) {
                    if (validTokens.contains(enteredToken)) {
                        Toast.makeText(this@HomeLogin, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@HomeLogin, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Tutup LoginActivity agar tidak bisa kembali dengan tombol back
                    } else {
                        Toast.makeText(this@HomeLogin, "Token tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@HomeLogin, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
}