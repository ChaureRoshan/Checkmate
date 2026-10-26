package com.example.chess.multiplayer

import android.util.Log
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.PlayerInfo
import com.example.chess.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CloudMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val notation: String,
    val promotionType: String? = null,
    val moveNumber: Int = 0
)

data class CloudRoom(
    val code: String,
    val hostId: String,
    val hostName: String,
    val hostRating: Int,
    val hostColor: String,
    val guestId: String? = null,
    val guestName: String? = null,
    val guestRating: Int? = null,
    val isStarted: Boolean = false,
    val timeControlMinutes: Int = 10,
    val incrementSeconds: Int = 0,
    val whiteClockMs: Long = 600000L,
    val blackClockMs: Long = 600000L,
    val lastMove: CloudMove? = null,
    val moveCount: Int = 0,
    val status: String = "ACTIVE", // ACTIVE, RESIGNED, DRAW_OFFERED, DRAW_AGREED, CHECKMATE, TIMEOUT
    val drawOfferedBy: String? = null,
    val resignedBy: String? = null,
    val voiceHostSpeaking: Boolean = false,
    val voiceHostAmp: Float = 0f,
    val voiceGuestSpeaking: Boolean = false,
    val voiceGuestAmp: Float = 0f,
    val timeoutLoserId: String? = null   // set when a player's clock ran out
)

data class CloudProfile(
    val profile: com.example.chess.data.UserProfileEntity,
    val nameChanges: Int
)

data class OnlinePlayer(
    val id: String,
    val name: String,
    val rating: Int,
    val avatarIndex: Int,
    val inGame: Boolean
)

data class Challenge(
    val fromId: String,
    val fromName: String,
    val fromRating: Int,
    val roomCode: String,
    val timeControlMinutes: Int,
    val status: String,   // PENDING, ACCEPTED, DECLINED
    val createdAt: Long
)

object FirebaseRealtimeManager {

    private const val TAG = "FirebaseRTDB"
    const val DATABASE_URL = "https://walkie-talkie-718d3-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun createRoom(
        code: String,
        host: PlayerInfo,
        timeControlMinutes: Int,
        incrementSeconds: Int = 0,
        hostColor: PieceColor = PieceColor.WHITE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("code", code)
                put("hostId", host.id)
                put("hostName", host.name)
                put("hostRating", host.rating)
                put("hostColor", hostColor.name)
                put("guestId", JSONObject.NULL)
                put("guestName", JSONObject.NULL)
                put("guestRating", JSONObject.NULL)
                put("isStarted", false)
                put("timeControlMinutes", timeControlMinutes)
                put("incrementSeconds", incrementSeconds)
                put("whiteClockMs", timeControlMinutes * 60 * 1000L)
                put("blackClockMs", timeControlMinutes * 60 * 1000L)
                put("moveCount", 0)
                put("status", "WAITING")
                put("createdAt", System.currentTimeMillis())
            }

            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .put(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create room in Firebase RTDB", e)
            false
        }
    }

    suspend fun getRoom(code: String): CloudRoom? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful || body == null || body == "null") {
                return@withContext null
            }

            parseRoomJson(JSONObject(body))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get room from Firebase RTDB", e)
            null
        }
    }

    suspend fun joinRoom(code: String, guest: PlayerInfo): CloudRoom? = withContext(Dispatchers.IO) {
        try {
            val existing = getRoom(code) ?: return@withContext null

            val patchJson = JSONObject().apply {
                put("guestId", guest.id)
                put("guestName", guest.name)
                put("guestRating", guest.rating)
                put("isStarted", true)
                put("status", "ACTIVE")
            }

            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .patch(patchJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            getRoom(code)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join room in Firebase RTDB", e)
            null
        }
    }

    suspend fun postMove(
        code: String,
        from: Position,
        to: Position,
        notation: String,
        promotionType: PieceType?,
        moveIndex: Int,
        whiteClockMs: Long,
        blackClockMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val moveObj = JSONObject().apply {
                put("fromRow", from.row)
                put("fromCol", from.col)
                put("toRow", to.row)
                put("toCol", to.col)
                put("notation", notation)
                put("promotionType", promotionType?.name ?: JSONObject.NULL)
                put("moveNumber", moveIndex)
                put("timestamp", System.currentTimeMillis())
            }

            val patchJson = JSONObject().apply {
                put("lastMove", moveObj)
                put("moveCount", moveIndex)
                put("whiteClockMs", whiteClockMs)
                put("blackClockMs", blackClockMs)
                put("status", "ACTIVE")
                put("drawOfferedBy", JSONObject.NULL)
            }

            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .patch(patchJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post move to Firebase RTDB", e)
            false
        }
    }

    suspend fun updateVoiceState(
        code: String,
        isHost: Boolean,
        isSpeaking: Boolean,
        amplitude: Float
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val keySpeaking = if (isHost) "voiceHostSpeaking" else "voiceGuestSpeaking"
            val keyAmp = if (isHost) "voiceHostAmp" else "voiceGuestAmp"

            val json = JSONObject().apply {
                put(keySpeaking, isSpeaking)
                put(keyAmp, amplitude.toDouble())
            }

            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendGameAction(
        code: String,
        status: String,
        playerId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("status", status)
                if (status == "RESIGNED") {
                    put("resignedBy", playerId)
                } else if (status == "DRAW_OFFERED") {
                    put("drawOfferedBy", playerId)
                }
            }

            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /** Tell the other phone that [loserId] ran out of time. Kept in its own field so a late move can't overwrite it. */
    suspend fun sendTimeout(code: String, loserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("status", "TIMEOUT")
                put("timeoutLoserId", loserId)
            }
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /** Profile saved for a Google account, so rating and stats follow the person to any phone. */
    suspend fun getCloudProfile(uid: String): CloudProfile? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url("$DATABASE_URL/chess_users/$uid.json").get().build()
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful || body.isNullOrEmpty() || body == "null") return@withContext null
                    val o = JSONObject(body)
                    CloudProfile(
                        profile = com.example.chess.data.UserProfileEntity(
                            guestId = "",
                            displayName = o.optString("displayName", "Player"),
                            rating = o.optInt("rating", 1200),
                            wins = o.optInt("wins", 0),
                            losses = o.optInt("losses", 0),
                            draws = o.optInt("draws", 0)
                        ),
                        nameChanges = o.optInt("nameChanges", 0)
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun putCloudProfile(
        uid: String,
        profile: com.example.chess.data.UserProfileEntity,
        nameChanges: Int
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("displayName", profile.displayName)
                    put("rating", profile.rating)
                    put("wins", profile.wins)
                    put("losses", profile.losses)
                    put("draws", profile.draws)
                    put("nameChanges", nameChanges)
                }
                val request = Request.Builder()
                    .url("$DATABASE_URL/chess_users/$uid.json")
                    .put(json.toString().toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun findOrCreateQuickMatch(
        guest: PlayerInfo,
        timeMinutes: Int,
        incrementSeconds: Int = 0
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        try {
            // Check for available open rooms in chess_rooms
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms.json?orderBy=\"status\"&equalTo=\"WAITING\"&limitToFirst=3")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && !body.isNullOrEmpty() && body != "null" && body != "{}") {
                val json = JSONObject(body)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val code = keys.next()
                    val roomObj = json.getJSONObject(code)
                    val hostId = roomObj.optString("hostId")
                    if (hostId != guest.id) {
                        // Join this waiting room!
                        val joined = joinRoom(code, guest)
                        if (joined != null) {
                            return@withContext (code to false) // joined as guest
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Quick match queue lookup error", e)
        }

        // Otherwise create new room code
        val newCode = "KNG-${(100..999).random()}"
        createRoom(newCode, guest, timeMinutes, incrementSeconds)
        (newCode to true) // host
    }

    fun startListeningToRoom(
        scope: CoroutineScope,
        code: String,
        onRoomUpdated: (CloudRoom) -> Unit
    ): Job {
        return scope.launch(Dispatchers.IO) {
            var lastMoveIndexSeen = -1
            var lastStatusSeen = ""
            var lastGuestIdSeen = ""

            while (isActive) {
                try {
                    val room = getRoom(code)
                    if (room != null) {
                        onRoomUpdated(room)
                    }
                } catch (e: Exception) {
                    // Ignore transient network hiccups
                }
                delay(600) // 600ms polling for near instant real-time moves and voice status
            }
        }
    }

    // ------------------------------------------------------------------
    // Presence (who is online) and direct challenges. No accounts: every
    // device uses its own guest id. Entries older than PRESENCE_TTL_MS are
    // treated as offline; the app refreshes its entry every few seconds.
    // ------------------------------------------------------------------

    private const val PRESENCE_TTL_MS = 35_000L
    private const val PRESENCE_CLEANUP_MS = 10 * 60_000L
    private const val CHALLENGE_TTL_MS = 90_000L

    private val serverTimestamp: JSONObject get() = JSONObject().put(".sv", "timestamp")

    /** Server clock from the HTTP Date header, so phones with a wrong clock don't break "online" checks. */
    private fun serverNow(response: Response): Long =
        response.headers.getDate("Date")?.time ?: System.currentTimeMillis()

    suspend fun updatePresence(player: PlayerInfo, inGame: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", player.id)
                put("name", player.name)
                put("rating", player.rating)
                put("avatarIndex", player.avatarIndex)
                put("inGame", inGame)
                put("lastSeen", serverTimestamp)
            }
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_presence/${player.id}.json")
                .put(json.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.w(TAG, "presence update failed", e)
            false
        }
    }

    suspend fun removePresence(playerId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_presence/$playerId.json")
                .delete()
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getOnlinePlayers(selfId: String): List<OnlinePlayer> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$DATABASE_URL/chess_presence.json").get().build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrEmpty() || body == "null") {
                    return@withContext emptyList()
                }
                val now = serverNow(response)
                val root = JSONObject(body)
                val result = mutableListOf<OnlinePlayer>()
                val staleIds = mutableListOf<String>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val o = root.optJSONObject(id) ?: continue
                    val age = now - o.optLong("lastSeen", 0L)
                    if (age > PRESENCE_CLEANUP_MS) staleIds.add(id)
                    if (id == selfId || age > PRESENCE_TTL_MS) continue
                    result.add(
                        OnlinePlayer(
                            id = id,
                            name = o.optString("name", "Player"),
                            rating = o.optInt("rating", 1200),
                            avatarIndex = o.optInt("avatarIndex", 0),
                            inGame = o.optBoolean("inGame", false)
                        )
                    )
                }
                // Best-effort cleanup of long-dead entries so the list doesn't grow forever
                staleIds.take(5).forEach { removePresence(it) }
                result.sortedWith(compareBy<OnlinePlayer> { it.inGame }.thenBy { it.name.lowercase() })
            }
        } catch (e: Exception) {
            Log.w(TAG, "online players fetch failed", e)
            emptyList()
        }
    }

    suspend fun sendChallenge(
        toId: String,
        from: PlayerInfo,
        roomCode: String,
        timeControlMinutes: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("fromId", from.id)
                put("fromName", from.name)
                put("fromRating", from.rating)
                put("roomCode", roomCode)
                put("timeControlMinutes", timeControlMinutes)
                put("status", "PENDING")
                put("createdAt", serverTimestamp)
            }
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_challenges/$toId/${from.id}.json")
                .put(json.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            Log.e(TAG, "sendChallenge failed", e)
            false
        }
    }

    /** Fresh PENDING challenges addressed to [myId], newest first. */
    suspend fun getIncomingChallenges(myId: String): List<Challenge> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$DATABASE_URL/chess_challenges/$myId.json").get().build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrEmpty() || body == "null") {
                    return@withContext emptyList()
                }
                val now = serverNow(response)
                val root = JSONObject(body)
                val list = mutableListOf<Challenge>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val o = root.optJSONObject(key) ?: continue
                    val created = o.optLong("createdAt", 0L)
                    if (o.optString("status") != "PENDING") continue
                    if (now - created > CHALLENGE_TTL_MS) continue
                    list.add(
                        Challenge(
                            fromId = o.optString("fromId", key),
                            fromName = o.optString("fromName", "Player"),
                            fromRating = o.optInt("fromRating", 1200),
                            roomCode = o.optString("roomCode"),
                            timeControlMinutes = o.optInt("timeControlMinutes", 10),
                            status = "PENDING",
                            createdAt = created
                        )
                    )
                }
                list.filter { it.roomCode.isNotEmpty() }.sortedByDescending { it.createdAt }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Status of a challenge we sent, or null if it was deleted / doesn't exist. */
    suspend fun getChallengeStatus(toId: String, fromId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_challenges/$toId/$fromId.json")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrEmpty() || body == "null") return@withContext null
                JSONObject(body).optString("status", "").ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun setChallengeStatus(toId: String, fromId: String, status: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().put("status", status)
                val request = Request.Builder()
                    .url("$DATABASE_URL/chess_challenges/$toId/$fromId.json")
                    .patch(json.toString().toRequestBody(jsonMediaType))
                    .build()
                httpClient.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }

    suspend fun deleteChallenge(toId: String, fromId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_challenges/$toId/$fromId.json")
                .delete()
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteRoom(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$DATABASE_URL/chess_rooms/$code.json")
                .delete()
                .build()
            httpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseRoomJson(json: JSONObject): CloudRoom {
        val lastMoveObj = json.optJSONObject("lastMove")
        val cloudMove = if (lastMoveObj != null) {
            CloudMove(
                fromRow = lastMoveObj.optInt("fromRow"),
                fromCol = lastMoveObj.optInt("fromCol"),
                toRow = lastMoveObj.optInt("toRow"),
                toCol = lastMoveObj.optInt("toCol"),
                notation = lastMoveObj.optString("notation"),
                promotionType = if (lastMoveObj.has("promotionType") && !lastMoveObj.isNull("promotionType"))
                    lastMoveObj.optString("promotionType") else null,
                moveNumber = lastMoveObj.optInt("moveNumber")
            )
        } else null

        return CloudRoom(
            code = json.optString("code"),
            hostId = json.optString("hostId"),
            hostName = json.optString("hostName"),
            hostRating = json.optInt("hostRating", 1200),
            hostColor = json.optString("hostColor", "WHITE"),
            guestId = if (json.has("guestId") && !json.isNull("guestId")) json.optString("guestId") else null,
            guestName = if (json.has("guestName") && !json.isNull("guestName")) json.optString("guestName") else null,
            guestRating = if (json.has("guestRating") && !json.isNull("guestRating")) json.optInt("guestRating") else null,
            isStarted = json.optBoolean("isStarted", false),
            timeControlMinutes = json.optInt("timeControlMinutes", 10),
            incrementSeconds = json.optInt("incrementSeconds", 0),
            whiteClockMs = json.optLong("whiteClockMs", 600000L),
            blackClockMs = json.optLong("blackClockMs", 600000L),
            lastMove = cloudMove,
            moveCount = json.optInt("moveCount", 0),
            status = json.optString("status", "ACTIVE"),
            drawOfferedBy = if (json.has("drawOfferedBy") && !json.isNull("drawOfferedBy")) json.optString("drawOfferedBy") else null,
            resignedBy = if (json.has("resignedBy") && !json.isNull("resignedBy")) json.optString("resignedBy") else null,
            voiceHostSpeaking = json.optBoolean("voiceHostSpeaking", false),
            voiceHostAmp = json.optDouble("voiceHostAmp", 0.0).toFloat(),
            voiceGuestSpeaking = json.optBoolean("voiceGuestSpeaking", false),
            voiceGuestAmp = json.optDouble("voiceGuestAmp", 0.0).toFloat(),
            timeoutLoserId = if (json.has("timeoutLoserId") && !json.isNull("timeoutLoserId")) json.optString("timeoutLoserId") else null
        )
    }
}
