package com.spendubaya.edutest

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Class Manager untuk menangani Navigasi Bawah (Back, Next, Refresh, Exit).
 * Menggunakan Ikon tanpa Teks.
 */
class WebViewNavigationManager(
    private val context: Context,
    private val webView: WebView
) {

    /**
     * Membuat View Navigation Bar.
     * @param onExitClick Callback untuk tombol keluar.
     */
    fun createNavigationBar(onExitClick: () -> Unit): View {
        // Container Navigasi
        val navLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#F5F5F5")) // Abu-abu sangat muda
            gravity = Gravity.CENTER
            // PERBAIKAN: Padding vertikal dikurangi (4f) agar background tidak terlalu tinggi
            setPadding(dpToPx(16f), dpToPx(4f), dpToPx(16f), dpToPx(4f))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // --- Tombol Navigasi (Icon Only) ---
        // Urutan: Back - Next - Refresh - Exit

        // Back: Tanpa tint (gunakan warna asli XML)
        val btnBack = createNavIconButton(R.drawable.ikon_back).apply {
            setOnClickListener {
                if (webView.canGoBack()) webView.goBack()
                else Toast.makeText(context, "Halaman awal", Toast.LENGTH_SHORT).show()
            }
        }

        // Next: Tanpa tint (gunakan warna asli XML)
        val btnNext = createNavIconButton(R.drawable.ikon_next).apply {
            setOnClickListener {
                if (webView.canGoForward()) webView.goForward()
                else Toast.makeText(context, "Halaman akhir", Toast.LENGTH_SHORT).show()
            }
        }

        // Refresh: Tanpa tint (gunakan warna asli XML)
        val btnRefresh = createNavIconButton(R.drawable.ikon_refresh).apply {
            setOnClickListener {
                webView.reload()
                Toast.makeText(context, "Menyegarkan...", Toast.LENGTH_SHORT).show()
            }
        }

        // Exit: Kita beri opsi applyRedTint=true jika ikon aslinya hitam/netral.
        // Jika ikon_keluar Anda sudah merah di XML, set false juga tidak masalah.
        // Di sini saya set true untuk memastikan tombol keluar tetap merah jika pakai aset default.
        val btnExit = createNavIconButton(R.drawable.ikon_keluar, applyRedTint = true).apply {
            setOnClickListener { onExitClick() }
        }

        // Menambahkan ke layout dengan Spacer (Space Between)
        navLayout.addView(btnBack)
        navLayout.addView(createFlexibleSpacer())
        navLayout.addView(btnNext)
        navLayout.addView(createFlexibleSpacer())
        navLayout.addView(btnRefresh)
        navLayout.addView(createFlexibleSpacer())
        navLayout.addView(btnExit)

        return navLayout
    }

    // Fungsi Helper membuat tombol icon
    // PERBAIKAN: Default applyRedTint = false agar warna asli XML (Merah) tidak tertimpa abu-abu
    private fun createNavIconButton(iconRes: Int, applyRedTint: Boolean = false): ImageButton {
        return ImageButton(context).apply {
            setImageResource(iconRes)
            setBackgroundColor(Color.TRANSPARENT) // Background transparan
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

            // Logika pewarnaan:
            if (applyRedTint) {
                // Paksa jadi merah (misal untuk tombol exit)
                setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark), PorterDuff.Mode.SRC_IN)
            } else {
                // Hapus filter apapun, gunakan warna asli dari file XML
                clearColorFilter()
            }

            // Ukuran tombol agak besar untuk touch target yang baik
            layoutParams = LinearLayout.LayoutParams(dpToPx(48f), dpToPx(48f))

            // Efek klik (Ripple) standar Android
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }
    }

    // Helper Spasi Fleksibel
    private fun createFlexibleSpacer(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply {
                weight = 1f
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}