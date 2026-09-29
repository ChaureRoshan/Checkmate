package com.example.chess.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.GameMode
import com.example.chess.model.GameStatus
import com.example.chess.model.PieceColor
import com.example.chess.model.PlayerInfo
import com.example.chess.ui.components.ChessBoardView
import com.example.chess.ui.components.PawnPromotionDialog
import com.example.chess.ui.components.PlayerCardView
import com.example.chess.ui.components.VoiceChatBar
import com.example.chess.viewmodel.GameViewModel
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val board by viewModel.board.collectAsState()
    val selectedPos by viewModel.selectedPos.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val theme by viewModel.boardTheme.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val whiteClockMs by viewModel.whiteClockMs.collectAsState()
    val blackClockMs by viewModel.blackClockMs.collectAsState()
    val pendingPromotion by viewModel.pendingPromotion.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val opponent by viewModel.opponent.collectAsState()
    val roomCode by viewModel.roomCode.collectAsState()
    val isWaitingForOpponent by viewModel.isWaitingForOpponent.collectAsState()
    val drawOfferedByOpponent by viewModel.drawOfferedByOpponent.collectAsState()
    val voiceState by viewModel.voiceChatManager.state.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()

    var showResignConfirm by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showMoveHistoryModal by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.voiceChatManager.updatePermissionStatus()
    }

    BackHandler {
        if (board.gameStatus == GameStatus.ACTIVE || board.gameStatus == GameStatus.CHECK) {
            showExitConfirm = true
        } else {
            onNavigateBack()
        }
    }

    val opponentColor = playerColor.opposite()
    val isPlayerTurn = board.turn == playerColor
    val isOpponentTurn = board.turn == opponentColor

    // Material difference
    val whiteMaterialValue = board.capturedByBlack.sumOf { it.type.value }
    val blackMaterialValue = board.capturedByWhite.sumOf { it.type.value }
    val playerMaterialDiff = if (playerColor == PieceColor.WHITE) {
        whiteMaterialValue - blackMaterialValue
    } else {
        blackMaterialValue - whiteMaterialValue
    }
    val opponentMaterialDiff = -playerMaterialDiff

    val playerInfo = PlayerInfo(
        id = userProfile.guestId,
        name = userProfile.displayName,
        rating = userProfile.rating,
        avatarIndex = userProfile.avatarIndex,
        isGuest = true,
        isSpeaking = voiceState.isRecording || voiceState.isPushToTalk
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Match Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (board.gameStatus == GameStatus.ACTIVE || board.gameStatus == GameStatus.CHECK) {
                        showExitConfirm = true
                    } else {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .testTag("match_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Match Status / Room Code Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (gameMode == GameMode.ONLINE_MULTIPLAYER && roomCode != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurfaceVariant)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(roomCode ?: ""))
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ROOM: $roomCode",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = ChampagneGold,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Time Control preset chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⏱ ${selectedPreset.title} ${selectedPreset.category}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChampagneGold
                    )
                }

                // Check or Turn Badge
                val statusText = when (board.gameStatus) {
                    GameStatus.CHECK -> "CHECK!"
                    GameStatus.CHECKMATE -> "CHECKMATE"
                    GameStatus.STALEMATE -> "STALEMATE"
                    GameStatus.RESIGNATION -> "RESIGNED"
                    GameStatus.TIMEOUT -> "FLAG FALLEN (0:00)"
                    GameStatus.DRAW_AGREED -> "DRAW"
                    GameStatus.ACTIVE -> if (board.turn == PieceColor.WHITE) "White to move" else "Black to move"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (board.gameStatus) {
                                GameStatus.CHECK -> CrimsonCheck.copy(alpha = 0.2f)
                                GameStatus.TIMEOUT -> CrimsonCheck.copy(alpha = 0.3f)
                                else -> ObsidianSurfaceVariant
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (board.gameStatus) {
                            GameStatus.CHECK -> CrimsonCheck
                            GameStatus.TIMEOUT -> CrimsonCheck
                            else -> Color.LightGray
                        }
                    )
                }
            }

            // Move List and Flip Board Actions
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { viewModel.flipBoardPerspective() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Flip Board",
                        tint = ChampagneGold
                    )
                }

                IconButton(
                    onClick = { showMoveHistoryModal = true },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("move_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = "Move History",
                        tint = Color.White
                    )
                }
            }
        }

        // Waiting for Opponent Overlay (in custom room)
        if (isWaitingForOpponent) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChampagneGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = ChampagneGold,
                        strokeWidth = 2.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Waiting for opponent...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Share Room Code '$roomCode' with a friend to begin.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Opponent Player Card
        PlayerCardView(
            player = opponent,
            color = opponentColor,
            remainingTimeMs = if (opponentColor == PieceColor.WHITE) whiteClockMs else blackClockMs,
            isTurn = isOpponentTurn,
            capturedPieces = if (opponentColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack,
            materialAdvantage = opponentMaterialDiff,
            isOpponent = true
        )

        // Main Chess Board
        ChessBoardView(
            board = board,
            selectedPos = selectedPos,
            legalMoves = legalMoves,
            perspective = playerColor,
            theme = theme,
            onSquareClick = { pos -> viewModel.onSquareSelected(pos) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Live Voice Chat Bar
        VoiceChatBar(
            voiceState = voiceState,
            opponentName = opponent.name,
            onToggleMic = { viewModel.voiceChatManager.toggleMicMute() },
            onStartPushToTalk = { viewModel.voiceChatManager.startPushToTalk() },
            onStopPushToTalk = { viewModel.voiceChatManager.stopPushToTalk() },
            onToggleDeafen = { viewModel.voiceChatManager.toggleDeafen() },
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )

        // User Player Card
        PlayerCardView(
            player = playerInfo,
            color = playerColor,
            remainingTimeMs = if (playerColor == PieceColor.WHITE) whiteClockMs else blackClockMs,
            isTurn = isPlayerTurn,
            capturedPieces = if (playerColor == PieceColor.WHITE) board.capturedByWhite else board.capturedByBlack,
            materialAdvantage = playerMaterialDiff,
            isOpponent = false
        )

        // Match Bottom Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Offer Draw Button
            OutlinedButton(
                onClick = { viewModel.offerDraw() },
                enabled = board.gameStatus == GameStatus.ACTIVE || board.gameStatus == GameStatus.CHECK,
                modifier = Modifier
                    .weight(1f)
                    .testTag("offer_draw_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = "Draw",
                    modifier = Modifier.size(16.dp),
                    tint = ChampagneGold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Draw", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Resign Button
            OutlinedButton(
                onClick = { showResignConfirm = true },
                enabled = board.gameStatus == GameStatus.ACTIVE || board.gameStatus == GameStatus.CHECK,
                modifier = Modifier
                    .weight(1f)
                    .testTag("resign_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonCheck),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonCheck.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Resign",
                    modifier = Modifier.size(16.dp),
                    tint = CrimsonCheck
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Resign", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Pawn Promotion Dialog
    if (pendingPromotion != null) {
        PawnPromotionDialog(
            color = playerColor,
            onSelectPiece = { chosenType ->
                viewModel.onSelectPromotionPiece(chosenType)
            },
            onDismiss = {
                viewModel.cancelPromotion()
            }
        )
    }

    // Resign Confirmation Dialog
    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Resign Match?", color = CrimsonCheck, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to forfeit this match? Your opponent will be awarded victory.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resign()
                        showResignConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonCheck, contentColor = Color.White)
                ) {
                    Text("Resign", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Exit Match Confirmation Dialog
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Exit Game?", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Leaving the current match will automatically forfeit the game.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resign()
                        showExitConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonCheck, contentColor = Color.White)
                ) {
                    Text("Forfeit & Exit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Stay in Game", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Draw Offer Received Dialog
    if (drawOfferedByOpponent) {
        AlertDialog(
            onDismissRequest = { viewModel.declineDrawOffer() },
            containerColor = ObsidianSurface,
            title = {
                Text("Draw Offered", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "${opponent.name} has offered a draw. Do you accept?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acceptDrawOffer() },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black)
                ) {
                    Text("Accept Draw", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineDrawOffer() }) {
                    Text("Decline", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Move History Modal
    if (showMoveHistoryModal) {
        AlertDialog(
            onDismissRequest = { showMoveHistoryModal = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Move History", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                val moves = board.moveHistory
                if (moves.isEmpty()) {
                    Text("No moves played yet.", color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        val turnPairs = moves.chunked(2)
                        itemsIndexed(turnPairs) { index, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ChampagneGold,
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = pair[0].notation,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (pair.size > 1) pair[1].notation else "",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveHistoryModal = false }) {
                    Text("Close", color = ChampagneGold)
                }
            }
        )
    }

    // Game Over Banner / Modal
    val isGameOver = board.gameStatus in listOf(
        GameStatus.CHECKMATE,
        GameStatus.STALEMATE,
        GameStatus.RESIGNATION,
        GameStatus.TIMEOUT,
        GameStatus.DRAW_AGREED
    )

    if (isGameOver) {
        val isVictory = board.winner == playerColor
        val isDraw = board.gameStatus == GameStatus.STALEMATE || board.gameStatus == GameStatus.DRAW_AGREED
        val title = when {
            isDraw -> "Match Drawn"
            board.gameStatus == GameStatus.TIMEOUT -> if (isVictory) "Victory on Time!" else "Time Out (Defeat)"
            isVictory -> "Victory!"
            else -> "Defeat"
        }
        val titleColor = when {
            isDraw -> ElectricCyan
            isVictory -> EmeraldAccent
            else -> CrimsonCheck
        }

        val reasonText = when (board.gameStatus) {
            GameStatus.CHECKMATE -> "by Checkmate"
            GameStatus.RESIGNATION -> "by Resignation"
            GameStatus.TIMEOUT -> if (isVictory) "${opponent.name}'s clock reached 0:00" else "Your chess clock reached 0:00"
            GameStatus.STALEMATE -> "by Stalemate"
            GameStatus.DRAW_AGREED -> "by Mutual Agreement"
            else -> ""
        }

        AlertDialog(
            onDismissRequest = {},
            containerColor = ObsidianSurface,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor
                    )
                    Text(
                        text = reasonText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isVictory) "+16 ELO Rating" else if (isDraw) "+0 ELO Rating" else "-12 ELO Rating",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isVictory) EmeraldAccent else if (isDraw) ElectricCyan else CrimsonCheck
                        )
                    }

                    Text(
                        text = "Total Moves: ${board.moveHistory.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black),
                    modifier = Modifier.testTag("rematch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rematch",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rematch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onNavigateBack() }) {
                    Text("Back to Lobby", color = Color.White)
                }
            }
        )
    }
}
