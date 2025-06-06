package com.spendubaya.edutest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var tokenEditText: EditText
    private lateinit var loginButton: Button

    // Ganti dengan URL Web App Apps Script Anda
    private val GOOGLE_SHEETS_API_URL = "https://script.google.com/macros/s/AKfycbwPjons8YZaMxYKG42QHF-NdnLP8w1pSV9r5eTbQyfA41GO48lG4XOkHfNh66nqd2WQUw/exec"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenEditText = findViewById(R.id.tokenEditText)
        loginButton = findViewById(R.id.loginButton)

        loginButton.setOnClickListener {
            val enteredToken = tokenEditText.text.toString()
            if (enteredToken.isNotEmpty()) {
                verifyToken(enteredToken)
            } else {
                Toast.makeText(this, "Mohon masukkan token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyToken(enteredToken: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(GOOGLE_SHEETS_API_URL)
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
                        Toast.makeText(this@LoginActivity, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Tutup LoginActivity agar tidak bisa kembali dengan tombol back
                    } else {
                        Toast.makeText(this@LoginActivity, "Token tidak valid", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }
}