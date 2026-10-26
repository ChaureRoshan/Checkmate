package com.example.chess.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.SoundEffects
import com.example.chess.auth.AuthManager
import com.example.chess.auth.GoogleAccount
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
import com.example.chess.multiplayer.Challenge
import com.example.chess.multiplayer.CloudRoom
import com.example.chess.multiplayer.FirebaseRealtimeManager
import com.example.chess.multiplayer.MultiplayerEvent
import com.example.chess.multiplayer.MultiplayerHub
import com.example.chess.multiplayer.MultiplayerRoom
import com.example.chess.multiplayer.MultiplayerSession
import com.example.chess.multiplayer.OnlinePlayer
import com.example.chess.voice.LiveKitVoiceManager
import com.example.chess.voice.VoiceChatManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessDatabase.getDatabase(application)
    val soundEffects = SoundEffects(application)
    val liveKitManager = LiveKitVoiceManager(application)
    val authManager = AuthManager(application)
    val voiceChatManager = VoiceChatManager(application, liveKitManager)

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
        TimeControlPreset("3|2", "Blitz", 3, 2)
    )

    private val _selectedPreset = MutableStateFlow(timeControlPresets[3]) // Default 10m Rapid
    val selectedPreset: StateFlow<TimeControlPreset> = _selectedPreset.asStateFlow()

    private val _boardFlipped = MutableStateFlow(false)
    val boardFlipped: StateFlow<Boolean> = _boardFlipped.asStateFlow()

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

    // ---- Google account (optional). Guests keep working without it. ----
    private val _account = MutableStateFlow<GoogleAccount?>(authManager.savedAccount())
    val account: StateFlow<GoogleAccount?> = _account.asStateFlow()

    // Every person can change the name only 2 times
    private val maxNameChanges = 2
    private val _nameChangesLeft =
        MutableStateFlow((maxNameChanges - authManager.nameChangesUsed).coerceAtLeast(0))
    val nameChangesLeft: StateFlow<Int> = _nameChangesLeft.asStateFlow()

    // ---- Online presence / direct challenges (no account needed) ----
    private val _onlinePlayers = MutableStateFlow<List<OnlinePlayer>>(emptyList())
    val onlinePlayers: StateFlow<List<OnlinePlayer>> = _onlinePlayers.asStateFlow()

    private val _incomingChallenge = MutableStateFlow<Challenge?>(null)
    val incomingChallenge: StateFlow<Challenge?> = _incomingChallenge.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _exitToLobby = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val exitToLobby: SharedFlow<Unit> = _exitToLobby.asSharedFlow()

    private var presenceJob: Job? = null
    private var outgoingChallengeJob: Job? = null
    private var lastPresenceId: String? = null
    private val handledChallenges = mutableSetOf<String>()
    private var clockSyncedFromCloud = false
    private var activeIncrementSeconds: Int? = null
    private var timeoutHandled = false

    /** Online games use the increment the host created the room with, so both players get the same clock. */
    private fun currentIncrementSeconds(): Int =
        if (_gameMode.value == GameMode.ONLINE_MULTIPLAYER)
            activeIncrementSeconds ?: _selectedPreset.value.incrementSeconds
        else
            _selectedPreset.value.incrementSeconds

    private var multiplayerSession: MultiplayerSession? = null
    private var clockJob: Job? = null
    private var botJob: Job? = null
    private var cloudSyncJob: Job? = null
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
        val trimmed = newName.trim().take(16)
        if (trimmed.isEmpty() || trimmed == _userProfile.value.displayName) return
        if (_nameChangesLeft.value <= 0) {
            _notice.value = "Naam badalne ke 2 chance khatam ho gaye."
            return
        }
        val used = authManager.nameChangesUsed + 1
        authManager.nameChangesUsed = used
        _nameChangesLeft.value = (maxNameChanges - used).coerceAtLeast(0)
        val updated = _userProfile.value.copy(displayName = trimmed)
        _userProfile.value = updated
        _notice.value = if (_nameChangesLeft.value > 0) "Naam badal gaya. 1 chance aur bacha hai."
        else "Naam badal gaya. Ab naam nahi badal sakte."
        viewModelScope.launch {
            db.userProfileDao().insertOrUpdateProfile(updated)
            _account.value?.let { FirebaseRealtimeManager.putCloudProfile(it.uid, updated, used) }
        }
    }

    fun markWelcomeDone() {
        authManager.welcomeDone = true
    }

    /** After "Continue with Google": load the saved cloud profile, or upload what this phone already has. */
    fun onGoogleSignedIn(account: GoogleAccount) {
        authManager.saveAccount(account)
        _account.value = account
        viewModelScope.launch {
            val local = _userProfile.value
            val cloud = FirebaseRealtimeManager.getCloudProfile(account.uid)
            val merged = if (cloud != null) {
                // Existing account: bring back rating, stats, name and used name-changes
                authManager.nameChangesUsed = cloud.nameChanges
                local.copy(
                    displayName = cloud.profile.displayName,
                    rating = cloud.profile.rating,
                    wins = cloud.profile.wins,
                    losses = cloud.profile.losses,
                    draws = cloud.profile.draws
                )
            } else {
                // Brand new account: it starts with the Google name and 2 fresh name changes
                authManager.nameChangesUsed = 0
                local.copy(displayName = account.name)
            }
            _nameChangesLeft.value = (maxNameChanges - authManager.nameChangesUsed).coerceAtLeast(0)
            _userProfile.value = merged
            db.userProfileDao().insertOrUpdateProfile(merged)
            FirebaseRealtimeManager.putCloudProfile(account.uid, merged, authManager.nameChangesUsed)
        }
    }

    /** Back to a fresh guest on this phone. The Google account's data stays in the cloud. */
    fun signOut() {
        authManager.clearAccount()
        _account.value = null
        viewModelScope.launch {
            val fresh = _userProfile.value.copy(
                displayName = "Guest_${Random.nextInt(1000, 9999)}",
                rating = 1200,
                wins = 0,
                losses = 0,
                draws = 0
            )
            _userProfile.value = fresh
            db.userProfileDao().insertOrUpdateProfile(fresh)
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

    fun createOnlineRoom(
        timeMinutes: Int = _selectedPreset.value.minutes,
        challengeTargetId: String? = null
    ) {
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
            val created = FirebaseRealtimeManager.createRoom(
                code = code,
                host = hostPlayer,
                timeControlMinutes = timeMinutes,
                incrementSeconds = _selectedPreset.value.incrementSeconds,
                hostColor = PieceColor.WHITE
            )
            // Direct request: only send it once the room really exists
            if (challengeTargetId != null) {
                if (created && FirebaseRealtimeManager.sendChallenge(challengeTargetId, hostPlayer, code, timeMinutes)) {
                    watchOutgoingChallenge(challengeTargetId, hostPlayer.id, code)
                } else {
                    _notice.value = "Request bhej nahi paya. Internet check karo."
                    leaveOnlineRoom()
                    _exitToLobby.tryEmit(Unit)
                }
            }
        }
        clockSyncedFromCloud = true // host decides the time control
        timeoutHandled = false
        startCloudRoomListener(code, isHost = true)
        connectVoice(code)

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
        clockSyncedFromCloud = false
        timeoutHandled = false
        startCloudRoomListener(cleanCode, isHost = false)
        connectVoice(cleanCode)

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
                _selectedPreset.value.minutes,
                _selectedPreset.value.incrementSeconds
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
                // Wait for a real player to join (the old fake "simulated opponent" was removed)
            }

            clockSyncedFromCloud = isHost
            timeoutHandled = false
            startCloudRoomListener(code, isHost = isHost)
            connectVoice(code)
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
                    // The mover's own phone knows his exact time (incl. increment): use it
                    if (_playerColor.value == PieceColor.WHITE) {
                        _blackClockMs.value = cloudRoom.blackClockMs
                    } else {
                        _whiteClockMs.value = cloudRoom.whiteClockMs
                    }
                }
            }

            // The other phone says someone's time is over -> end the game here too
            val timeoutLoser = cloudRoom.timeoutLoserId
            val boardState = _board.value.gameStatus
            if (timeoutLoser != null && !timeoutHandled &&
                (boardState == GameStatus.ACTIVE || boardState == GameStatus.CHECK)
            ) {
                timeoutHandled = true
                val iLost = timeoutLoser == _userProfile.value.guestId
                val loserColor = if (iLost) _playerColor.value else _playerColor.value.opposite()
                if (loserColor == PieceColor.WHITE) _whiteClockMs.value = 0L else _blackClockMs.value = 0L
                _board.value = _board.value.copy(
                    gameStatus = GameStatus.TIMEOUT,
                    winner = loserColor.opposite()
                )
                soundEffects.playTimeoutSound()
                if (iLost) {
                    soundEffects.playDefeatSound()
                    onGameEnded("LOSS")
                } else {
                    soundEffects.playVictorySound()
                    onGameEnded("WIN")
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

            activeIncrementSeconds = cloudRoom.incrementSeconds

            // Guest uses the host's time control (before any move is played)
            if (!isHost && !clockSyncedFromCloud && cloudRoom.moveCount == 0) {
                clockSyncedFromCloud = true
                resetGame(timeMinutes = cloudRoom.timeControlMinutes)
            }
        }
    }

    // ------------------------------------------------------------------
    // Voice
    // ------------------------------------------------------------------
    private fun connectVoice(code: String) {
        val me = _userProfile.value
        voiceChatManager.connect(code, me.guestId, me.displayName)
    }

    // ------------------------------------------------------------------
    // Presence + challenges
    // ------------------------------------------------------------------

    /** Call with true from onStart and false from onStop so we only show "online" while the app is open. */
    fun setAppVisible(visible: Boolean) {
        presenceJob?.cancel()
        presenceJob = null
        if (!visible) {
            val id = lastPresenceId
            lastPresenceId = null
            _onlinePlayers.value = emptyList()
            _incomingChallenge.value = null
            if (id != null) viewModelScope.launch { FirebaseRealtimeManager.removePresence(id) }
            return
        }
        presenceJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                try {
                    val me = _userProfile.value
                    val previousId = lastPresenceId
                    if (previousId != null && previousId != me.guestId) {
                        FirebaseRealtimeManager.removePresence(previousId)
                    }
                    lastPresenceId = me.guestId
                    val inGame = _roomCode.value != null

                    if (tick % 2 == 0) {
                        FirebaseRealtimeManager.updatePresence(
                            PlayerInfo(
                                id = me.guestId,
                                name = me.displayName,
                                rating = me.rating,
                                avatarIndex = me.avatarIndex
                            ),
                            inGame = inGame
                        )
                    }
                    _onlinePlayers.value = FirebaseRealtimeManager.getOnlinePlayers(me.guestId)

                    if (inGame) {
                        _incomingChallenge.value = null
                    } else {
                        _incomingChallenge.value = FirebaseRealtimeManager
                            .getIncomingChallenges(me.guestId)
                            .firstOrNull { !handledChallenges.contains(challengeKey(it)) }
                    }
                } catch (_: Exception) {
                    // transient network problem; try again on the next tick
                }
                tick++
                delay(4000)
            }
        }
    }

    private fun challengeKey(c: Challenge) = "${c.fromId}:${c.createdAt}"

    /** Send a direct play request to an online player and wait in a fresh room. */
    fun challengePlayer(target: OnlinePlayer) {
        if (target.inGame) return
        createOnlineRoom(
            timeMinutes = _selectedPreset.value.minutes,
            challengeTargetId = target.id
        )
    }

    private fun watchOutgoingChallenge(targetId: String, myId: String, code: String) {
        outgoingChallengeJob?.cancel()
        outgoingChallengeJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            while (isActive && _roomCode.value == code && _isWaitingForOpponent.value) {
                val status = FirebaseRealtimeManager.getChallengeStatus(targetId, myId)
                val timedOut = System.currentTimeMillis() - startedAt > 60_000L
                if (status == "DECLINED" || timedOut || status == null) {
                    _notice.value = if (status == "DECLINED") "Request decline kar di gayi."
                    else "Koi jawab nahi aaya."
                    FirebaseRealtimeManager.deleteChallenge(targetId, myId)
                    leaveOnlineRoom()
                    _exitToLobby.tryEmit(Unit)
                    return@launch
                }
                if (status == "ACCEPTED") break // the guest joins the room; the room listener takes over
                delay(2000)
            }
            FirebaseRealtimeManager.deleteChallenge(targetId, myId)
        }
    }

    fun acceptChallenge(challenge: Challenge) {
        handledChallenges.add(challengeKey(challenge))
        _incomingChallenge.value = null
        val me = _userProfile.value.guestId
        viewModelScope.launch {
            FirebaseRealtimeManager.setChallengeStatus(me, challenge.fromId, "ACCEPTED")
        }
        joinOnlineRoom(challenge.roomCode)
    }

    fun declineChallenge(challenge: Challenge) {
        handledChallenges.add(challengeKey(challenge))
        _incomingChallenge.value = null
        val me = _userProfile.value.guestId
        viewModelScope.launch {
            FirebaseRealtimeManager.setChallengeStatus(me, challenge.fromId, "DECLINED")
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** Stops all online syncing + voice when the player goes back to the lobby. Safe to call anytime. */
    fun leaveOnlineRoom() {
        val code = _roomCode.value ?: return
        val wasWaitingHost = _isWaitingForOpponent.value
        cloudSyncJob?.cancel()
        cloudSyncJob = null
        outgoingChallengeJob?.cancel()
        outgoingChallengeJob = null
        voiceChatManager.disconnect()
        multiplayerSession?.close()
        multiplayerSession = null
        _roomCode.value = null
        _isWaitingForOpponent.value = false
        activeIncrementSeconds = null
        if (wasWaitingHost) {
            viewModelScope.launch { FirebaseRealtimeManager.deleteRoom(code) }
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
        _boardFlipped.value = false
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
        // Game over (time out, resign, mate...): nothing can be played any more
        if (currBoard.gameStatus != GameStatus.ACTIVE && currBoard.gameStatus != GameStatus.CHECK) return
        val movingPiece = currBoard.pieceAt(from) ?: return
        val isCapture = currBoard.pieceAt(to) != null || (movingPiece.type == PieceType.PAWN && currBoard.enPassantTarget == to)

        val newBoard = ChessEngine.makeMove(currBoard, from, to, promotion)
        _board.value = newBoard
        _selectedPos.value = null
        _legalMoves.value = emptyList()

        // Add time increment if applicable
        if (currentIncrementSeconds() > 0) {
            val incMs = currentIncrementSeconds() * 1000L
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
        if (currBoard.gameStatus != GameStatus.ACTIVE && currBoard.gameStatus != GameStatus.CHECK) return
        val movingPiece = currBoard.pieceAt(from) ?: return
        val isCapture = currBoard.pieceAt(to) != null || (movingPiece.type == PieceType.PAWN && currBoard.enPassantTarget == to)

        val newBoard = ChessEngine.makeMove(currBoard, from, to, promotion)
        _board.value = newBoard

        // Add time increment for opponent if applicable
        if (currentIncrementSeconds() > 0) {
            val incMs = currentIncrementSeconds() * 1000L
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

    private fun notifyTimeoutOnline(loserColor: PieceColor) {
        if (_gameMode.value != GameMode.ONLINE_MULTIPLAYER) return
        val code = _roomCode.value ?: return
        timeoutHandled = true
        val loserId = if (loserColor == _playerColor.value) _userProfile.value.guestId else _opponent.value.id
        viewModelScope.launch { FirebaseRealtimeManager.sendTimeout(code, loserId) }
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
                        notifyTimeoutOnline(PieceColor.WHITE)
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
                        notifyTimeoutOnline(PieceColor.BLACK)
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

    /** Only turns the board around for viewing. Which side you play never changes. */
    fun flipBoardPerspective() {
        _boardFlipped.value = !_boardFlipped.value
    }

    private fun onGameEnded(result: String) {
        clockJob?.cancel()
        botJob?.cancel()
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
            _account.value?.let {
                FirebaseRealtimeManager.putCloudProfile(it.uid, updatedProfile, authManager.nameChangesUsed)
            }
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
        presenceJob?.cancel()
        outgoingChallengeJob?.cancel()
        lastPresenceId?.let { id ->
            // viewModelScope is already cancelled here, so do a best-effort fire-and-forget delete
            Thread { kotlinx.coroutines.runBlocking { FirebaseRealtimeManager.removePresence(id) } }.start()
        }
        multiplayerSession?.close()
        voiceChatManager.release()
    }
}
