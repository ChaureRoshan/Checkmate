package com.example.chess.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveKitVoiceState(
    val serverUrl: String = "wss://chess-pvoekttu.livekit.cloud",
    val projectId: String = "p_65ebmc1ge32",
    val isConfigured: Boolean = false,
    val isConnected: Boolean = false,
    val currentRoom: String? = null,
    val activeToken: String? = null
)

class LiveKitVoiceManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("livekit_voice_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        LiveKitVoiceState(
            serverUrl = "wss://chess-pvoekttu.livekit.cloud",
            projectId = "p_65ebmc1ge32",
            isConfigured = prefs.getString("livekit_api_key", "").isNullOrEmpty().not()
        )
    )
    val state: StateFlow<LiveKitVoiceState> = _state.asStateFlow()

    fun getApiKey(): String = prefs.getString("livekit_api_key", "") ?: ""
    fun getApiSecret(): String = prefs.getString("livekit_api_secret", "") ?: ""

    fun saveCredentials(apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString("livekit_api_key", apiKey.trim())
            .putString("livekit_api_secret", apiSecret.trim())
            .apply()

        _state.value = _state.value.copy(
            isConfigured = apiKey.trim().isNotEmpty() && apiSecret.trim().isNotEmpty()
        )
    }

    fun joinRoom(
        roomName: String,
        participantIdentity: String,
        participantName: String
    ): String? {
        val apiKey = getApiKey()
        val apiSecret = getApiSecret()

        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            Log.d("LiveKitVoiceManager", "LiveKit API Key or Secret not configured yet")
            return null
        }

        return try {
            val token = LiveKitTokenGenerator.createToken(
                apiKey = apiKey,
                apiSecret = apiSecret,
                roomName = roomName,
                participantIdentity = participantIdentity,
                participantName = participantName
            )
            _state.value = _state.value.copy(
                isConnected = true,
                currentRoom = roomName,
                activeToken = token
            )
            token
        } catch (e: Exception) {
            Log.e("LiveKitVoiceManager", "Failed to generate LiveKit token", e)
            null
        }
    }

    fun leaveRoom() {
        _state.value = _state.value.copy(
            isConnected = false,
            currentRoom = null,
            activeToken = null
        )
    }
}
