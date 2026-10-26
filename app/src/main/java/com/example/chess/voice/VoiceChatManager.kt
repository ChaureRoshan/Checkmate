package com.example.chess.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteAudioTrack
import io.livekit.android.room.track.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

data class VoiceChatState(
    val hasPermission: Boolean = false,
    val isMicMuted: Boolean = true,
    val isPushToTalk: Boolean = false,
    val isRecording: Boolean = false,        // true while our mic is actually live
    val userAmplitude: Float = 0f,
    val isOpponentSpeaking: Boolean = false,
    val opponentAmplitude: Float = 0f,
    val isDeafened: Boolean = false,
    val isConnected: Boolean = false,
    val opponentInVoice: Boolean = false,    // someone else is connected to the same voice room
    val statusText: String = "Voice Ready"
)

/**
 * Real voice chat over LiveKit. One LiveKit room per chess room code, so both players
 * (and nobody else) hear each other. Same public API as the old fake implementation,
 * so VoiceChatBar keeps working unchanged.
 */
class VoiceChatManager(
    private val context: Context,
    private val liveKit: LiveKitVoiceManager
) {
    private val tag = "VoiceChatManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var room: Room? = null
    private var connectJob: Job? = null
    private var levelJob: Job? = null
    private var connectedRoomName: String? = null
    private var pendingConnect: Triple<String, String, String>? = null

    private val _state = MutableStateFlow(VoiceChatState(hasPermission = checkPermission()))
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    fun checkPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun updatePermissionStatus() {
        val granted = checkPermission()
        _state.update {
            it.copy(
                hasPermission = granted,
                statusText = when {
                    !granted -> "Mic Permission Required"
                    it.isConnected -> "Voice Connected"
                    else -> it.statusText
                }
            )
        }
        // We only join the voice room once the mic is allowed. Joining first and getting the
        // permission later can leave the microphone silent on some phones.
        val pending = pendingConnect
        if (granted && pending != null) {
            pendingConnect = null
            connect(pending.first, pending.second, pending.third)
        }
    }

    /** Join the voice room belonging to this chess room. Safe to call repeatedly. */
    fun connect(roomCode: String, identity: String, displayName: String) {
        val roomName = "chess_" + roomCode.trim().uppercase()
        if (connectedRoomName == roomName) return
        disconnect()

        if (!checkPermission()) {
            pendingConnect = Triple(roomCode, identity, displayName)
            _state.update { it.copy(hasPermission = false, statusText = "Mic Permission Required") }
            return
        }

        val token = liveKit.createToken(roomName, identity, displayName)
        if (token == null) {
            _state.update { it.copy(statusText = "Voice not configured") }
            return
        }

        connectedRoomName = roomName
        _state.update { it.copy(statusText = "Connecting voice...") }

        connectJob = scope.launch {
            try {
                val newRoom = LiveKit.create(appContext = context.applicationContext)
                room = newRoom
                newRoom.connect(liveKit.serverUrl, token)
                _state.update { it.copy(isConnected = true, statusText = "Voice Connected") }
                startLevelLoop(newRoom)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "LiveKit connect error", e)
                try { room?.release() } catch (_: Exception) {}
                room = null
                connectedRoomName = null
                _state.update { it.copy(isConnected = false, statusText = "Voice connection failed") }
            }
        }
    }

    fun disconnect() {
        pendingConnect = null
        connectJob?.cancel()
        connectJob = null
        levelJob?.cancel()
        levelJob = null
        try {
            room?.disconnect()
            room?.release()
        } catch (e: Exception) {
            Log.w(tag, "disconnect error", e)
        }
        room = null
        connectedRoomName = null
        _state.update {
            it.copy(
                isConnected = false,
                opponentInVoice = false,
                isMicMuted = true,
                isPushToTalk = false,
                isRecording = false,
                userAmplitude = 0f,
                isOpponentSpeaking = false,
                opponentAmplitude = 0f,
                statusText = "Voice Ready"
            )
        }
    }

    fun toggleMicMute() {
        if (!_state.value.hasPermission) return
        if (!_state.value.isConnected) {
            _state.update { it.copy(statusText = "Voice not connected yet") }
            return
        }
        setMic(_state.value.isMicMuted) // muted -> turn on, live -> turn off
    }

    fun startPushToTalk() {
        if (!_state.value.hasPermission || !_state.value.isConnected) return
        _state.update { it.copy(isPushToTalk = true) }
        setMic(true)
    }

    fun stopPushToTalk() {
        _state.update { it.copy(isPushToTalk = false) }
        setMic(false)
    }

    fun toggleDeafen() {
        val newDeafen = !_state.value.isDeafened
        _state.update {
            it.copy(
                isDeafened = newDeafen,
                isOpponentSpeaking = if (newDeafen) false else it.isOpponentSpeaking,
                statusText = if (newDeafen) "Opponent Muted" else "Opponent Audio Active"
            )
        }
        applyRemoteVolume()
    }

    private fun setMic(enabled: Boolean) {
        val r = room ?: return
        _state.update {
            it.copy(
                isMicMuted = !enabled,
                isRecording = enabled,
                statusText = if (enabled) "Microphone Live" else "Microphone Muted"
            )
        }
        scope.launch {
            try {
                r.localParticipant.setMicrophoneEnabled(enabled)
                if (enabled && r.localParticipant.getTrackPublication(Track.Source.MICROPHONE) == null) {
                    _state.update {
                        it.copy(isMicMuted = true, isRecording = false, statusText = "Mic start nahi hua, dobara dabao")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "setMicrophoneEnabled failed", e)
                _state.update { it.copy(isMicMuted = true, isRecording = false, statusText = "Mic error") }
            }
        }
    }

    private fun applyRemoteVolume() {
        val r = room ?: return
        val volume = if (_state.value.isDeafened) 0.0 else 1.0
        r.remoteParticipants.values.forEach { p ->
            (p.getTrackPublication(Track.Source.MICROPHONE)?.track as? RemoteAudioTrack)?.setVolume(volume)
        }
    }

    private fun startLevelLoop(r: Room) {
        levelJob?.cancel()
        levelJob = scope.launch {
            while (isActive) {
                val remotes = r.remoteParticipants.values
                val oppLevel = remotes.maxOfOrNull { it.audioLevel } ?: 0f
                val oppSpeaking = remotes.any { it.isSpeaking }
                val micLive = !_state.value.isMicMuted
                val myLevel = if (micLive) r.localParticipant.audioLevel else 0f
                val deafened = _state.value.isDeafened

                _state.update {
                    it.copy(
                        opponentInVoice = remotes.isNotEmpty(),
                        userAmplitude = sqrt(myLevel.coerceIn(0f, 1f)),
                        opponentAmplitude = if (deafened) 0f else sqrt(oppLevel.coerceIn(0f, 1f)),
                        isOpponentSpeaking = oppSpeaking && !deafened
                    )
                }
                // New remote tracks can appear at any time; keep their volume in sync with "deafen".
                if (deafened) applyRemoteVolume()
                delay(100)
            }
        }
    }

    fun release() {
        disconnect()
        scope.cancel()
    }
}
