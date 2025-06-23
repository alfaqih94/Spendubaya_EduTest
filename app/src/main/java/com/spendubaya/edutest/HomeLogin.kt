package com.spendubaya.edutest

import android.content.Intent
import android.os.Bundle
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
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
import org.json.JSONObject // Import JSONObject untuk mengurai objek JSON
import java.net.URL

class HomeLogin : AppCompatActivity() {

    private lateinit var tokenEditText: EditText
    private lateinit var loginButton: Button

    // Ganti dengan URL Web App Apps Script Anda
    // Asumsi: Apps Script ini akan mengembalikan JSON Array of Objects,
    // di mana setiap objek memiliki "token" dan "url".
    private val googleSheetAPI = "https://script.google.com/macros/s/AKfycbzPE8iOQyMdyN6vFodZA54OZDgDI6QBDlAapzmjvXD5ofztUinTu7QVt1WeZmQUMXXiXw/exec"

    private var pingMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

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
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            pingMediaPlayer = MediaPlayer.create(this, R.raw.ping) // Asumsi ada ping.wav di res/raw
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("HomeLogin", "Error loading ping sound: ${e.message}")
            Toast.makeText(this, "Error loading ping sound.", Toast.LENGTH_LONG).show()
        }

        tokenEditText = findViewById(R.id.token_input)
        loginButton = findViewById(R.id.tombol_masuk_token)

        loginButton.setOnClickListener {
            val enteredToken = tokenEditText.text.toString().trim() // Menambahkan trim() untuk menghilangkan spasi
            if (enteredToken.isNotEmpty()) {
                verifyToken(enteredToken)
            } else {
                playPingSound()
                Toast.makeText(this, "Mohon masukkan token", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun verifyToken(enteredToken: String) {
        // Tampilkan loading atau disable tombol jika perlu
        loginButton.isEnabled = false
        Toast.makeText(this, "Memverifikasi token...", Toast.LENGTH_SHORT).show()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(googleSheetAPI)
                val connection = url.openConnection()
                val reader = connection.getInputStream().bufferedReader()
                val response = reader.use { it.readText() }

                val tokenUrlMap = mutableMapOf<String, String>() // Map untuk menyimpan pasangan token -> URL
                val jsonArray = JSONArray(response)

                // Iterasi melalui JSONArray dan mengisi map tokenUrlMap
                for (i in 0 until jsonArray.length()) {
                    val jsonObject: JSONObject = jsonArray.getJSONObject(i)
                    val token = jsonObject.getString("token")
                    // Jika URL kosong di sheet, ini akan menjadi string kosong, bukan null
                    val urlLink = jsonObject.optString("url", "") // Menggunakan optString dengan default ""
                    tokenUrlMap[token] = urlLink
                }

                launch(Dispatchers.Main) {
                    if (tokenUrlMap.containsKey(enteredToken)) {
                        val examUrl = tokenUrlMap[enteredToken] // Ini bisa berupa string kosong jika tidak ada URL
                        Toast.makeText(this@HomeLogin, "Login Berhasil! Memuat ujian...", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@HomeLogin, MainActivity::class.java)
                        // Mengirim URL (bisa kosong) ke MainActivity
                        intent.putExtra("EXAM_URL", examUrl)
                        startActivity(intent)
                        // finish() // KOMENTARI BARIS INI UNTUK DEBUGGING
                    } else {
                        Toast.makeText(this@HomeLogin, "Token tidak valid", Toast.LENGTH_SHORT).show()
                        playPingSound()
                    }
                    loginButton.isEnabled = true // Aktifkan kembali tombol
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    playPingSound()
                    Toast.makeText(this@HomeLogin, "Error saat memverifikasi token: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                    loginButton.isEnabled = true // Aktifkan kembali tombol
                }
            }
        }
    }
    /**
     * Memutar suara 'ping'
     */
    private fun playPingSound() {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0) // Atur volume maksimal

                if (pingMediaPlayer != null) {
                    if (pingMediaPlayer!!.isPlaying) {
                        pingMediaPlayer?.seekTo(0) // Mulai ulang jika sedang diputar
                    } else {
                        pingMediaPlayer?.start() // Mulai putar
                    }
                } else {
                    // Buat ulang jika null (misalnya, setelah dilepaskan di onDestroy)
                    pingMediaPlayer = MediaPlayer.create(this, R.raw.ping)
                    pingMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("LoginAwal", "Error playing ping sound: ${e.message}")
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Melepaskan MediaPlayer saat aktivitas dihancurkan
        pingMediaPlayer?.release()
        pingMediaPlayer = null
        Log.d("LoginAwal", "onDestroy: Ping media player released.")
    }
}
