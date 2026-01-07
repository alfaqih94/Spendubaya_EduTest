package com.spendubaya.edutest

import android.app.Activity
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * Mengelola pembuatan Status Bar di bagian atas.
 * Layout: [Kiri: Baterai + Jam] ----- Spacer ----- [Kanan: Timer]
 */
class TopBarManager(private val activity: Activity) {

    fun setupTopBar(parentView: ViewGroup): LinearLayout {

        // 1. Container Utama TopBar (Match Parent width)
        val topBarLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dpToPx(16f), dpToPx(8f), dpToPx(16f), dpToPx(8f))
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        parentView.addView(topBarLayout)

        // Kita tidak lagi menggunakan satu statusContainer untuk semua.
        // Kita butuh container yang dikembalikan ke MainActivity agar StatusViewManager bisa mengisinya.
        // Namun, karena StatusViewManager mendesain untuk menambahkan view ke 1 container,
        // kita akan membuat StatusViewManager sedikit lebih pintar atau
        // kita membuat container "Wrapper" yang di dalamnya sudah diatur posisinya.

        // Agar kompatibel dengan kode MainActivity yang ada (yang mengharapkan 1 return container),
        // kita akan tetap me-return satu LinearLayout, TAPI di dalamnya kita manipulasi "add logic" nya
        // atau kita ubah StatusViewManager.

        // SOLUSI TERBAIK: Ubah `setupTopBar` agar mengembalikan Container Utama,
        // lalu di StatusViewManager kita ubah logikanya untuk memisah Kiri dan Kanan.
        // Tapi permintaan Anda spesifik mengubah "Letaknya".

        return topBarLayout // Kembalikan layout utama, nanti StatusViewManager akan kita ubah agar mengisi ini dengan benar.
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, activity.resources.displayMetrics).toInt()
    }
}