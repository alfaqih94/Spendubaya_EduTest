package com.spendubaya.edutest

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

/**
 * Mengelola tampilan dan logika Dialog Keluar.
 */
class ExitDialogManager(private val context: Context) {

    fun show(
        expectedToken: String?,
        onConfirm: () -> Unit,
        onDeny: () -> Unit
    ) {
        // Ambil font jika ada (gunakan try-catch atau null safety jika font tidak ditemukan di project lain)
        val customFont2 = ResourcesCompat.getFont(context, R.font.wdxll)
        val customFont = ResourcesCompat.getFont(context, R.font.cherrybomb)
        val customTitleView = TextView(context).apply {
            text = "Konfirmasi Keluar"
            setTextColor(Color.BLACK) // Warna judul
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f) // Ukuran teks judul
            setTypeface(customFont2, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan judul
            setPadding(0, dpToPx(0f), 0, dpToPx(0f)) // Padding
        }
        val customMessageView = TextView(context).apply {
            text = "Masukkan kode untuk keluar" // <--- BARIS BERUBAH: Pesan lebih umum
            setTextColor(Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(customFont2, Typeface.ITALIC)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dpToPx(0f), 0, dpToPx(8f))
        }

        // --- Setup UI Dialog Secara Programmatical ---
        val input = EditText(context).apply {
            hint = "Masukkan Kode"
            setTextColor(Color.BLACK)
            textSize = 30f
            setHintTextColor(Color.BLACK)
            // Gunakan TYPE_CLASS_NUMBER dan TYPE_NUMBER_VARIATION_PASSWORD untuk menyamarkan input
            inputType = InputType.TYPE_CLASS_NUMBER
            setTypeface(customFont2, Typeface.BOLD)
            setPadding(50, 40, 50, 40) // Padding internal
            setBackgroundResource(android.R.drawable.editbox_background) // Latar belakang kotak edit standar
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan teks di dalam EditText
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // Menyesuaikan padding agar lebih simetris dan menyediakan ruang yang cukup untuk tombol
            setPadding(dpToPx(24f), dpToPx(24f), dpToPx(24f), dpToPx(24f))
            setBackgroundColor(Color.WHITE) // Latar belakang putih untuk konten dialog
            gravity = Gravity.CENTER_HORIZONTAL // Tengahkan EditText secara horizontal di dalam tata letak ini
            addView(customTitleView)
            addView(customMessageView)
            addView(input) // Tambahkan EditText ke tata letak
        }

        // --- Build Dialog ---
        // Simpan instance dialog ke dalam variabel 'dialog'
        val dialog = AlertDialog.Builder(context)
            .setView(layout)
            .setCancelable(false) // Dialog tidak bisa ditutup dengan klik luar
            .setPositiveButton("KELUAR") { _, _ ->
                val inputCode = input.text.toString()
                if (inputCode == expectedToken && !expectedToken.isNullOrEmpty()) {
                    onConfirm() // Token benar
                } else {
                    Toast.makeText(context, "Kode salah!", Toast.LENGTH_SHORT).show()
                    onDeny() // Token salah
                }
            }
            .setNegativeButton("BATAL") { d, _ ->
                d.dismiss()
            }
            .show() // Tampilkan dialog

        // --- PERBAIKAN: Set Warna Tombol Secara Manual ---
        // Ini harus dilakukan SETELAH .show() dipanggil

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
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

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
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

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }
}