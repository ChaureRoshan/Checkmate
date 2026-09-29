package com.example.chess.multiplayer

import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.PlayerInfo
import com.example.chess.model.Position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class MultiplayerRoom(
    val code: String,
    val host: PlayerInfo,
    var guest: PlayerInfo? = null,
    val timeControlMinutes: Int = 10,
    val hostColor: PieceColor = PieceColor.WHITE,
    var isStarted: Boolean = false
)

sealed interface MultiplayerEvent {
    data class OpponentJoined(val opponent: PlayerInfo) : MultiplayerEvent
    data class MoveReceived(
        val from: Position,
        val to: Position,
        val promotionType: PieceType?,
        val opponentClockMs: Long
    ) : MultiplayerEvent
    data class DrawOffered(val fromPlayerName: String) : MultiplayerEvent
    data class DrawAccepted(val byPlayerName: String) : MultiplayerEvent
    data class OpponentResigned(val opponentName: String) : MultiplayerEvent
    object RematchRequested : MultiplayerEvent
    object OpponentDisconnected : MultiplayerEvent
}

object MultiplayerHub {
    // In-memory global rooms registry for room codes
    private val activeRooms = ConcurrentHashMap<String, MultiplayerRoom>()
    private val roomEventBuses = ConcurrentHashMap<String, MutableSharedFlow<MultiplayerEvent>>()

    fun registerRoom(room: MultiplayerRoom): MutableSharedFlow<MultiplayerEvent> {
        activeRooms[room.code] = room
        val bus = MutableSharedFlow<MultiplayerEvent>(extraBufferCapacity = 64)
        roomEventBuses[room.code] = bus
        return bus
    }

    fun getRoom(code: String): MultiplayerRoom? = activeRooms[code.uppercase().trim()]

    fun getEventBus(code: String): MutableSharedFlow<MultiplayerEvent>? =
        roomEventBuses[code.uppercase().trim()]

    fun removeRoom(code: String) {
        val key = code.uppercase().trim()
        activeRooms.remove(key)
        roomEventBuses.remove(key)
    }
}

class MultiplayerSession(
    private val userPlayer: PlayerInfo,
    private val roomCode: String,
    val isHost: Boolean
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var simulatedOpponentJob: Job? = null

    private val _events = MutableSharedFlow<MultiplayerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<MultiplayerEvent> = _events.asSharedFlow()

    private val _currentRoom = MutableStateFlow<MultiplayerRoom?>(null)
    val currentRoom: StateFlow<MultiplayerRoom?> = _currentRoom.asStateFlow()

    init {
        val bus = MultiplayerHub.getEventBus(roomCode)
        if (bus != null) {
            scope.launch {
                bus.collect { event ->
                    _events.emit(event)
                }
            }
        }
    }

    fun attachRoom(room: MultiplayerRoom) {
        _currentRoom.value = room
    }

    fun joinAsGuest(room: MultiplayerRoom) {
        room.guest = userPlayer
        room.isStarted = true
        _currentRoom.value = room
        val bus = MultiplayerHub.getEventBus(room.code)
        scope.launch {
            bus?.emit(MultiplayerEvent.OpponentJoined(userPlayer))
        }
    }

    fun sendMove(from: Position, to: Position, promotionType: PieceType?, myClockMs: Long) {
        val bus = MultiplayerHub.getEventBus(roomCode)
        scope.launch {
            bus?.emit(MultiplayerEvent.MoveReceived(from, to, promotionType, myClockMs))
        }
    }

    fun sendResignation() {
        val bus = MultiplayerHub.getEventBus(roomCode)
        scope.launch {
            bus?.emit(MultiplayerEvent.OpponentResigned(userPlayer.name))
        }
    }

    fun offerDraw() {
        val bus = MultiplayerHub.getEventBus(roomCode)
        scope.launch {
            bus?.emit(MultiplayerEvent.DrawOffered(userPlayer.name))
        }
    }

    fun acceptDraw() {
        val bus = MultiplayerHub.getEventBus(roomCode)
        scope.launch {
            bus?.emit(MultiplayerEvent.DrawAccepted(userPlayer.name))
        }
    }

    fun requestRematch() {
        val bus = MultiplayerHub.getEventBus(roomCode)
        scope.launch {
            bus?.emit(MultiplayerEvent.RematchRequested)
        }
    }

    fun simulateQuickMatchGuestArrival(onOpponentJoined: (PlayerInfo) -> Unit) {
        simulatedOpponentJob?.cancel()
        simulatedOpponentJob = scope.launch {
            delay(1200 + Random.nextLong(1500))
            if (!isActive) return@launch
            val guestNames = listOf(
                "NimzoKnight_94", "ValkyriePawn", "DeepBlue_Guest",
                "TacticalRook", "FischerFan", "QuietBishop", "BlitzKing_82"
            )
            val randomOpponent = PlayerInfo(
                id = "guest_sim_${Random.nextInt(1000, 9999)}",
                name = guestNames.random(),
                rating = 1200 + Random.nextInt(-150, 220),
                avatarIndex = Random.nextInt(0, 4),
                isGuest = true
            )
            _currentRoom.value?.guest = randomOpponent
            _currentRoom.value?.isStarted = true
            onOpponentJoined(randomOpponent)
            _events.emit(MultiplayerEvent.OpponentJoined(randomOpponent))
        }
    }

    fun close() {
        simulatedOpponentJob?.cancel()
        if (isHost) {
            MultiplayerHub.removeRoom(roomCode)
        }
    }
}
