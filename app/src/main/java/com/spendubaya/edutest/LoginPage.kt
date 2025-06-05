package com.spendubaya.edutest

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.bluetooth.BluetoothManager

class LoginPage : AppCompatActivity() {

    private val REQUEST_BLUETOOTH_PERMISSION = 1
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
        setContentView(R.layout.activity_login_page)

        val btnCheck: Button = findViewById(R.id.loginButton) // Mengganti ID tombol
        btnCheck.setOnClickListener {
            performChecks()
        }
    }

    private fun performChecks() {
        // 1. Periksa aplikasi floating
        if (isFloatingAppRunning()) {
            startActivity(Intent(this, ErrorPage1::class.java))
            return
        }

        // 2. Periksa Bluetooth
        if (checkBluetoothStatus()) {
            startActivity(Intent(this, ErrorPage2::class.java))
            return
        }

        // 3. Periksa koneksi internet
        if (!isNetworkConnected()) {
            startActivity(Intent(this, ErrorPage3::class.java))
            return
        }

        // Jika semua aman, lanjutkan ke MainActivity
        Toast.makeText(this, "Semua kondisi aman! Melanjutkan ke aplikasi.", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Selesai LoginPage setelah ke MainActivity
    }

    private fun isFloatingAppRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Penjelasan penting tentang API level dan QUERY_ALL_PACKAGES ada di AndroidManifest.xml
        // dan komentar di Activity1.kt sebelumnya.
        // Di Android 10 (API 29) ke atas, getRunningAppProcesses()
        // hanya mengembalikan aplikasi Anda sendiri dan beberapa proses sistem.
        // Anda tidak bisa lagi secara langsung melihat proses aplikasi lain untuk tujuan keamanan/privasi.
        // Untuk deteksi "floating apps" yang lebih reliable di API tinggi,
        // Anda perlu mempertimbangkan pendekatan yang berbeda (misalnya, memeriksa izin SYSTEM_ALERT_WINDOW
        // atau AccessibilityService jika relevan).

        // Untuk tujuan demonstrasi ini, kita akan melakukan simulasi atau
        // menggunakan metode yang mungkin tidak 100% akurat di API tinggi.
        // Metode ini lebih relevan untuk API < 29.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // For API < 29
            val runningAppProcesses = manager.runningAppProcesses
            runningAppProcesses?.forEach { processInfo ->
                if (floatingApps.contains(processInfo.processName)) {
                    return true
                }
            }
        }
        // Di API 29+, kita tidak bisa lagi mengandalkan getRunningAppProcesses() untuk ini.
        // Anda mungkin perlu implementasi lain atau menganggapnya "aman" jika Anda tidak
        // memiliki cara untuk memeriksanya secara andal.
        return false
    }

    private fun checkBluetoothStatus(): Boolean {
        // ----- PERUBAHAN DI SINI UNTUK MENGHINDARI DEPRECATED -----
        val bluetoothManager: BluetoothManager? = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
        // --------------------------------------------------------

        if (bluetoothAdapter == null) {
            // Perangkat tidak mendukung Bluetooth
            Toast.makeText(this, "Perangkat ini tidak mendukung Bluetooth.", Toast.LENGTH_LONG).show()
            return false // Anggap sebagai tidak ada masalah Bluetooth jika tidak didukung
        }

        // Meminta izin BLUETOOTH_CONNECT jika API level 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH_PERMISSION)
                return false // Akan diperiksa lagi setelah permission diberikan
            }
        } else { // Untuk API < 31, cukup BLUETOOTH
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), REQUEST_BLUETOOTH_PERMISSION)
                return false // Akan diperiksa lagi setelah permission diberikan
            }
        }

        return bluetoothAdapter.isEnabled // Cek status isEnabled setelah mendapatkan adapter
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Bluetooth diberikan, coba lagi pemeriksaan
                performChecks()
            } else {
                Toast.makeText(this, "Izin Bluetooth ditolak. Tidak dapat memeriksa status Bluetooth.", Toast.LENGTH_LONG).show()
            }
        }
    }
}