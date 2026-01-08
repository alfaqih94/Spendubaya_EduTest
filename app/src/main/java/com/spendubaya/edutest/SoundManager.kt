package com.spendubaya.edutest

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log

/**
 * Class khusus untuk menangani logika suara dan volume.
 * Memisahkan ini dari MainActivity membuat kode lebih bersih.
 */
class SoundManager(private val context: Context) {

    private var attentionMediaPlayer: MediaPlayer? = null
    private var exitMediaPlayer: MediaPlayer? = null // Jika Anda punya suara khusus keluar
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    init {
        initializePlayers()
    }

    private fun initializePlayers() {
        try {
            // Inisialisasi suara alarm/attention
            attentionMediaPlayer = MediaPlayer.create(context, R.raw.retro)
            // Tambahkan suara lain jika perlu
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SoundManager", "Error init sounds: ${e.message}")
        }
    }

    fun playAttentionSound() {
        try {
            // Paksa volume maksimal
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

            attentionMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.seekTo(0)
                } else {
                    player.start()
                }
            } ?: run {
                // Jika player null (terhapus), buat ulang
                initializePlayers()
                attentionMediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: ${e.message}")
        }
    }

    fun release() {
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
        exitMediaPlayer?.release()
        exitMediaPlayer = null
    }
}