package com.spendubaya.edutest

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.InputType // Import for InputType
import androidx.core.content.res.ResourcesCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var attentionMediaPlayer: MediaPlayer? = null
    private var exitMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private lateinit var batteryStatus: TextView
    private lateinit var timeStatus: TextView
    private lateinit var statusContainer: LinearLayout // Container for battery and time
    private lateinit var exitButton: ImageButton // Exit button instance

    // STATIC WEB URL
    private val staticWebURL = "https://sites.google.com/view/spendubaya-edutest"

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

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
        addBatteryAndTimeDisplay()

        // Inisialisasi AudioManager untuk kontrol volume
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Coba memuat pemutar media untuk suara
        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Gagal memuat suara awal: ${e.message}")
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
    private fun getExitCode(): String {
        val dateFormat = SimpleDateFormat("ddMMyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

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
            text = "Masukkan kode (DDMMYY) untuk keluar"
            setTextColor(Color.BLACK) // Warna pesan
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) // Ukuran teks pesan
            setTypeface(customFont2, Typeface.ITALIC) // Font dan gaya untuk pesan
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan pesan
            setPadding(0, dpToPx(0f), 0, dpToPx(8f)) // Padding
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
            .setView(layout) // Atur tampilan kustom untuk dialog
            .setPositiveButton("KELUAR") { _, _ ->
                if (input.text.toString() == getExitCode()) {
                    playAttentionSound() // Putar suara saat percobaan keluar berhasil
                    stopLockTask() // Keluar dari mode Lock Task
                    finishAffinity() // Selesaikan semua aktivitas dalam tugas
                } else {
                    Toast.makeText(this, "Kode salah!", Toast.LENGTH_SHORT).show() // Tampilkan toast kesalahan
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
            Log.e("MainActivity", "Kesalahan saat memutar suara perhatian: ${e.message}")
        }
    }

    // Memuat URL statis ke dalam WebView
    private fun loadStaticUrl() {
        webView.loadUrl(staticWebURL)
        Log.i("MainActivity", "Memuat URL: $staticWebURL")
    }

    // Memeriksa dan mengaktifkan Kiosk Mode jika kondisi terpenuhi
    private fun checkKioskModeStatus() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
            startLockTask() // Mulai mode Lock Task
            Log.i("MainActivity", "Kiosk Mode aktif!")
            loadStaticUrl() // Muat URL setelah Kiosk Mode dikonfirmasi/dimulai
        } else {
            // Jika bukan pemilik perangkat/profil, Kiosk Mode mungkin tidak aktif secara otomatis
            // Tetap coba memulai Lock Task tetapi log peringatan.
            startLockTask()
            Log.w("MainActivity", "Kiosk Mode mungkin tidak aktif secara otomatis. Aktifkan secara manual jika diperlukan.")
            loadStaticUrl()
        }
    }

    // Menangani hasil pengaktifan admin perangkat
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.i("MainActivity", "Admin perangkat diaktifkan. Silakan coba buka aplikasi lagi.")
                finish() // Selesaikan untuk memulai ulang aplikasi dan menerapkan perubahan
            } else {
                Log.w("MainActivity", "Admin perangkat tidak diaktifkan. Kiosk Mode tidak dapat diaktifkan.")
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
    private fun addBatteryAndTimeDisplay() {
        // Tata letak untuk ikon dan teks baterai
        val batteryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL // Tengahkan item secara vertikal di dalam tata letak ini
            setPadding(0, 0, dpToPx(8f), 0) // Padding di kanan untuk jarak dari waktu
        }

        val batteryIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_baterai)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Ukuran ikon
                setMargins(0, 0, dpToPx(4f), 0) // Margin kanan untuk ikon
            }
        }
        batteryStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Ukuran teks untuk status baterai
        }

        batteryLayout.addView(batteryIcon)
        batteryLayout.addView(batteryStatus)

        // Tata letak untuk ikon dan teks waktu, dengan penyesuaian vertikal
        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL // Tengahkan item secara vertikal di dalam tata letak ini
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Tambahkan margin atas kecil untuk mendorong waktu sedikit ke bawah sesuai permintaan
                setMargins(0, dpToPx(2f), 0, 0)
            }
        }

        val timeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_jam)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Ukuran ikon
                setMargins(0, 0, dpToPx(4f), 0) // Margin kanan untuk ikon
            }
        }
        timeStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Ukuran teks untuk status waktu
        }

        timeLayout.addView(timeIcon)
        timeLayout.addView(timeStatus)

        // Tambahkan tata letak baterai dan waktu ke statusContainer utama
        statusContainer.addView(batteryLayout)
        statusContainer.addView(timeLayout)

        updateBatteryAndTime() // Pembaruan awal baterai dan waktu
    }

    // Memperbarui persentase baterai dan waktu saat ini secara berkala
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
