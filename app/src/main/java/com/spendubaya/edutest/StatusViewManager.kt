package com.spendubaya.edutest

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

data class StatusComponents(
    val batteryText: TextView,
    val timeText: TextView,
    val timerText: TextView
)

class StatusViewManager(private val context: Context) {

    /**
     * Membuat view status dengan layout: [Baterai Jam] <--- Spasi ---> [Timer]
     * @param container Container utama dari TopBarManager (Horizontal Linear Layout)
     */
    fun createStatusViews(container: LinearLayout): StatusComponents {

        // --- GRUP KIRI (Baterai & Jam) ---
        val leftGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Baterai (Merah)
        val batteryText = createBaseTextView(isRed = true)
        leftGroup.addView(createStatusItem(R.drawable.ikon_baterai, batteryText, isRed = true))

        // 2. Jam (Merah)
        val timeText = createBaseTextView(isRed = true)
        // Tambahkan margin kiri sedikit agar tidak nempel baterai
        val timeItem = createStatusItem(R.drawable.ikon_jam, timeText, isRed = true).apply {
            setPadding(dpToPx(12f), 0, 0, 0)
        }
        leftGroup.addView(timeItem)


        // --- SPACER TENGAH (Pendorong) ---
        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1).apply {
                weight = 1f // Ini yang bikin Kiri dan Kanan terpisah mentok
            }
        }


        // --- GRUP KANAN (Timer) ---
        val rightGroup = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 3. Timer (Merah & Bold)
        val timerText = createBaseTextView(isRed = true).apply {
            setTypeface(null, Typeface.BOLD)
        }
        rightGroup.addView(createStatusItem(R.drawable.ikon_timer, timerText, isRed = true))


        // --- MASUKKAN KE CONTAINER UTAMA ---
        container.removeAllViews() // Bersihkan dulu jaga-jaga
        container.addView(leftGroup)
        container.addView(spacer)
        container.addView(rightGroup)

        return StatusComponents(batteryText, timeText, timerText)
    }

    private fun createBaseTextView(isRed: Boolean = false): TextView {
        return TextView(context).apply {
            // Ubah warna teks jadi Merah jika diminta
            setTextColor(if (isRed) Color.RED else Color.DKGRAY)
            textSize = 16f
        }
    }

    private fun createStatusItem(iconRes: Int, textView: TextView, isRed: Boolean = false): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icon = ImageView(context).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(dpToPx(20f), dpToPx(20f)).apply {
                setMargins(0, 0, dpToPx(4f), 0)
            }
            // Ubah warna Icon jadi Merah jika diminta
            if (isRed) {
                setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark), PorterDuff.Mode.SRC_IN)
            } else {
                clearColorFilter()
            }
        }

        layout.addView(icon)
        layout.addView(textView)
        return layout
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }
}