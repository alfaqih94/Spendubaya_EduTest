package com.spendubaya.edutest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.LinearLayout


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var exitMediaPlayer: MediaPlayer? = null
    private var attentionMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    private val GOOGLE_SHEET_CSV_URL = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQ-0ksNwAXFSYUtj7g4-oQZXFZf6zVffqXYHsvw9MvtMNAd7WGzNdpK4RE4DThpr4qTsuWpHFr9x4zt/pub?gid=0&single=true&output=csv"

    private var kioskModeActive = false
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName
    // private lateinit var instructionDialog: AlertDialog // Baris ini dihapus karena tidak lagi dibutuhkan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )



        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
            // PASTIKAN INI ADALAH R.raw.attention_sound, BUKAN R.raw.exit_sound
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal memuat suara awal.", Toast.LENGTH_SHORT).show()
        }

        val btnExit = findViewById<Button>(R.id.btnExit)
        btnExit.setOnClickListener {
            showExitDialog()
        }

        // showKioskModeInstructions() // Baris ini dihapus untuk menghilangkan dialog peringatan

        // Panggil checkKioskModeStatus langsung di onCreate agar langsung mencoba mengaktifkan kiosk
        // dan memuat URL jika berhasil.
        checkKioskModeStatus()
    }

    override fun onResume() {
        super.onResume()
        // startLockTask() // Tidak perlu lagi memanggil di onResume jika sudah di onCreate dan checkKioskModeStatus
        // checkKioskModeStatus() // Sudah dipanggil di onCreate, bisa dihilangkan di onResume jika Lock Task diharapkan aktif terus
        // Namun, membiarkannya di onResume bisa jadi fallback jika aplikasi sempat keluar Lock Task dan kembali
        // Keputusan ada pada Anda, untuk ujian sebaiknya tetap ada agar konsisten.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask() // Tetap panggil ini untuk memastikan mode Lock Task aktif kembali jika aplikasi di-resume dari background
        }
        checkKioskModeStatus() // Tetap panggil ini untuk re-check status dan memuat URL jika diperlukan
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // startLockTask() // Sudah dipanggil di onResume, bisa dihilangkan di onStart untuk menghindari redudansi
            // Namun, membiarkannya juga tidak masalah
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Nonaktifkan tombol back
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) { // Hapus '?' karena event dipastikan non-null
            if (currentFocus?.rootView?.tag != "exit_dialog_active") {
                playAttentionSound()
            }
        }
        return super.dispatchKeyEvent(event) // Hapus '!!' karena event dipastikan non-null
    }

    private fun showExitDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Masukkan Kode di sini"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(
                dpToPx(16f),
                dpToPx(16f),
                dpToPx(16f),
                dpToPx(16f)
            )
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setStroke(2, Color.GRAY)
                setColor(Color.parseColor("#F5F5F5"))
            }
            background = backgroundDrawable
            setTextColor(Color.BLACK)
            setHintTextColor(Color.LTGRAY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24f), dpToPx(24f), dpToPx(24f), dpToPx(16f))
            }
            layoutParams = params
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setTitle("Kode Keluar Ujian")
            .setMessage("Untuk mengakhiri sesi ujian, harap masukkan kode verifikasi yang benar.")
            .setView(input)
            .setPositiveButton("KELUAR") { _, _ ->
                if (input.text.toString() == "123") { // Ganti kode Anda di sini
                    playAttentionSound()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        stopLockTask()
                    }
                    finish()
                } else {
                    Toast.makeText(this, "Kode salah! Silakan coba lagi.", Toast.LENGTH_SHORT).show()
                    playAttentionSound()
                }
            }
            .setNegativeButton("BATAL", null)
            .create()

        dialog.window?.decorView?.rootView?.tag = "exit_dialog_active"
        dialog.setOnDismissListener {
            dialog.window?.decorView?.rootView?.tag = null
        }
        dialog.show()

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.apply {
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f))
            val buttonDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8f).toFloat()
                setColor(Color.parseColor("#4CAF50"))
            }
            background = buttonDrawable
            setAllCaps(true)
        }

        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.apply {
            setTextColor(Color.GRAY)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f))
            setAllCaps(true)
        }
    }

    private fun playAttentionSound() {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

                if (attentionMediaPlayer != null) {
                    if (attentionMediaPlayer!!.isPlaying) {
                        attentionMediaPlayer?.seekTo(0)
                    } else {
                        attentionMediaPlayer?.start()
                    }
                } else {
                    // PASTIKAN DI SINI MEMUAT R.raw.attention_sound, BUKAN R.raw.exit_sound
                    attentionMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
                    attentionMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadUrlFromGoogleSheet() {
        GlobalScope.launch(Dispatchers.IO) {
            var webUrl = "https://link-ujian-anda.com"
            var connection: HttpURLConnection? = null
            try {
                val url = URL(GOOGLE_SHEET_CSV_URL)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val firstLine = reader.readLine()
                    reader.close()

                    if (!firstLine.isNullOrEmpty()) {
                        webUrl = firstLine.trim()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Gagal memuat URL ujian. Menggunakan default.", Toast.LENGTH_LONG).show()
                }
            } finally {
                connection?.disconnect()
            }

            withContext(Dispatchers.Main) {
                webView.loadUrl(webUrl)
            }
        }
    }

    // Fungsi untuk memeriksa status Kiosk Mode
    private fun checkKioskModeStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
                // Jika aplikasi adalah Device Owner/Profile Owner, aktifkan Lock Task langsung
                startLockTask()
                kioskModeActive = true
                Toast.makeText(this, "Mode Kiosk aktif!", Toast.LENGTH_SHORT).show()
                loadUrlFromGoogleSheet()
            } else {
                // Jika tidak, Lock Task tidak dapat diaktifkan secara otomatis tanpa interaksi
                // atau setup MDM. Aplikasi akan tetap berjalan tanpa peringatan.
                kioskModeActive = false
                // Anda bisa memilih untuk tidak menampilkan Toast ini jika tidak ingin menginformasikan
                // bahwa mode kiosk tidak aktif.
                Toast.makeText(this, "Mode Kiosk mungkin tidak aktif otomatis. Aktifkan secara manual jika diperlukan.", Toast.LENGTH_LONG).show()
                loadUrlFromGoogleSheet() // Tetap muat webView
            }
        } else {
            // Untuk versi Android di bawah Lollipop, Lock Task Mode tidak tersedia
            kioskModeActive = false
            Toast.makeText(this, "Perangkat ini tidak mendukung Mode Kiosk.", Toast.LENGTH_LONG).show()
            loadUrlFromGoogleSheet() // Tetap muat webView
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Admin perangkat diaktifkan. Silakan coba buka aplikasi lagi.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Admin perangkat tidak diaktifkan. Mode Kiosk tidak dapat diaktifkan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exitMediaPlayer?.release()
        exitMediaPlayer = null
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
        // instructionDialog tidak perlu di-dismiss di onDestroy lagi karena sudah dihapus
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val REQUEST_CODE_ENABLE_ADMIN = 1
    }
}