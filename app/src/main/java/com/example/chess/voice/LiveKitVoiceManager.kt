package com.example.chess.voice

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveKitVoiceState(
    val serverUrl: String = LiveKitVoiceManager.SERVER_URL,
    val projectId: String = LiveKitVoiceManager.PROJECT_ID,
    val isConfigured: Boolean = false
)

/**
 * Holds the LiveKit credentials and creates join tokens.
 *
 * Keys come from (1) the in-app dialog (overrides) or (2) BuildConfig, which is filled from
 * .env / GitHub secrets at build time. NOTE: a secret that ships inside an APK can be extracted.
 * That is acceptable for a personal / friends-only app; for a public release, move token
 * creation to a tiny server and ship only the token endpoint.
 */
class LiveKitVoiceManager(context: Context) {

    companion object {
        const val SERVER_URL = "wss://chess-pvoekttu.livekit.cloud"
        const val PROJECT_ID = "p_65ebmc1ge32"
        private const val PLACEHOLDER_PREFIX = "YOUR_"
    }

    private val prefs = context.getSharedPreferences("livekit_voice_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(LiveKitVoiceState(isConfigured = hasCredentials()))
    val state: StateFlow<LiveKitVoiceState> = _state.asStateFlow()

    val serverUrl: String get() = SERVER_URL

    /** Only the user-entered override (used to prefill the dialog). */
    fun getApiKey(): String = prefs.getString("livekit_api_key", "") ?: ""
    fun getApiSecret(): String = prefs.getString("livekit_api_secret", "") ?: ""

    private fun effectiveKey(): String =
        getApiKey().ifEmpty { buildValue(BuildConfig.LIVEKIT_API_KEY) }

    private fun effectiveSecret(): String =
        getApiSecret().ifEmpty { buildValue(BuildConfig.LIVEKIT_API_SECRET) }

    private fun buildValue(v: String): String =
        if (v.isBlank() || v.startsWith(PLACEHOLDER_PREFIX)) "" else v

    private fun hasCredentials(): Boolean =
        effectiveKey().isNotEmpty() && effectiveSecret().isNotEmpty()

    fun saveCredentials(apiKey: String, apiSecret: String) {
        prefs.edit()
            .putString("livekit_api_key", apiKey.trim())
            .putString("livekit_api_secret", apiSecret.trim())
            .apply()
        _state.value = _state.value.copy(isConfigured = hasCredentials())
    }

    /** Returns a signed join token, or null if no credentials are configured. */
    fun createToken(roomName: String, identity: String, displayName: String): String? {
        val key = effectiveKey()
        val secret = effectiveSecret()
        if (key.isEmpty() || secret.isEmpty()) {
            Log.d("LiveKitVoiceManager", "LiveKit API key/secret not configured")
            return null
        }
        return try {
            LiveKitTokenGenerator.createToken(
                apiKey = key,
                apiSecret = secret,
                roomName = roomName,
                participantIdentity = identity,
                participantName = displayName
            )
        } catch (e: Exception) {
            Log.e("LiveKitVoiceManager", "Failed to generate LiveKit token", e)
            null
        }
    }
}
