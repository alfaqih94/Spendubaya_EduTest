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
import android.text.InputType
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var exitMediaPlayer: MediaPlayer? = null
    private var attentionMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private lateinit var batteryStatus: TextView
    private lateinit var timeStatus: TextView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var maximizeButton: ImageButton

    // URL WEB STATIS
    private val STATIC_WEB_URL = "https://www.facebook.com/"

    private var kioskModeActive = false
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    // KODE KELUAR STATIS

    // VARIABEL UNTUK MENGONTROL TOAST KIOSK (DIHAPUS, karena toast dihilangkan)
    // private var hasShownKioskModeToast = false

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
        buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dpToPx(20f), dpToPx(25f), dpToPx(20f), dpToPx(20f)) // Gunakan dpToPx
            visibility = LinearLayout.VISIBLE
        }
        val rootView = findViewById<FrameLayout>(android.R.id.content)
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = dpToPx(25f)
        }
        rootView.addView(buttonContainer, layoutParams)

        addNavigationButtons()
        addBatteryAndTimeDisplay()
        addMinimizeButton()
        addExitButton()
        addMaximizeButton()


        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.attention_sound)
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(this, "Gagal memuat suara awal.", Toast.LENGTH_SHORT).show() // DIHAPUS
            Log.e("MainActivity", "Failed to load initial sounds: ${e.message}") // Ganti dengan Log
        }

        checkKioskModeStatus()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startLockTask()
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
        return dateFormat.format(Date())}

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

        val dialog = AlertDialog.Builder(this, R.style.CustomAlertDialogTheme)
            .setTitle("Kode Keluar Ujian")
            .setMessage("Untuk mengakhiri sesi ujian, harap masukkan kode verifikasi yang benar.")
            .setView(input)
            .setPositiveButton("KELUAR") { _, _ ->
                if (input.text.toString() == getExitCode()) {
                    playAttentionSound()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        stopLockTask()
                    }
                    finish()
                } else {
                    // Toast.makeText(this, "Kode salah! Silakan coba lagi.", Toast.LENGTH_SHORT).show() // DIHAPUS
                    Log.w("MainActivity", "Incorrect exit code entered.") // Ganti dengan Log
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

    private fun playExitSound() {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

                if (exitMediaPlayer != null) {
                    if (exitMediaPlayer!!.isPlaying) {
                        exitMediaPlayer?.seekTo(0)
                    } else {
                        exitMediaPlayer?.start()
                    }
                } else {
                    exitMediaPlayer = MediaPlayer.create(this, R.raw.exit_sound)
                    exitMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing exit sound: ${e.message}") // Ganti dengan Log
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
                    attentionMediaPlayer = MediaPlayer.create(this, R.raw.attention_sound)
                    attentionMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing attention sound: ${e.message}") // Ganti dengan Log
        }
    }

    private fun loadStaticUrl() {
        webView.loadUrl(STATIC_WEB_URL)
        // Toast.makeText(this, "Memuat URL: $STATIC_WEB_URL", Toast.LENGTH_SHORT).show() // DIHAPUS
        Log.i("MainActivity", "Loading URL: $STATIC_WEB_URL") // Ganti dengan Log
    }

    // Fungsi untuk memeriksa status Kiosk Mode
    private fun checkKioskModeStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
                startLockTask()
                kioskModeActive = true
                // Toast.makeText(this, "Mode Kiosk aktif!", Toast.LENGTH_SHORT).show() // DIHAPUS
                Log.i("MainActivity", "Kiosk Mode is active!") // Ganti dengan Log
                // hasShownKioskModeToast = true // Tidak perlu lagi karena toast dihilangkan
                loadStaticUrl()
            } else {
                kioskModeActive = false
                // Toast.makeText(this, "Mode Kiosk mungkin tidak aktif otomatis. Aktifkan secara manual jika diperlukan.", Toast.LENGTH_LONG).show() // DIHAPUS
                Log.w("MainActivity", "Kiosk Mode might not be active automatically. Enable manually if needed.") // Ganti dengan Log
                playAttentionSound()
                // hasShownKioskModeToast = true // Tidak perlu lagi karena toast dihilangkan
                loadStaticUrl()
            }
        } else {
            kioskModeActive = false
            // Toast.makeText(this, "Perangkat ini tidak mendukung Mode Kiosk.", Toast.LENGTH_LONG).show() // DIHAPUS
            Log.w("MainActivity", "This device does not support Kiosk Mode.") // Ganti dengan Log
            playAttentionSound()
            // hasShownKioskModeToast = true // Tidak perlu lagi karena toast dihilangkan
            loadStaticUrl()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // Toast.makeText(this, "Admin perangkat diaktifkan. Silakan coba buka aplikasi lagi.", Toast.LENGTH_LONG).show() // DIHAPUS
                Log.i("MainActivity", "Device admin enabled. Please try opening the app again.") // Ganti dengan Log
                finish()
            } else {
                // Toast.makeText(this, "Admin perangkat tidak diaktifkan. Mode Kiosk tidak dapat diaktifkan.", Toast.LENGTH_LONG).show() // DIHAPUS
                Log.w("MainActivity", "Device admin not enabled. Kiosk Mode cannot be activated.") // Ganti dengan Log
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exitMediaPlayer?.release()
        exitMediaPlayer = null
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
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
    private fun addNavigationButtons() {
        val buttons = listOf(Pair(R.drawable.back_icon) { webView.goBack() }, Pair(R.drawable.next_icon) { webView.goForward() }, Pair(R.drawable.refresh_icon) { webView.reload() })
        val buttonSize = dpToPx(50f) // Ukuran tombol 50dp
        val buttonMargin = dpToPx(25f) // Margin 10dp
        for ((icon, action) in buttons) {
            val button = ImageButton(this).apply {
                setImageResource(icon)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { action() }
            }
            buttonContainer.addView(button)
        }
    }
    private fun addBatteryAndTimeDisplay() {
        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20,20,20,20)
        }

        val batteryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val batteryIcon = ImageView(this).apply {
            setImageResource(R.drawable.battery_icon)
        }
        batteryStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 14f
        }

        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val timeIcon = ImageView(this).apply {
            setImageResource(R.drawable.clock_icon)
        }
        timeStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 14f
        }

        batteryLayout.addView(batteryIcon)
        batteryLayout.addView(batteryStatus)
        timeLayout.addView(timeIcon)
        timeLayout.addView(timeStatus)

        statusLayout.addView(batteryLayout)
        statusLayout.addView(timeLayout)
        buttonContainer.addView(statusLayout)
        updateBatteryAndTime()
    }
    @SuppressLint("SetTextI18n")
    private fun updateBatteryAndTime() {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryStatus.text = "$level%"

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeStatus.text = timeFormat.format(Date())

        Handler(Looper.getMainLooper()).postDelayed({ updateBatteryAndTime() }, 60000)
    }

    private fun addMinimizeButton() {
        val buttonSize = dpToPx(50f) // Ukuran tombol 50dp
        val buttonMargin = dpToPx(10f) // Margin 10dp

        val minimizeButton = ImageButton(this).apply {
            setImageResource(R.drawable.minimize_icon)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                buttonContainer.visibility = LinearLayout.GONE
                maximizeButton.visibility = ImageButton.VISIBLE
            }
        }
        buttonContainer.addView(minimizeButton)
    }

    private fun addExitButton() {
        val exitButton = ImageButton(this).apply {
            setImageResource(R.drawable.exit_icon)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showExitDialog() }
        }
        buttonContainer.addView(exitButton)
    }

    private fun addMaximizeButton() {
        val buttonSize = dpToPx(50f) // Ukuran tombol 50dp
        val buttonMargin = dpToPx(10f) // Margin 10dp

        maximizeButton = ImageButton(this).apply {
            setImageResource(R.drawable.maximize_icon)
            setBackgroundColor(Color.TRANSPARENT)
            visibility = ImageButton.GONE
            setOnClickListener {
                buttonContainer.visibility = LinearLayout.VISIBLE
                buttonContainer.setBackgroundColor(Color.TRANSPARENT) // Ubah agar transparan, bukan buram
                this.visibility = ImageButton.GONE
            }

        }
        val layoutParams = FrameLayout.LayoutParams(120, 120).apply {
            gravity = Gravity.TOP or Gravity.END
            marginEnd = 40
            topMargin = 40
        }
        val rootView = findViewById<FrameLayout>(android.R.id.content)
        rootView.addView(maximizeButton, layoutParams)
    }
}