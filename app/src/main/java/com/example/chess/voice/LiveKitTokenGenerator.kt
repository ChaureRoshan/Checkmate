package com.example.chess.voice

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LiveKitTokenGenerator {

    /**
     * Generates a signed LiveKit AccessToken (JWT) using HMAC-SHA256.
     */
    fun createToken(
        apiKey: String,
        apiSecret: String,
        roomName: String,
        participantIdentity: String,
        participantName: String,
        ttlSeconds: Long = 86400
    ): String {
        val now = System.currentTimeMillis() / 1000

        // JWT Header
        val header = JSONObject().apply {
            put("alg", "HS256")
            put("typ", "JWT")
        }

        // LiveKit Video Grants
        val videoGrant = JSONObject().apply {
            put("room", roomName)
            put("roomJoin", true)
            put("canPublish", true)
            put("canSubscribe", true)
            put("canPublishData", true)
        }

        // JWT Payload / Claims
        val payload = JSONObject().apply {
            put("iss", apiKey)
            put("sub", participantIdentity)
            put("name", participantName)
            put("nbf", now - 5)
            put("exp", now + ttlSeconds)
            put("video", videoGrant)
        }

        val headerBase64 = base64UrlEncode(header.toString().toByteArray(StandardCharsets.UTF_8))
        val payloadBase64 = base64UrlEncode(payload.toString().toByteArray(StandardCharsets.UTF_8))
        val contentToSign = "$headerBase64.$payloadBase64"

        val signature = hmacSha256(contentToSign, apiSecret)
        val signatureBase64 = base64UrlEncode(signature)

        return "$contentToSign.$signatureBase64"
    }

    private fun hmacSha256(data: String, key: String): ByteArray {
        val secretKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
