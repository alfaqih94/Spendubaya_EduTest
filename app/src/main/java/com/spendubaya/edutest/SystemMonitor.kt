package com.spendubaya.edutest

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Class helper untuk menangani update Baterai dan Jam secara berkala.
 */
class SystemMonitor(
    private val context: Context,
    private val batteryTextView: TextView,
    private val timeTextView: TextView
) {
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateInfo()
            handler.postDelayed(this, 60000) // Update setiap 1 menit
        }
    }

    fun startMonitoring() {
        updateRunnable.run()
    }

    fun stopMonitoring() {
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateInfo() {
        // Update Baterai
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryTextView.text = "$level%"

        // Update Waktu
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeTextView.text = timeFormat.format(Date())
    }
}