package com.spendubaya.edutest

import android.graphics.Color
import android.os.CountDownTimer
import android.widget.TextView
import java.util.Locale

/**
 * Class untuk menangani logika hitung mundur ujian.
 * Menerima callback untuk memberi tahu MainActivity kapan harus membunyikan alarm atau keluar.
 */
class ExamTimer(
    private val timerTextView: TextView,
    private val onWarning: () -> Unit, // Callback saat sisa waktu sedikit (untuk suara)
    private val onFinishExam: () -> Unit // Callback saat waktu habis
) {

    private var countdownTimer: CountDownTimer? = null
    private var warningSoundPlayed = false
    private var finalWarningSoundPlayed = false

    fun start(minutes: Int) {
        val totalMillis = minutes * 60 * 1000L

        cancel() // Hentikan timer lama jika ada

        countdownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                val minutesRemaining = (millisUntilFinished / (1000 * 60)) % 60
                val secondsRemaining = (millisUntilFinished / 1000) % 60

                // Update UI Text
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutesRemaining, secondsRemaining)
                timerTextView.text = timeFormatted

                // Logika Peringatan 5 Menit
                if (minutesRemaining <= 5 && !warningSoundPlayed && millisUntilFinished > 0) {
                    timerTextView.setTextColor(Color.RED)
                    onWarning.invoke() // Panggil fungsi suara di Main
                    warningSoundPlayed = true
                }

                // Logika Peringatan 1 Menit
                if (minutesRemaining <= 1 && !finalWarningSoundPlayed && millisUntilFinished > 0) {
                    onWarning.invoke() // Panggil fungsi suara di Main
                    finalWarningSoundPlayed = true
                }
            }

            override fun onFinish() {
                timerTextView.text = "00:00:00"
                onFinishExam.invoke()
            }
        }.start()
    }

    fun cancel() {
        countdownTimer?.cancel()
    }
}