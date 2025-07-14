package com.spendubaya.edutest

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.os.CountDownTimer
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    private lateinit var statusContainer: LinearLayout
    private lateinit var exitButton: ImageButton

    // Variabel untuk menyimpan URL dinamis yang diterima dari HomeLogin
    private var dynamicExamURL: String? = null
    private var dynamicExitToken: String? = null // <--- Perubahan di sini: Variabel baru untuk token keluar
    private var examDurationMinutes: Int = 0 // <--- BARIS BARU: Variabel untuk durasi ujian (menit)
    private lateinit var countdownTimerText: TextView // <--- BARIS BARU: TextView untuk hitung mundur
    private var countdownTimer: CountDownTimer? = null // <--- BARIS BARU: Objek timer

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    // Layout untuk menampilkan pesan error/info saat web tidak dimuat
    private lateinit var errorLayout: LinearLayout
    private lateinit var exitErrorButton: Button // Referensi tombol keluar dari layout error XML

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi Device Policy Manager untuk fungsionalitas Kiosk Mode
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Atur FLAG_SECURE untuk mencegah tangkapan layar dan perekaman layar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        // Jaga layar tetap menyala
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Atur tata letak aktivitas utama (pastikan ini berisi FrameLayout atau root serupa)
        setContentView(R.layout.main_activity)

        // Inisialisasi WebView dan pengaturannya
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true // Aktifkan JavaScript untuk konten web
        webView.webViewClient = WebViewClient() // Tangani semua URL di dalam WebView ini

        // Dapatkan URL dinamis dari Intent
        dynamicExamURL = intent.getStringExtra("EXAM_URL")
        dynamicExitToken = intent.getStringExtra("EXIT_TOKEN") // <--- Perubahan di sini: Ambil token keluar
        examDurationMinutes = intent.getIntExtra("EXAM_DURATION_MINUTES", 0) // <--- BARIS BARU: Ambil durasi ujian dari Intent
        if (dynamicExamURL != null) {
            Log.i("MainActivity", "Menerima URL dinamis: $dynamicExamURL")
        } else {
            Log.w("MainActivity", "Tidak ada URL dinamis yang diterima. WebView mungkin kosong.")
        }
        if (dynamicExitToken != null) {
            Log.i("MainActivity", "Menerima Exit Token dinamis: $dynamicExitToken") // Log token keluar
        } else {
            Log.w("MainActivity", "Tidak ada Exit Token dinamis yang diterima.")
        }
        Log.i("MainActivity", "Menerima Durasi Ujian: $examDurationMinutes menit")

        // Dapatkan root view dari aktivitas untuk menambahkan view dinamis di atas
        val rootView = findViewById<FrameLayout>(android.R.id.content)

        // Buat tata letak bilah atas yang akan menampung baterai/waktu dan tombol keluar
        val topBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL // Atur anak-anak secara horizontal
            setBackgroundColor(Color.TRANSPARENT) // Tanpa latar belakang untuk bilah
            // Padding untuk seluruh bilah atas (meningkatkan padding atas untuk menggesernya ke bawah)
            setPadding(dpToPx(16f), dpToPx(24f), dpToPx(16f), dpToPx(8f))
            gravity = Gravity.CENTER_VERTICAL // Tengahkan konten secara vertikal di dalam bilah ini
        }

        // Tambahkan topBarLayout ke root view, posisikan di paling atas
        rootView.addView(topBarLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, // Lebar cocok dengan induk
            FrameLayout.LayoutParams.WRAP_CONTENT // Tinggi membungkus konten
        ).apply {
            gravity = Gravity.TOP // Posisikan di atas
        })

        // --- Awal Perubahan Tata Letak untuk Pemusatan ---

        // Spacer kiri untuk mendorong konten ke tengah
        val leftSpacer = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // Lebar 0
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f // Ini akan mengambil ruang yang tersedia secara merata dengan rightSpacer
            }
        }
        topBarLayout.addView(leftSpacer)

        // Inisialisasi statusContainer (untuk baterai dan waktu), akan ditambahkan ke topBarLayout
        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // Tanpa bobot di sini, akan membungkus kontennya
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        topBarLayout.addView(statusContainer) // Tambahkan statusContainer ke topBarLayout

        // Spacer kanan untuk mendorong konten ke tengah dan tombol keluar ke kanan
        val rightSpacer = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // Lebar 0
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f // Ini akan mengambil ruang yang tersedia secara merata dengan leftSpacer
            }
        }
        topBarLayout.addView(rightSpacer)

        // Inisialisasi dan tambahkan tombol keluar
        exitButton = ImageButton(this).apply {
            setImageResource(R.drawable.ikon_keluar) // Atur ikon keluar
            // Beri warna ikon merah
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN)
            setBackgroundColor(Color.TRANSPARENT) // Latar belakang transparan untuk tombol
            setOnClickListener { showExitDialog() } // Atur listener klik untuk menampilkan dialog keluar
            scaleType = ImageView.ScaleType.FIT_CENTER // Skala ikon agar pas dengan batas tombol

            val iconSize = dpToPx(48f) // Ukuran yang diperbesar untuk ikon bilah atas

            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                // Tidak diperlukan gravitasi khusus di sini karena spacer menangani pemosisian
            }
        }
        topBarLayout.addView(exitButton) // Tambahkan exitButton ke topBarLayout

        // --- Akhir Perubahan Tata Letak untuk Pemusatan ---

        // Panggil fungsi untuk menambahkan tampilan baterai dan waktu ke statusContainer
        addBatteryTimeAndCountdownDisplay()

        // --- Inisialisasi Layout Error dari XML ---
        // Inflate the error_urlload.xml layout
        errorLayout = layoutInflater.inflate(R.layout.error_urlload, rootView, false) as LinearLayout
        rootView.addView(errorLayout) // Tambahkan errorLayout ke root view
        errorLayout.visibility = View.GONE // Sembunyikan secara default

        // Dapatkan referensi tombol keluar dari layout error yang baru di-inflate
        exitErrorButton = errorLayout.findViewById(R.id.exit_error_button)
        exitErrorButton.setOnClickListener {
            stopLockTask()
            finishAffinity()// Panggil dialog keluar yang sudah ada
        }
        // --- Akhir Perubahan ---

        // Inisialisasi AudioManager untuk kontrol volume
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Coba memuat pemutar media untuk suara
        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing initial sounds: ${e.message}")
        }

        // Siapkan OnBackPressedCallback untuk penanganan navigasi kembali modern
        // Callback ini secara efektif menonaktifkan tombol kembali dengan tidak melakukan apa-apa.
        val onBackPressedCallback = object : OnBackPressedCallback(true) { // 'true' berarti diaktifkan
            override fun handleOnBackPressed() {
                // Jangan lakukan apa-apa untuk menonaktifkan tombol kembali
                playAttentionSound() // Putar suara perhatian saat tombol kembali ditekan
            }
        }
        // Tambahkan callback ke onBackPressedDispatcher aktivitas
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Periksa dan aktifkan Kiosk Mode
        checkKioskModeStatus()
        if (examDurationMinutes > 0) {
            startCountdownTimer(examDurationMinutes)
        } else {
            countdownTimerText.text = "00:00:00" // Tampilkan 0 jika tidak ada durasi
            Log.w("MainActivity", "Durasi ujian 0 atau tidak valid, timer tidak dimulai.")
        }
    }

    override fun onResume() {
        super.onResume()
        // Mulai mode Lock Task (Kiosk Mode)
        startLockTask()
        playAttentionSound() // Putar suara saat dilanjutkan
        checkKioskModeStatus() // Periksa kembali status mode kios
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intersep event tombol untuk mengontrol perilaku Kiosk Mode
        if (event.action == KeyEvent.ACTION_DOWN) {
            playAttentionSound() // Putar suara saat ada penekanan tombol

            when (event.keyCode) {
                // Blokir tombol sistem ini untuk mencegah keluar dari Kiosk Mode
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    return true // Konsumsi event, mencegah perilaku default
                }
                else -> {
                    // Izinkan perilaku default untuk tombol lain
                }
            }
        }
        return super.dispatchKeyEvent(event) // Teruskan event ke superclass untuk penanganan normal
    }

    // Menghasilkan kode keluar dinamis berdasarkan tanggal saat ini


    // Menampilkan dialog untuk keluar dari Kiosk Mode, memerlukan kode
    private fun showExitDialog() {
        val customFont2 = ResourcesCompat.getFont(this, R.font.wdxll)
        val customFont = ResourcesCompat.getFont(this, R.font.cherrybomb)
        val customTitleView = TextView(this).apply {
            text = "Konfirmasi Keluar"
            setTextColor(Color.BLACK) // Warna judul
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f) // Ukuran teks judul
            setTypeface(customFont2, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan judul
            setPadding(0, dpToPx(0f), 0, dpToPx(0f)) // Padding
        }

        // Membuat TextView untuk Pesan Kustom
        val customMessageView = TextView(this).apply {
            text = "Masukkan kode untuk keluar" // <--- BARIS BERUBAH: Pesan lebih umum
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(customFont2, Typeface.ITALIC)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dpToPx(0f), 0, dpToPx(8f))
        }

        val input = EditText(this).apply {
            hint = "Masukkan Kode"
            setTextColor(Color.BLACK)
            textSize = 30f
            setHintTextColor(Color.BLACK)
            // Gunakan TYPE_CLASS_NUMBER dan TYPE_NUMBER_VARIATION_PASSWORD untuk menyamarkan input
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTypeface(customFont2, Typeface.BOLD)
            setPadding(50, 40, 50, 40) // Padding internal
            setBackgroundResource(android.R.drawable.editbox_background) // Latar belakang kotak edit standar
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan teks di dalam EditText
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Menyesuaikan padding agar lebih simetris dan menyediakan ruang yang cukup untuk tombol
            setPadding(dpToPx(24f), dpToPx(24f), dpToPx(24f), dpToPx(24f))
            setBackgroundColor(Color.WHITE) // Latar belakang putih untuk konten dialog
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan EditText secara horizontal di dalam tata letak ini
            addView(customTitleView)
            addView(customMessageView)
            addView(input) // Tambahkan EditText ke tata letak
        }

        // Buat AlertDialog

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("KELUAR") { _, _ ->
                // <--- BARIS BERUBAH: Gunakan dynamicExitToken untuk verifikasi
                if (input.text.toString() == dynamicExitToken && !dynamicExitToken.isNullOrEmpty()) {
                    playAttentionSound()
                    stopLockTask()
                    finishAffinity()
                } else {
                    Toast.makeText(this, "Kode salah!", Toast.LENGTH_SHORT).show()
                    playAttentionSound() // <--- BARIS BARU: Putar suara jika kode salah
                }
            }
            // Tambahkan tombol negatif untuk menutup dialog
            .setNegativeButton("BATAL") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ikon_keluar) // Atur ikon dialog
            .show() // Tampilkan dialog

        // Setelah dialog ditampilkan, kita bisa mengakses tombolnya dan mengubah warnanya
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(Color.BLACK) // Ubah warna teks menjadi putih agar kontras dengan latar belakang
            setBackgroundResource(R.drawable.tombol_dialog_keluar) // Terapkan drawable untuk tombol KELUAR
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Meningkatkan ukuran teks tombol
            setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f)) // Tambahkan padding untuk estetika
            // Mengatur LayoutParams untuk lebar dan tinggi
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, // Biarkan lebar menyesuaikan konten
                LinearLayout.LayoutParams.WRAP_CONTENT // Biarkan tinggi menyesuaikan konten
            ).apply {
                setMargins(0, 0, dpToPx(8f), 0) // Margin kanan untuk pemisah antara tombol
            }
            setTypeface(customFont, Typeface.NORMAL)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(Color.BLACK) // Tetapkan warna teks abu-abu gelap
            setBackgroundResource(R.drawable.tombol_dialog_batal) // Terapkan drawable untuk tombol BATAL
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) // Meningkatkan ukuran teks tombol
            setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f)) // Tambahkan padding untuk estetika
            // Mengatur LayoutParams untuk lebar dan tinggi
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, // Biarkan lebar menyesuaikan konten
                LinearLayout.LayoutParams.WRAP_CONTENT // Biarkan tinggi menyesuaikan konten
            ).apply{
                setMargins(0, 0, dpToPx(8f), 0)
            }
            setTypeface(customFont, Typeface.NORMAL)
        }
    }

    // Memutar suara perhatian, memastikan volume maksimal
    private fun playAttentionSound() {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0) // Atur volume maksimal

                if (attentionMediaPlayer != null) {
                    if (attentionMediaPlayer!!.isPlaying) {
                        attentionMediaPlayer?.seekTo(0) // Mulai ulang jika sedang diputar
                    } else {
                        attentionMediaPlayer?.start() // Mulai putar
                    }
                } else {
                    // Buat ulang jika null (misalnya, setelah dilepaskan di onDestroy)
                    attentionMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
                    attentionMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing attention sound: ${e.message}")
        }
    }

    // Memuat URL ke dalam WebView (hanya menggunakan URL dinamis) atau menampilkan error layout
    private fun loadWebViewUrl() {
        if (dynamicExamURL != null && dynamicExamURL!!.isNotEmpty()) {
            webView.visibility = View.VISIBLE // Tampilkan WebView
            errorLayout.visibility = View.GONE // Sembunyikan layout error
            webView.loadUrl(dynamicExamURL!!)
            Log.i("MainActivity", "Loading URL: $dynamicExamURL")
        } else {
            webView.visibility = View.GONE // Sembunyikan WebView
            errorLayout.visibility = View.VISIBLE // Tampilkan layout error
            // Memuat about:blank di WebView untuk memastikan tidak ada konten lama yang terlihat
            webView.loadUrl("about:blank")
            Log.w("MainActivity", "No dynamic URL found to load. Displaying error layout.")
        }
    }

    // Memeriksa dan mengaktifkan Kiosk Mode jika kondisi terpenuhi
    private fun checkKioskModeStatus() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
            startLockTask() // Mulai mode Lock Task
            Log.i("MainActivity", "Kiosk Mode is active!")
            loadWebViewUrl() // Muat URL setelah Kiosk Mode dikonfirmasi/dimulai
        } else {
            // Jika bukan pemilik perangkat/profil, Kiosk Mode mungkin tidak aktif secara otomatis
            // Tetap coba memulai Lock Task tetapi log peringatan.
            startLockTask()
            Log.w("MainActivity", "Kiosk Mode may not be active automatically. Enable manually if needed.")
            loadWebViewUrl()
        }
    }

    // Menangani hasil pengaktifan admin perangkat
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.i("MainActivity", "Device admin enabled. Please try opening the app again.")
                finish() // Selesaikan untuk memulai ulang aplikasi dan menerapkan perubahan
            } else {
                Log.w("MainActivity", "Device admin not enabled. Kiosk Mode cannot be activated.")
            }
        }
    }

    // Melepaskan pemutar media saat aktivitas dihancurkan
    override fun onDestroy() {
        super.onDestroy()
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
        exitMediaPlayer?.release()
        exitMediaPlayer = null
        countdownTimer?.cancel() // <--- BARIS BARU: Pastikan timer dibatalkan saat aktivitas dihancurkan
        Log.d("MainActivity", "onDestroy: Countdown timer cancelled and media players released.")
    }

    // Fungsi utilitas untuk mengonversi DP ke Piksel
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

    // Menambahkan status baterai dan tampilan waktu ke statusContainer
    // <--- FUNGSI BERUBAH TOTAL: Ganti nama fungsi dan tambahkan elemen timer
    private fun addBatteryTimeAndCountdownDisplay() {
        // Tata letak untuk ikon dan teks baterai
        val batteryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(8f), 0) // Padding di kanan untuk jarak dari waktu
        }

        val batteryIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_baterai)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
                setMargins(0, 0, dpToPx(4f), 0)
            }
        }
        batteryStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f
        }

        batteryLayout.addView(batteryIcon)
        batteryLayout.addView(batteryStatus)

        // Tata letak untuk ikon dan teks waktu
        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(8f), 0) // Padding di kanan untuk jarak dari timer
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(2f), 0, 0)
            }
        }

        val timeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_jam)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
                setMargins(0, 0, dpToPx(4f), 0)
            }
        }
        timeStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f
        }

        timeLayout.addView(timeIcon)
        timeLayout.addView(timeStatus)

        // <--- BARIS BARU: Tata letak untuk ikon dan teks hitung mundur
        val countdownLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(8f), 0) // Padding di kanan untuk jarak antar elemen
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(2f), 0, 0)
            }
        }

        val timerIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_timer) // <--- BARIS BARU: Asumsi ada ikon_timer.png/xml di res/drawable
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
                setMargins(0, 0, dpToPx(4f), 0)
            }
        }
        countdownTimerText = TextView(this).apply {
            setTextColor(Color.RED) // Warna merah untuk timer
            textSize = 16f
            setTypeface(null, Typeface.BOLD) // Tebal
        }

        countdownLayout.addView(timerIcon)
        countdownLayout.addView(countdownTimerText)


        // Tambahkan tata letak baterai, waktu, dan hitung mundur ke statusContainer utama
        statusContainer.addView(batteryLayout)
        statusContainer.addView(timeLayout)
        statusContainer.addView(countdownLayout) // <--- BARIS BARU: Tambahkan layout hitung mundur

        updateBatteryAndTime() // Pembaruan awal baterai dan waktu
    }

    // Memperbarui persentase baterai dan waktu saat ini secara berkala
    // <--- FUNGSI BARU: Fungsi untuk memulai timer hitung mundur
    private fun startCountdownTimer(minutes: Int) {
        val totalMillis = minutes * 60 * 1000L // Konversi menit ke milidetik

        countdownTimer?.cancel() // Batalkan timer sebelumnya jika ada

        countdownTimer = object : CountDownTimer(totalMillis, 1000) { // Interval setiap 1 detik
            private var warningSoundPlayed = false // <--- BARIS BARU: Flag untuk memastikan suara peringatan hanya diputar sekali
            private var finalWarningSoundPlayed = false // <--- BARIS BARU: Flag untuk peringatan terakhir (misal 1 menit)

            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutesRemaining = (millisUntilFinished / (1000 * 60)) % 60 // <--- BARIS BERUBAH: Ganti nama variabel
                val secondsRemaining = (millisUntilFinished / 1000) % 60 // <--- BARIS BERUBAH: Ganti nama variabel

                // Format HH:MM:SS
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutesRemaining, secondsRemaining)
                countdownTimerText.text = timeFormatted

                // Logic untuk peringatan suara
                if (minutesRemaining <= 5 && !warningSoundPlayed && millisUntilFinished > 0) { // <--- BARIS BERUBAH: Peringatan 5 menit
                    countdownTimerText.setTextColor(Color.RED)
                    playAttentionSound()
                    warningSoundPlayed = true // <--- BARIS BARU: Set flag agar tidak berbunyi lagi
                }

                // Opsional: Peringatan lebih lanjut jika tersisa 1 menit
                if (minutesRemaining <= 1 && !finalWarningSoundPlayed && millisUntilFinished > 0) { // <--- BARIS BARU: Peringatan 1 menit
                    playAttentionSound() // Bunyikan lagi untuk peringatan 1 menit
                    finalWarningSoundPlayed = true // <--- BARIS BARU: Set flag
                }
            }

            override fun onFinish() {
                countdownTimerText.text = "00:00:00"
                Toast.makeText(this@MainActivity, "Waktu ujian habis! Aplikasi keluar otomatis.", Toast.LENGTH_LONG).show()
                playAttentionSound() // Putar suara saat waktu habis
                Handler(Looper.getMainLooper()).postDelayed({
                    stopLockTask()
                    finishAffinity() // Keluar dari aplikasi
                }, 1500) // Beri sedikit waktu untuk Toast dan suara
            }
        }.start()
        Log.d("MainActivity", "Countdown timer started for $minutes minutes.")
    }

    private fun updateBatteryAndTime() {
        // Dapatkan tingkat baterai
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryStatus.text = "$level%"

        // Dapatkan waktu saat ini
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeStatus.text = timeFormat.format(Date())

        // Jadwalkan pembaruan berikutnya setelah 1 menit (60000 ms)
        Handler(Looper.getMainLooper()).postDelayed({ updateBatteryAndTime() }, 60000)
    }
}
