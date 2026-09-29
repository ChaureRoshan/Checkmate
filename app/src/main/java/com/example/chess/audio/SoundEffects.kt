package com.example.chess.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEffects(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isMuted = false
    private var isHapticsEnabled = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setHapticsEnabled(enabled: Boolean) {
        isHapticsEnabled = enabled
    }

    fun playMoveSound() {
        if (isHapticsEnabled) vibrate(18, 80)
        if (isMuted) return
        scope.launch {
            // Wood-like crisp tap (decaying sine 420Hz)
            playTone(frequency = 440.0, durationMs = 50, decay = 35.0)
        }
    }

    fun playCaptureSound() {
        if (isHapticsEnabled) vibrate(35, 180)
        if (isMuted) return
        scope.launch {
            // Resonant strike (lower punch 260Hz + 180Hz)
            playTone(frequency = 280.0, durationMs = 85, decay = 25.0)
        }
    }

    fun playCheckSound() {
        if (isHapticsEnabled) vibrate(45, 220)
        if (isMuted) return
        scope.launch {
            // Dual chime warning
            playTone(frequency = 587.33, durationMs = 70, decay = 20.0) // D5
            playTone(frequency = 783.99, durationMs = 120, decay = 15.0) // G5
        }
    }

    fun playVictorySound() {
        if (isHapticsEnabled) vibrate(60, 255)
        if (isMuted) return
        scope.launch {
            playTone(frequency = 523.25, durationMs = 80, decay = 15.0) // C5
            playTone(frequency = 659.25, durationMs = 80, decay = 15.0) // E5
            playTone(frequency = 783.99, durationMs = 200, decay = 10.0) // G5
        }
    }

    fun playDefeatSound() {
        if (isHapticsEnabled) vibrate(50, 160)
        if (isMuted) return
        scope.launch {
            playTone(frequency = 440.0, durationMs = 120, decay = 15.0)
            playTone(frequency = 370.0, durationMs = 220, decay = 10.0)
        }
    }

    fun playLowTimeTick() {
        if (isHapticsEnabled) vibrate(12, 60)
        if (isMuted) return
        scope.launch {
            // Subtle high-pitch clock tick
            playTone(frequency = 880.0, durationMs = 30, decay = 40.0)
        }
    }

    fun playTimeoutSound() {
        if (isHapticsEnabled) vibrate(70, 255)
        if (isMuted) return
        scope.launch {
            // Buzzer-like double tone for clock flag drop
            playTone(frequency = 330.0, durationMs = 150, decay = 8.0)
            playTone(frequency = 220.0, durationMs = 280, decay = 6.0)
        }
    }

    private fun playTone(frequency: Double, durationMs: Int, decay: Double) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-decay * t)
            val sample = sin(2.0 * PI * frequency * t) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.75).toInt().toShort()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            Thread.sleep(durationMs.toLong() + 20)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Exception) {
            // Ignore sound interruptions gracefully
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Ignore vibration permissions or hardware absence
        }
    }
}
