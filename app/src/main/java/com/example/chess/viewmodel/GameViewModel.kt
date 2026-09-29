package com.example.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.SoundEffects
import com.example.chess.data.ChessDatabase
import com.example.chess.data.GameRecordEntity
import com.example.chess.data.UserProfileEntity
import com.example.chess.engine.ChessAi
import com.example.chess.engine.ChessBoard
import com.example.chess.engine.ChessEngine
import com.example.chess.model.BoardTheme
import com.example.chess.model.BotDifficulty
import com.example.chess.model.GameMode
import com.example.chess.model.GameStatus
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.PlayerInfo
import com.example.chess.model.Position
import com.example.chess.multiplayer.CloudRoom
import com.example.chess.multiplayer.FirebaseRealtimeManager
import com.example.chess.multiplayer.MultiplayerEvent
import com.example.chess.multiplayer.MultiplayerHub
import com.example.chess.multiplayer.MultiplayerRoom
import com.example.chess.multiplayer.MultiplayerSession
import com.example.chess.voice.LiveKitVoiceManager
import com.example.chess.voice.VoiceChatManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessDatabase.getDatabase(application)
    val soundEffects = SoundEffects(application)
    val voiceChatManager = VoiceChatManager(application)
    val liveKitManager = LiveKitVoiceManager(application)

    private val _board = MutableStateFlow(ChessBoard.initial())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _selectedPos = MutableStateFlow<Position?>(null)
    val selectedPos: StateFlow<Position?> = _selectedPos.asStateFlow()

    private val _legalMoves = MutableStateFlow<List<Position>>(emptyList())
    val legalMoves: StateFlow<List<Position>> = _legalMoves.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.ONLINE_MULTIPLAYER)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _botDifficulty = MutableStateFlow(BotDifficulty.TACTICAL)
    val botDifficulty: StateFlow<BotDifficulty> = _botDifficulty.asStateFlow()

    private val _boardTheme = MutableStateFlow(BoardTheme.SLATE)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    data class TimeControlPreset(val title: String, val category: String, val minutes: Int, val incrementSeconds: Int)

    val timeControlPresets = listOf(
        TimeControlPreset("1m", "Bullet", 1, 0),
        TimeControlPreset("3m", "Blitz", 3, 0),
        TimeControlPreset("5m", "Blitz", 5, 0),
        TimeControlPreset("10m", "Rapid", 10, 0),
        TimeControlPreset("15m", "Classical", 15, 10)
    )

    private val _selectedPreset = MutableStateFlow(timeControlPresets[3]) // Default 10m Rapid
    val selectedPreset: StateFlow<TimeControlPreset> = _selectedPreset.asStateFlow()

    private val _playerColor = MutableStateFlow(PieceColor.WHITE)
    val playerColor: StateFlow<PieceColor> = _playerColor.asStateFlow()

    private val _whiteClockMs = MutableStateFlow(10 * 60 * 1000L)
    val whiteClockMs: StateFlow<Long> = _whiteClockMs.asStateFlow()

    private val _blackClockMs = MutableStateFlow(10 * 60 * 1000L)
    val blackClockMs: StateFlow<Long> = _blackClockMs.asStateFlow()

    private val _pendingPromotion = MutableStateFlow<Pair<Position, Position>?>(null)
    val pendingPromotion: StateFlow<Pair<Position, Position>?> = _pendingPromotion.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfileEntity(
            guestId = "guest_${Random.nextInt(1000, 9999)}",
            displayName = "Guest_${Random.nextInt(1000, 9999)}",
            rating = 1200
        )
    )
    val userProfile: StateFlow<UserProfileEntity> = _userProfile.asStateFlow()

    private val _opponent = MutableStateFlow(
        PlayerInfo(id = "bot_1", name = "Tactical Adept", rating = 1500, isGuest = false)
    )
    val opponent: StateFlow<PlayerInfo> = _opponent.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _isWaitingForOpponent = MutableStateFlow(false)
    val isWaitingForOpponent: StateFlow<Boolean> = _isWaitingForOpponent.asStateFlow()

    private val _drawOfferedByOpponent = MutableStateFlow(false)
    val drawOfferedByOpponent: StateFlow<Boolean> = _drawOfferedByOpponent.asStateFlow()

    private val _recentGames = MutableStateFlow<List<GameRecordEntity>>(emptyList())
    val recentGames: StateFlow<List<GameRecordEntity>> = _recentGames.asStateFlow()

    private var multiplayerSession: MultiplayerSession? = null
    private var clockJob: Job? = null
    private var botJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var voiceSyncJob: Job? = null
    private var matchStartTime = System.currentTimeMillis()

    init {
        loadUserProfile()
        loadRecentGames()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            db.userProfileDao().getUserProfile().collect { profile ->
                if (profile != null) {
                    _userProfile.value = profile
                    _boardTheme.value = try {
                        BoardTheme.valueOf(profile.themeName)
                    } catch (_: Exception) {
                        BoardTheme.SLATE
                    }
                } else {
                    // Create initial guest profile
                    val guestId = "guest_${Random.nextInt(1000, 9999)}"
                    val newProfile = UserProfileEntity(
                        guestId = guestId,
                        displayName = "Guest_${Random.nextInt(1000, 9999)}",
                        rating = 1200,
                        avatarIndex = Random.nextInt(0, 4)
                    )
                    db.userProfileDao().insertOrUpdateProfile(newProfile)
                    _userProfile.value = newProfile
                }
            }
        }
    }

    private fun loadRecentGames() {
        viewModelScope.launch {
            db.gameDao().getAllGames().collect { list ->
                _recentGames.value = list
            }
        }
    }

    fun updateGuestDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = _userProfile.value.copy(displayName = trimmed)
        _userProfile.value = updated
        viewModelScope.launch {
            db.userProfileDao().insertOrUpdateProfile(updated)
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
        val updated = _userProfile.value.copy(themeName = theme.name)
        _userProfile.value = updated
        viewModelScope.launch {
            db.userProfileDao().insertOrUpdateProfile(updated)
        }
    }

    fun selectTimeControl(preset: TimeControlPreset) {
        _selectedPreset.value = preset
        _whiteClockMs.value = preset.minutes * 60 * 1000L
        _blackClockMs.value = preset.minutes * 60 * 1000L
    }

    fun startVsBot(difficulty: BotDifficulty) {
        _gameMode.value = GameMode.VS_BOT
        _botDifficulty.value = difficulty
        _playerColor.value = PieceColor.WHITE
        _opponent.value = PlayerInfo(
            id = "bot_${difficulty.name}",
            name = difficulty.title,
            rating = difficulty.rating,
            isGuest = false
        )
        resetGame(timeMinutes = _selectedPreset.value.minutes)
    }

    fun startPassAndPlay() {
        _gameMode.value = GameMode.PASS_AND_PLAY
        _playerColor.value = PieceColor.WHITE
        _opponent.value = PlayerInfo(
            id = "local_guest",
            name = "Player 2",
            rating = 1200,
            isGuest = true
        )
        resetGame(timeMinutes = _selectedPreset.value.minutes)
    }

    fun createOnlineRoom(timeMinutes: Int = _selectedPreset.value.minutes) {
        _gameMode.value = GameMode.ONLINE_MULTIPLAYER
        val code = "KNG-${Random.nextInt(100, 999)}"
        _roomCode.value = code
        _playerColor.value = PieceColor.WHITE
        _isWaitingForOpponent.value = true

        val hostPlayer = PlayerInfo(
            id = _userProfile.value.guestId,
            name = _userProfile.value.displayName,
            rating = _userProfile.value.rating,
            avatarIndex = _userProfile.value.avatarIndex,
            isGuest = true
        )

        val room = MultiplayerRoom(
            code = code,
            host = hostPlayer,
            timeControlMinutes = timeMinutes,
            hostColor = PieceColor.WHITE
        )

        MultiplayerHub.registerRoom(room)
        val session = MultiplayerSession(hostPlayer, code, isHost = true)
        session.attachRoom(room)
        multiplayerSession = session

        observeMultiplayerEvents(session)

        // Publish to Firebase Realtime Database
        viewModelScope.launch {
            FirebaseRealtimeManager.createRoom(
                code = code,
                host = hostPlayer,
                timeControlMinutes = timeMinutes,
                incrementSeconds = _selectedPreset.value.incrementSeconds,
                hostColor = PieceColor.WHITE
            )
        }
        startCloudRoomListener(code, isHost = true)
        startVoiceStateSync(code, isHost = true)

        resetGame(timeMinutes = timeMinutes)
    }

    fun joinOnlineRoom(code: String) {
        val cleanCode = code.trim().uppercase()
        val room = MultiplayerHub.getRoom(cleanCode)

        _gameMode.value = GameMode.ONLINE_MULTIPLAYER
        _roomCode.value = cleanCode
        _isWaitingForOpponent.value = false

        val guestPlayer = PlayerInfo(
            id = _userProfile.value.guestId,
            name = _userProfile.value.displayName,
            rating = _userProfile.value.rating,
            avatarIndex = _userProfile.value.avatarIndex,
            isGuest = true
        )

        val hostColor = room?.hostColor ?: PieceColor.WHITE
        _playerColor.value = hostColor.opposite()

        _opponent.value = room?.host ?: PlayerInfo(
            id = "host_p",
            name = "Host Player",
            rating = 1200,
            isGuest = true
        )

        val session = MultiplayerSession(guestPlayer, cleanCode, isHost = false)
        if (room != null) {
            session.joinAsGuest(room)
        }
        multiplayerSession = session

        observeMultiplayerEvents(session)

        // Join room in Firebase Realtime Database
        viewModelScope.launch {
            val cloudRoom = FirebaseRealtimeManager.joinRoom(cleanCode, guestPlayer)
            if (cloudRoom != null) {
                _opponent.value = PlayerInfo(
                    id = cloudRoom.hostId,
                    name = cloudRoom.hostName,
                    rating = cloudRoom.hostRating,
                    isGuest = true
                )
            }
        }
        startCloudRoomListener(cleanCode, isHost = false)
        startVoiceStateSync(cleanCode, isHost = false)

        resetGame(timeMinutes = room?.timeControlMinutes ?: _selectedPreset.value.minutes)
    }

    fun startQuickMatch() {
        _gameMode.value = GameMode.ONLINE_MULTIPLAYER
        val guestPlayer = PlayerInfo(
            id = _userProfile.value.guestId,
            name = _userProfile.value.displayName,
            rating = _userProfile.value.rating,
            avatarIndex = _userProfile.value.avatarIndex,
            isGuest = true
        )

        viewModelScope.launch {
            val (code, isHost) = FirebaseRealtimeManager.findOrCreateQuickMatch(
                guestPlayer,
                _selectedPreset.value.minutes
            )
            _roomCode.value = code
            _playerColor.value = if (isHost) PieceColor.WHITE else PieceColor.BLACK
            _isWaitingForOpponent.value = isHost

            if (isHost) {
                val room = MultiplayerRoom(
                    code = code,
                    host = guestPlayer,
                    timeControlMinutes = _selectedPreset.value.minutes,
                    hostColor = PieceColor.WHITE
                )
                MultiplayerHub.registerRoom(room)
                val session = MultiplayerSession(guestPlayer, code, isHost = true)
                session.attachRoom(room)
                multiplayerSession = session
                observeMultiplayerEvents(session)
                // Fallback automated opponent if no user joins within 3.5s
                session.simulateQuickMatchGuestArrival { opp ->
                    _opponent.value = opp
                    _isWaitingForOpponent.value = false
                }
            }

            startCloudRoomListener(code, isHost = isHost)
            startVoiceStateSync(code, isHost = isHost)
            resetGame(timeMinutes = _selectedPreset.value.minutes)
        }
    }

    private fun startCloudRoomListener(code: String, isHost: Boolean) {
        cloudSyncJob?.cancel()
        cloudSyncJob = FirebaseRealtimeManager.startListeningToRoom(viewModelScope, code) { cloudRoom ->
            // Update opponent if joined
            if (_isWaitingForOpponent.value && isHost && cloudRoom.guestId != null) {
                _opponent.value = PlayerInfo(
                    id = cloudRoom.guestId,
                    name = cloudRoom.guestName ?: "Online Player",
                    rating = cloudRoom.guestRating ?: 1200,
                    isGuest = true
                )
                _isWaitingForOpponent.value = false
            } else if (!isHost && cloudRoom.hostId.isNotEmpty()) {
                _opponent.value = PlayerInfo(
                    id = cloudRoom.hostId,
                    name = cloudRoom.hostName,
                    rating = cloudRoom.hostRating,
                    isGuest = true
                )
            }

            // Sync moves if a new move came from cloud opponent
            val cloudMove = cloudRoom.lastMove
            val localMoveCount = _board.value.moveHistory.size
            if (cloudMove != null && cloudRoom.moveCount > localMoveCount) {
                val expectedTurn = _board.value.turn
                if (expectedTurn != _playerColor.value) {
                    val from = Position(cloudMove.fromRow, cloudMove.fromCol)
                    val to = Position(cloudMove.toRow, cloudMove.toCol)
                    val prom = cloudMove.promotionType?.let {
                        try { PieceType.valueOf(it) } catch (_: Exception) { null }
                    }
                    executeOpponentMove(from, to, prom)
                }
            }

            // Sync draw offers & resignations
            if (cloudRoom.status == "DRAW_OFFERED" && cloudRoom.drawOfferedBy != _userProfile.value.guestId) {
                _drawOfferedByOpponent.value = true
            } else if (cloudRoom.status == "DRAW_AGREED" && _board.value.gameStatus != GameStatus.DRAW_AGREED) {
                _board.value = _board.value.copy(gameStatus = GameStatus.DRAW_AGREED)
                onGameEnded("DRAW")
            } else if (cloudRoom.status == "RESIGNED" && cloudRoom.resignedBy != _userProfile.value.guestId) {
                _board.value = _board.value.copy(
                    gameStatus = GameStatus.RESIGNATION,
                    winner = _playerColor.value
                )
                soundEffects.playVictorySound()
                onGameEnded("WIN")
            }

            // Sync live opponent voice state
            val oppSpeaking = if (isHost) cloudRoom.voiceGuestSpeaking else cloudRoom.voiceHostSpeaking
            if (oppSpeaking) {
                voiceChatManager.simulateOpponentSpeaking(true, durationMs = 800)
            }
        }
    }

    private fun startVoiceStateSync(code: String, isHost: Boolean) {
        voiceSyncJob?.cancel()
        voiceSyncJob = viewModelScope.launch {
            voiceChatManager.state.collect { vState ->
                val isSpeaking = vState.isRecording || vState.isPushToTalk
                val amp = if (isSpeaking) vState.userAmplitude else 0f
                FirebaseRealtimeManager.updateVoiceState(code, isHost, isSpeaking, amp)
            }
        }
    }

    private fun observeMultiplayerEvents(session: MultiplayerSession) {
        viewModelScope.launch {
            session.events.collect { event ->
                when (event) {
                    is MultiplayerEvent.OpponentJoined -> {
                        _opponent.value = event.opponent
                        _isWaitingForOpponent.value = false
                    }
                    is MultiplayerEvent.MoveReceived -> {
                        executeOpponentMove(event.from, event.to, event.promotionType)
                    }
                    is MultiplayerEvent.DrawOffered -> {
                        _drawOfferedByOpponent.value = true
                    }
                    is MultiplayerEvent.DrawAccepted -> {
                        _board.value = _board.value.copy(gameStatus = GameStatus.DRAW_AGREED)
                        onGameEnded("DRAW")
                    }
                    is MultiplayerEvent.OpponentResigned -> {
                        _board.value = _board.value.copy(
                            gameStatus = GameStatus.RESIGNATION,
                            winner = _playerColor.value
                        )
                        soundEffects.playVictorySound()
                        onGameEnded("WIN")
                    }
                    is MultiplayerEvent.RematchRequested -> {
                        resetGame()
                    }
                    is MultiplayerEvent.OpponentDisconnected -> {
                        // opponent disconnected notification
                    }
                }
            }
        }
    }

    fun resetGame(timeMinutes: Int = _selectedPreset.value.minutes) {
        _board.value = ChessBoard.initial()
        _selectedPos.value = null
        _legalMoves.value = emptyList()
        _pendingPromotion.value = null
        _whiteClockMs.value = timeMinutes * 60 * 1000L
        _blackClockMs.value = timeMinutes * 60 * 1000L
        _drawOfferedByOpponent.value = false
        matchStartTime = System.currentTimeMillis()
        startClock()
    }

    fun onSquareSelected(pos: Position) {
        val currBoard = _board.value
        if (currBoard.gameStatus != GameStatus.ACTIVE && currBoard.gameStatus != GameStatus.CHECK) return

        // In vs Bot or Online, player can only move pieces of their assigned color
        if (_gameMode.value != GameMode.PASS_AND_PLAY && currBoard.turn != _playerColor.value) {
            return
        }

        val selected = _selectedPos.value

        if (selected == null) {
            // Select piece if it belongs to current player
            val piece = currBoard.pieceAt(pos)
            if (piece != null && piece.color == currBoard.turn) {
                _selectedPos.value = pos
                _legalMoves.value = ChessEngine.getLegalMoves(currBoard, pos)
            }
        } else {
            if (pos in _legalMoves.value) {
                // Check if promotion
                val piece = currBoard.pieceAt(selected)
                val isPawnPromotion = piece?.type == PieceType.PAWN && (pos.row == 0 || pos.row == 7)

                if (isPawnPromotion) {
                    _pendingPromotion.value = selected to pos
                } else {
                    executeMove(selected, pos, null)
                }
            } else {
                // If clicked on another own piece, switch selection
                val piece = currBoard.pieceAt(pos)
                if (piece != null && piece.color == currBoard.turn) {
                    _selectedPos.value = pos
                    _legalMoves.value = ChessEngine.getLegalMoves(currBoard, pos)
                } else {
                    _selectedPos.value = null
                    _legalMoves.value = emptyList()
                }
            }
        }
    }

    fun onSelectPromotionPiece(pieceType: PieceType) {
        val pending = _pendingPromotion.value ?: return
        _pendingPromotion.value = null
        executeMove(pending.first, pending.second, pieceType)
    }

    fun cancelPromotion() {
        _pendingPromotion.value = null
        _selectedPos.value = null
        _legalMoves.value = emptyList()
    }

    private fun executeMove(from: Position, to: Position, promotion: PieceType?) {
        val currBoard = _board.value
        val movingPiece = currBoard.pieceAt(from) ?: return
        val isCapture = currBoard.pieceAt(to) != null || (movingPiece.type == PieceType.PAWN && currBoard.enPassantTarget == to)

        val newBoard = ChessEngine.makeMove(currBoard, from, to, promotion)
        _board.value = newBoard
        _selectedPos.value = null
        _legalMoves.value = emptyList()

        // Add time increment if applicable
        if (_selectedPreset.value.incrementSeconds > 0) {
            val incMs = _selectedPreset.value.incrementSeconds * 1000L
            if (movingPiece.color == PieceColor.WHITE) {
                _whiteClockMs.value += incMs
            } else {
                _blackClockMs.value += incMs
            }
        }

        // Sound effects
        if (newBoard.gameStatus == GameStatus.CHECKMATE) {
            if (newBoard.winner == _playerColor.value) {
                soundEffects.playVictorySound()
                onGameEnded("WIN")
            } else {
                soundEffects.playDefeatSound()
                onGameEnded("LOSS")
            }
        } else if (newBoard.gameStatus == GameStatus.STALEMATE || newBoard.gameStatus == GameStatus.DRAW_AGREED) {
            soundEffects.playMoveSound()
            onGameEnded("DRAW")
        } else if (newBoard.gameStatus == GameStatus.CHECK) {
            soundEffects.playCheckSound()
        } else if (isCapture) {
            soundEffects.playCaptureSound()
        } else {
            soundEffects.playMoveSound()
        }

        // Multiplayer move broadcast
        if (_gameMode.value == GameMode.ONLINE_MULTIPLAYER) {
            val clock = if (_playerColor.value == PieceColor.WHITE) _whiteClockMs.value else _blackClockMs.value
            multiplayerSession?.sendMove(from, to, promotion, clock)

            _roomCode.value?.let { code ->
                viewModelScope.launch {
                    FirebaseRealtimeManager.postMove(
                        code = code,
                        from = from,
                        to = to,
                        notation = newBoard.lastMove?.notation ?: "",
                        promotionType = promotion,
                        moveIndex = newBoard.moveHistory.size,
                        whiteClockMs = _whiteClockMs.value,
                        blackClockMs = _blackClockMs.value
                    )
                }
            }
        }

        // Bot response
        if (_gameMode.value == GameMode.VS_BOT && newBoard.turn != _playerColor.value &&
            (newBoard.gameStatus == GameStatus.ACTIVE || newBoard.gameStatus == GameStatus.CHECK)
        ) {
            triggerBotMove()
        }
    }

    private fun executeOpponentMove(from: Position, to: Position, promotion: PieceType?) {
        val currBoard = _board.value
        val movingPiece = currBoard.pieceAt(from) ?: return
        val isCapture = currBoard.pieceAt(to) != null || (movingPiece.type == PieceType.PAWN && currBoard.enPassantTarget == to)

        val newBoard = ChessEngine.makeMove(currBoard, from, to, promotion)
        _board.value = newBoard

        // Add time increment for opponent if applicable
        if (_selectedPreset.value.incrementSeconds > 0) {
            val incMs = _selectedPreset.value.incrementSeconds * 1000L
            if (movingPiece.color == PieceColor.WHITE) {
                _whiteClockMs.value += incMs
            } else {
                _blackClockMs.value += incMs
            }
        }

        if (newBoard.gameStatus == GameStatus.CHECKMATE) {
            if (newBoard.winner == _playerColor.value) {
                soundEffects.playVictorySound()
                onGameEnded("WIN")
            } else {
                soundEffects.playDefeatSound()
                onGameEnded("LOSS")
            }
        } else if (newBoard.gameStatus == GameStatus.CHECK) {
            soundEffects.playCheckSound()
        } else if (isCapture) {
            soundEffects.playCaptureSound()
        } else {
            soundEffects.playMoveSound()
        }
    }

    private fun triggerBotMove() {
        botJob?.cancel()
        botJob = viewModelScope.launch {
            // Realistic think delay
            delay(400 + Random.nextLong(600))
            if (!isActive) return@launch

            val currBoard = _board.value
            val bestMove = ChessAi.getBestMove(currBoard, currBoard.turn, _botDifficulty.value)
            if (bestMove != null) {
                executeMove(bestMove.first, bestMove.second, PieceType.QUEEN)
            }
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var lastTickTime = System.currentTimeMillis()
            var lastBeepSecond = -1L

            while (isActive) {
                delay(50) // 50ms for smooth sub-second countdown
                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now

                val currBoard = _board.value
                if (currBoard.gameStatus != GameStatus.ACTIVE && currBoard.gameStatus != GameStatus.CHECK) {
                    break
                }

                if (currBoard.turn == PieceColor.WHITE) {
                    val remaining = (_whiteClockMs.value - delta).coerceAtLeast(0L)
                    _whiteClockMs.value = remaining

                    // Audible low-time tick warning in last 10 seconds
                    val secondsRemaining = remaining / 1000
                    if (secondsRemaining in 1..10 && secondsRemaining != lastBeepSecond) {
                        lastBeepSecond = secondsRemaining
                        soundEffects.playLowTimeTick()
                    }

                    if (remaining <= 0L) {
                        _whiteClockMs.value = 0L
                        _board.value = currBoard.copy(
                            gameStatus = GameStatus.TIMEOUT,
                            winner = PieceColor.BLACK
                        )
                        soundEffects.playTimeoutSound()
                        if (_playerColor.value == PieceColor.BLACK) {
                            soundEffects.playVictorySound()
                            onGameEnded("WIN")
                        } else {
                            soundEffects.playDefeatSound()
                            onGameEnded("LOSS")
                        }
                        break
                    }
                } else {
                    val remaining = (_blackClockMs.value - delta).coerceAtLeast(0L)
                    _blackClockMs.value = remaining

                    val secondsRemaining = remaining / 1000
                    if (secondsRemaining in 1..10 && secondsRemaining != lastBeepSecond) {
                        lastBeepSecond = secondsRemaining
                        soundEffects.playLowTimeTick()
                    }

                    if (remaining <= 0L) {
                        _blackClockMs.value = 0L
                        _board.value = currBoard.copy(
                            gameStatus = GameStatus.TIMEOUT,
                            winner = PieceColor.WHITE
                        )
                        soundEffects.playTimeoutSound()
                        if (_playerColor.value == PieceColor.WHITE) {
                            soundEffects.playVictorySound()
                            onGameEnded("WIN")
                        } else {
                            soundEffects.playDefeatSound()
                            onGameEnded("LOSS")
                        }
                        break
                    }
                }
            }
        }
    }

    fun resign() {
        val currBoard = _board.value
        val winner = _playerColor.value.opposite()
        _board.value = currBoard.copy(
            gameStatus = GameStatus.RESIGNATION,
            winner = winner
        )
        soundEffects.playDefeatSound()
        multiplayerSession?.sendResignation()
        _roomCode.value?.let { code ->
            viewModelScope.launch {
                FirebaseRealtimeManager.sendGameAction(code, "RESIGNED", _userProfile.value.guestId)
            }
        }
        onGameEnded("LOSS")
    }

    fun offerDraw() {
        if (_gameMode.value == GameMode.ONLINE_MULTIPLAYER) {
            multiplayerSession?.offerDraw()
            _roomCode.value?.let { code ->
                viewModelScope.launch {
                    FirebaseRealtimeManager.sendGameAction(code, "DRAW_OFFERED", _userProfile.value.guestId)
                }
            }
        } else if (_gameMode.value == GameMode.VS_BOT) {
            // Bot evaluates draw offer: accepts if material is equal and moves > 25
            if (_board.value.moveHistory.size > 25) {
                _board.value = _board.value.copy(gameStatus = GameStatus.DRAW_AGREED)
                onGameEnded("DRAW")
            }
        }
    }

    fun acceptDrawOffer() {
        _drawOfferedByOpponent.value = false
        _board.value = _board.value.copy(gameStatus = GameStatus.DRAW_AGREED)
        multiplayerSession?.acceptDraw()
        _roomCode.value?.let { code ->
            viewModelScope.launch {
                FirebaseRealtimeManager.sendGameAction(code, "DRAW_AGREED", _userProfile.value.guestId)
            }
        }
        onGameEnded("DRAW")
    }

    fun declineDrawOffer() {
        _drawOfferedByOpponent.value = false
    }

    fun flipBoardPerspective() {
        _playerColor.value = _playerColor.value.opposite()
    }

    private fun onGameEnded(result: String) {
        clockJob?.cancel()
        val durationSeconds = ((System.currentTimeMillis() - matchStartTime) / 1000).coerceAtLeast(1)
        val pgn = _board.value.moveHistory.joinToString(" ") { it.notation }

        // Update Elo and profile
        val profile = _userProfile.value
        val ratingDelta = when (result) {
            "WIN" -> 16
            "LOSS" -> -12
            else -> 0
        }
        val newRating = (profile.rating + ratingDelta).coerceAtLeast(400)
        val newWins = if (result == "WIN") profile.wins + 1 else profile.wins
        val newLosses = if (result == "LOSS") profile.losses + 1 else profile.losses
        val newDraws = if (result == "DRAW") profile.draws + 1 else profile.draws

        val updatedProfile = profile.copy(
            rating = newRating,
            wins = newWins,
            losses = newLosses,
            draws = newDraws
        )
        _userProfile.value = updatedProfile

        viewModelScope.launch {
            db.userProfileDao().insertOrUpdateProfile(updatedProfile)
            db.gameDao().insertGame(
                GameRecordEntity(
                    opponentName = _opponent.value.name,
                    gameMode = _gameMode.value.name,
                    playerColor = _playerColor.value.name,
                    result = result,
                    movesCount = _board.value.moveHistory.size,
                    pgn = pgn,
                    durationSeconds = durationSeconds
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        botJob?.cancel()
        cloudSyncJob?.cancel()
        voiceSyncJob?.cancel()
        multiplayerSession?.close()
        voiceChatManager.release()
    }
}
