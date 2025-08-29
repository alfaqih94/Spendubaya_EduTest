package com.spendubaya.edutest

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginAwal : AppCompatActivity() {

    private val requestBluetoothPermission = 1
    private var pingMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null

    private val floatingApps = listOf(
        "com.applay.overlay",
        "com.mercandalli.android.apps.bubble",
        "com.lwi.android.flapps",
        "com.fossor.panels",
        "floatbrowser.floating.browser.float.web.window",
        "com.miui.freeform"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_awal)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            pingMediaPlayer = MediaPlayer.create(this, R.raw.ping) // Asumsi ada ping.wav di res/raw
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("LoginAwal", "Error loading ping sound: ${e.message}")
            Toast.makeText(this, "Error loading ping sound.", Toast.LENGTH_LONG).show()
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val btnCheck: Button = findViewById(R.id.tombol_cek)
        btnCheck.setOnClickListener {
            lifecycleScope.launch {
                performChecks()
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

    private suspend fun performChecks() {

        // Jika tidak ada pembaruan, lanjutkan dengan pemeriksaan lainnya
        if (!isNetworkConnected()) {
            startActivity(Intent(this, Error2InternetConn::class.java))
            return
        }

        if (checkBluetoothStatus()) {
            startActivity(Intent(this, Error1Bluetooth::class.java))
            return
        }

        if (isFloatingAppRunning()) {
            startActivity(Intent(this, Error3FloatingApps::class.java))
            return
        }

        Toast.makeText(this, "Semua kondisi aman! Melanjutkan ke aplikasi.", Toast.LENGTH_SHORT)
            .show()
        startActivity(Intent(this, HomeLogin::class.java))
        finish()
    }



    override fun onDestroy() {
        super.onDestroy()
        // Melepaskan MediaPlayer saat aktivitas dihancurkan
        pingMediaPlayer?.release()
        pingMediaPlayer = null
        Log.d("LoginAwal", "onDestroy: Ping media player released.")
    }


    private fun isFloatingAppRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val runningAppProcesses = manager.runningAppProcesses
            runningAppProcesses?.forEach { processInfo ->
                if (floatingApps.contains(processInfo.processName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkBluetoothStatus(): Boolean {
        val bluetoothManager: BluetoothManager? =
            getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth.", Toast.LENGTH_LONG)
                .show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    requestBluetoothPermission
                )
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    requestBluetoothPermission
                )
                return false
            }
        }
        return bluetoothAdapter.isEnabled
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestBluetoothPermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    performChecks()
                }
            } else {
                Toast.makeText(
                    this,
                    "Izin Bluetooth ditolak. Tidak dapat memeriksa status Bluetooth.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}