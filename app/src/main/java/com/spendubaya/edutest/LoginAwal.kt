package com.spendubaya.edutest

import android.Manifest
import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.core.net.toUri // Import untuk extension function toUri()

class LoginAwal : AppCompatActivity() {

    private val requestBluetoothPermission = 1
    // URL API Google Sheet untuk pengecekan nilai update dan link download
    private val googleSheetAPI = "https://script.google.com/macros/s/AKfycbz5T0rykfPcSlVIIdVtgHPqEtXmYDim0YoUpWU4383PizOuaUc7fWKDMtWVpWkVI2F_Cw/exec" // Pastikan ini URL yang mengembalikan nilai dan download_link

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

    private suspend fun performChecks() {
        // Cek pembaruan aplikasi terlebih dahulu dan dapatkan link download
        val (updateNeeded, downloadLink) = withContext(Dispatchers.IO) {
            checkAppUpdate() // Panggil fungsi yang sekarang mengembalikan Pair
        }

        if (updateNeeded) {
            // Tampilkan dialog pembaruan dengan link yang didapat dari API
            showUpdateDialog(downloadLink)
            return
        }

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

    /**
     * Fungsi untuk memeriksa status pembaruan aplikasi dan mendapatkan link download dari API Google Sheet.
     * Mengembalikan Pair<Boolean, String?>:
     * - Boolean: true jika pembaruan diperlukan (nilai = 1), false jika tidak.
     * - String?: Link download, null jika tidak ada update atau terjadi error.
     */
    private suspend fun checkAppUpdate(): Pair<Boolean, String?> {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        return try {
            val url = URL(googleSheetAPI)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()

            val stream = connection.inputStream
            reader = BufferedReader(InputStreamReader(stream))
            val buffer = StringBuffer()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line + "\n")
            }

            val jsonResponse = buffer.toString()
            val jsonObject = JSONObject(jsonResponse)
            val nilai = jsonObject.getInt("nilai")
            // Ambil link download dari JSON, defaultnya null jika tidak ada atau error
            val downloadLink = jsonObject.optString("download_link", null)

            when (nilai) {
                0 -> Pair(false, null) // Tidak ada pembaruan
                1 -> Pair(true, downloadLink) // Ada pembaruan, sertakan link
                else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginAwal, "Respons API tidak valid", Toast.LENGTH_SHORT).show()
                    }
                    Pair(false, null) // Anggap tidak ada pembaruan jika respons tidak sesuai
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginAwal, "Error koneksi ke server: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
            Pair(false, null) // Anggap tidak ada pembaruan jika terjadi error
        } finally {
            connection?.disconnect()
            try {
                reader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Menampilkan dialog untuk memberitahu pengguna bahwa pembaruan tersedia.
     * Menerima link download sebagai parameter.
     */
    private fun showUpdateDialog(downloadLink: String?) {
        AlertDialog.Builder(this)
            .setTitle("Pembaruan Aplikasi Tersedia")
            .setMessage("Versi terbaru aplikasi tersedia. Harap perbarui untuk melanjutkan.")
            .setPositiveButton("OK") { dialog, _ ->
                if (!downloadLink.isNullOrEmpty()) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, downloadLink.toUri())
                    startActivity(browserIntent)
                } else {
                    Toast.makeText(this, "Link download tidak tersedia. Harap hubungi admin.", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
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