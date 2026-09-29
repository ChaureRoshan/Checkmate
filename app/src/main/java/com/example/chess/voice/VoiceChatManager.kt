package com.example.chess.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random

data class VoiceChatState(
    val hasPermission: Boolean = false,
    val isMicMuted: Boolean = true,
    val isPushToTalk: Boolean = false,
    val isRecording: Boolean = false,
    val userAmplitude: Float = 0f,
    val isOpponentSpeaking: Boolean = false,
    val opponentAmplitude: Float = 0f,
    val isDeafened: Boolean = false,
    val statusText: String = "Voice Ready"
)

class VoiceChatManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var recordingJob: Job? = null
    private var opponentSimulationJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _state = MutableStateFlow(
        VoiceChatState(hasPermission = checkPermission())
    )
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    init {
        updatePermissionStatus()
    }

    fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updatePermissionStatus() {
        val granted = checkPermission()
        _state.value = _state.value.copy(
            hasPermission = granted,
            statusText = if (granted) "Voice Connected" else "Mic Permission Required"
        )
    }

    fun toggleMicMute() {
        if (!_state.value.hasPermission) return
        val newMuted = !_state.value.isMicMuted
        _state.value = _state.value.copy(
            isMicMuted = newMuted,
            statusText = if (newMuted) "Microphone Muted" else "Microphone Live"
        )

        if (!newMuted) {
            startAudioCapture()
        } else {
            stopAudioCapture()
        }
    }

    fun startPushToTalk() {
        if (!_state.value.hasPermission) return
        _state.value = _state.value.copy(
            isPushToTalk = true,
            isMicMuted = false,
            statusText = "Transmitting Voice..."
        )
        startAudioCapture()
    }

    fun stopPushToTalk() {
        _state.value = _state.value.copy(
            isPushToTalk = false,
            isMicMuted = true,
            statusText = "Voice Ready"
        )
        stopAudioCapture()
    }

    fun toggleDeafen() {
        val newDeafen = !_state.value.isDeafened
        _state.value = _state.value.copy(
            isDeafened = newDeafen,
            statusText = if (newDeafen) "Opponent Deafened" else "Opponent Audio Active"
        )
    }

    fun simulateOpponentSpeaking(speaking: Boolean, durationMs: Long = 2500) {
        if (_state.value.isDeafened) return
        opponentSimulationJob?.cancel()
        if (speaking) {
            opponentSimulationJob = scope.launch {
                val startTime = System.currentTimeMillis()
                _state.value = _state.value.copy(isOpponentSpeaking = true)
                while (isActive && System.currentTimeMillis() - startTime < durationMs) {
                    val fakeAmp = (0.2f + Random.nextFloat() * 0.7f)
                    _state.value = _state.value.copy(opponentAmplitude = fakeAmp)
                    delay(80)
                }
                _state.value = _state.value.copy(
                    isOpponentSpeaking = false,
                    opponentAmplitude = 0f
                )
            }
        } else {
            _state.value = _state.value.copy(
                isOpponentSpeaking = false,
                opponentAmplitude = 0f
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        if (recordingJob?.isActive == true) return
        if (!checkPermission()) return

        recordingJob = scope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    _state.value = _state.value.copy(isRecording = true)

                    val audioBuffer = ShortArray(bufferSize / 2)
                    while (isActive && _state.value.isRecording) {
                        val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                        if (readCount > 0) {
                            var sumSquares = 0.0
                            for (i in 0 until readCount) {
                                val s = audioBuffer[i]
                                sumSquares += (s * s).toDouble()
                            }
                            val rms = sqrt(sumSquares / readCount)
                            // Scale RMS logarithmically between 0.0 and 1.0
                            val db = if (rms > 1.0) 20 * log10(rms) else 0.0
                            val normalized = ((db - 20) / 70.0).coerceIn(0.0, 1.0).toFloat()

                            _state.value = _state.value.copy(userAmplitude = normalized)
                        }
                        delay(60)
                    }
                }
            } catch (_: Exception) {
                // Audio recording fallback gracefully in environments without audio input
            } finally {
                safeReleaseAudioRecord()
            }
        }
    }

    private fun stopAudioCapture() {
        recordingJob?.cancel()
        recordingJob = null
        _state.value = _state.value.copy(
            isRecording = false,
            userAmplitude = 0f
        )
        safeReleaseAudioRecord()
    }

    private fun safeReleaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun release() {
        stopAudioCapture()
        opponentSimulationJob?.cancel()
    }
}
