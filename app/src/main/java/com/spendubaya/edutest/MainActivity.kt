package com.spendubaya.edutest

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.widget.LinearLayout
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var attentionMediaPlayer: MediaPlayer? = null
    private var exitMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private lateinit var batteryStatus: TextView
    private lateinit var timeStatus: TextView
    // Perubahan: statusContainer akan digunakan untuk status baterai dan waktu saja
    private lateinit var statusContainer: LinearLayout
    // Tambahkan variabel untuk tombol keluar agar bisa diakses
    private lateinit var exitButton: ImageButton


    // URL WEB STATIS
    private val STATIC_WEB_URL = "https://sites.google.com/view/spendubaya-edutest"

    private var kioskModeActive = false
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main) // Pastikan layout utama adalah FrameLayout atau sejenisnya

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Inisialisasi statusContainer untuk baterai dan waktu
        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Ubah gravity menjadi CENTER_HORIZONTAL untuk menempatkan di tengah atas
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)
            // Padding sedikit dari atas dan samping agar tidak terlalu menempel
            setPadding(dpToPx(16f), dpToPx(16f), dpToPx(16f), dpToPx(16f))
        }

        // Dapatkan rootView sebagai FrameLayout untuk menambahkan view secara fleksibel
        val rootView = findViewById<FrameLayout>(android.R.id.content)

        // Tambahkan statusContainer ke rootView dengan LayoutParams yang sesuai
        rootView.addView(statusContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, // Lebar match_parent
            FrameLayout.LayoutParams.WRAP_CONTENT // Tinggi wrap_content
        ).apply {
            gravity = Gravity.TOP // Tetap di bagian atas
        })


        // Panggil fungsi untuk menambahkan tampilan baterai dan waktu
        addBatteryAndTimeDisplay()

        // Panggil fungsi untuk menambahkan tombol keluar
        addExitButton()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.attention_sound)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Failed to load initial sounds: ${e.message}")
        }
        checkKioskModeStatus()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
            playAttentionSound()
        }
        checkKioskModeStatus()
    }

    override fun onStart() {
        super.onStart()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // Nonaktifkan tombol back
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            playAttentionSound()

            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    return true
                }
                else -> {
                    // Biarkan perilaku default untuk tombol lain
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun getExitCode(): String {
        val dateFormat = SimpleDateFormat("ddMMyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showExitDialog() {
        val input = EditText(this).apply {
            hint = "Masukkan Kode"
            setTextColor(Color.RED)
            textSize = 40f
            setHintTextColor(Color.DKGRAY)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER // Hanya angka
            setTypeface(typeface, android.graphics.Typeface.BOLD) // Teks tebal
            setPadding(50, 40, 50, 40)
            setBackgroundResource(android.R.drawable.editbox_background)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 80, 40)
            setBackgroundColor(Color.WHITE)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Masukkan kode unik untuk keluar")
            .setView(layout)
            .setPositiveButton("KELUAR") { _, _ ->
                if (input.text.toString() == getExitCode()) {
                    playAttentionSound()
                    stopLockTask()
                    finishAffinity()
                } else {
                    Toast.makeText(this, "Kode salah!", Toast.LENGTH_SHORT).show()
                }
            }
            .setIcon(R.drawable.exit_icon)
            .show()
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
                    attentionMediaPlayer = MediaPlayer.create(this, R.raw.attention_sound)
                    attentionMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing attention sound: ${e.message}")
        }
    }

    private fun loadStaticUrl() {
        webView.loadUrl(STATIC_WEB_URL)
        Log.i("MainActivity", "Loading URL: $STATIC_WEB_URL")
    }

    private fun checkKioskModeStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
                startLockTask()
                kioskModeActive = true
                Log.i("MainActivity", "Kiosk Mode is active!")
                loadStaticUrl()
            } else {
                kioskModeActive = false
                startLockTask()
                Log.w("MainActivity", "Kiosk Mode might not be active automatically. Enable manually if needed.")
                loadStaticUrl()
            }
        } else {
            kioskModeActive = false
            Log.w("MainActivity", "This device does not support Kiosk Mode.")
            playAttentionSound()
            loadStaticUrl()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.i("MainActivity", "Device admin enabled. Please try opening the app again.")
                finish()
            } else {
                Log.w("MainActivity", "Device admin not enabled. Kiosk Mode cannot be activated.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
        exitMediaPlayer?.release()
        exitMediaPlayer = null
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

    private fun addBatteryAndTimeDisplay() {
        // Layout yang akan memegang ikon baterai/waktu dan teks
        val displayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Karena parent (statusContainer) sudah CENTER_HORIZONTAL, ini tidak perlu lagi gravity
            // Jika mau ada spasi antar ikon/teks, itu diatur di margin masing-masing elemen di bawah
        }

        val batteryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(8f), 0) // Padding kanan agar tidak terlalu dekat dengan waktu
        }

        val batteryIcon = ImageView(this).apply {
            setImageResource(R.drawable.battery_icon)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Ukuran ikon
                setMargins(0, 0, dpToPx(4f), 0) // Margin kanan ikon
            }
        }
        batteryStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Ukuran teks lebih kecil
        }

        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val timeIcon = ImageView(this).apply {
            setImageResource(R.drawable.clock_icon)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Ukuran ikon
                setMargins(0, 0, dpToPx(4f), 0) // Margin kanan ikon
            }
        }
        timeStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Ukuran teks lebih kecil
        }

        batteryLayout.addView(batteryIcon)
        batteryLayout.addView(batteryStatus)
        timeLayout.addView(timeIcon)
        timeLayout.addView(timeStatus)

        displayLayout.addView(batteryLayout)
        displayLayout.addView(timeLayout)

        statusContainer.addView(displayLayout) // Tambahkan displayLayout ke statusContainer
        updateBatteryAndTime()
    }

    @SuppressLint("SetTextI18n")
    private fun updateBatteryAndTime() {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryStatus.text = "$level%"

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeStatus.text = timeFormat.format(Date())

        // Update setiap 1 menit (60000 ms)
        Handler(Looper.getMainLooper()).postDelayed({ updateBatteryAndTime() }, 60000)
    }

    private fun addExitButton() {
        val rootView = findViewById<FrameLayout>(android.R.id.content)

        exitButton = ImageButton(this).apply {
            setImageResource(R.drawable.exit_icon)
            // Add this line to tint the icon
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showExitDialog() }
            scaleType = ImageView.ScaleType.FIT_CENTER

            val iconSize = dpToPx(72f)

            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dpToPx(16f), dpToPx(16f))
            }
        }
        rootView.addView(exitButton)
    }
}