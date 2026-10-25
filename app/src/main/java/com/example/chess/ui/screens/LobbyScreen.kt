package com.example.chess.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.GameRecordEntity
import com.example.chess.model.BoardTheme
import com.example.chess.model.BotDifficulty
import com.example.chess.multiplayer.OnlinePlayer
import com.example.chess.viewmodel.GameViewModel
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldDark
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ChessGreen = Color(0xFF81B64C)

@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onNavigateToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val recentGames by viewModel.recentGames.collectAsState()
    val currentTheme by viewModel.boardTheme.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val liveKitState by viewModel.liveKitManager.state.collectAsState()
    val onlinePlayers by viewModel.onlinePlayers.collectAsState()
    val incomingChallenge by viewModel.incomingChallenge.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var showHostRoomDialog by remember { mutableStateOf(false) }
    var showBotDifficultyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLiveKitDialog by remember { mutableStateOf(false) }
    var liveKitApiKeyInput by remember { mutableStateOf(viewModel.liveKitManager.getApiKey()) }
    var liveKitApiSecretInput by remember { mutableStateOf(viewModel.liveKitManager.getApiSecret()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checkmate",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                IconButton(onClick = { showThemeDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Board theme",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Profile (guest, no login)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .clickable { showEditNameDialog = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ChessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Rating ${userProfile.rating}  \u2022  ${userProfile.wins}W ${userProfile.losses}L ${userProfile.draws}D",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit name",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Big Play button + time control
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        viewModel.startQuickMatch()
                        onNavigateToGame()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .testTag("quick_match_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChessGreen,
                        contentColor = Color.White
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Play", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "${selectedPreset.title} \u2022 ${selectedPreset.category} \u2022 Online",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in viewModel.timeControlPresets) {
                        val isSelected = preset == selectedPreset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) ChessGreen else ObsidianSurface)
                                .clickable { viewModel.selectTimeControl(preset) }
                                .padding(vertical = 9.dp)
                                .testTag("time_control_${preset.title}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Voice chat status (no login: connects automatically in every online game)
        item {
            val voiceReady = liveKitState.isConfigured
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurface)
                    .clickable { showLiveKitDialog = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (voiceReady) ChessGreen else ChampagneGold,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (voiceReady) "Voice chat ready" else "Voice chat setup needed",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (voiceReady) "Online game me apne aap judta hai. Login nahi chahiye."
                        else "Tap karke LiveKit key daalo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Online players
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (onlinePlayers.isNotEmpty()) ChessGreen else Color.Gray)
                    )
                    Text(
                        text = "Online now (${onlinePlayers.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (onlinePlayers.isEmpty()) {
                    Text(
                        text = "Abhi koi aur online nahi hai. Dost ko app khulwao, wo yahan dikhega.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("no_online_players")
                    )
                } else {
                    onlinePlayers.forEach { player ->
                        OnlinePlayerRow(
                            player = player,
                            onChallenge = {
                                viewModel.challengePlayer(player)
                                onNavigateToGame()
                            }
                        )
                    }
                }
            }
        }

        // More ways to play
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "More ways to play",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface)
                ) {
                    PlayOptionRow(
                        icon = Icons.Default.AddCircleOutline,
                        title = "Play a friend",
                        subtitle = "Room code banao, dost ko bhejo",
                        onClick = {
                            viewModel.createOnlineRoom(timeMinutes = selectedPreset.minutes)
                            onNavigateToGame()
                        },
                        testTag = "host_room_button"
                    )
                    PlayOptionRow(
                        icon = Icons.Default.Group,
                        title = "Join with code",
                        subtitle = "Dost ka room code daalo",
                        onClick = { showJoinRoomDialog = true },
                        testTag = "join_room_button"
                    )
                    PlayOptionRow(
                        icon = Icons.Default.SmartToy,
                        title = "Play bots",
                        subtitle = "Novice, Adept ya Master",
                        onClick = { showBotDifficultyDialog = true },
                        testTag = "vs_bot_button"
                    )
                    PlayOptionRow(
                        icon = Icons.Default.Psychology,
                        title = "Pass & play",
                        subtitle = "2 players, 1 phone",
                        onClick = {
                            viewModel.startPassAndPlay()
                            onNavigateToGame()
                        },
                        testTag = "pass_and_play_button"
                    )
                }
            }
        }

        // Recent games
        item {
            Text(
                text = "Recent games",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        if (recentGames.isEmpty()) {
            item {
                Text(
                    text = "Abhi tak koi game nahi khela. Upar Play dabao.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recentGames.take(5)) { game ->
                GameHistoryCard(game = game)
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        var newNameInput by remember { mutableStateOf(userProfile.displayName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Edit Guest Name", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Choose your display name for matches:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = newNameInput,
                        onValueChange = { if (it.length <= 16) newNameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("guest_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateGuestDisplayName(newNameInput)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Join Room Dialog
    if (showJoinRoomDialog) {
        var roomCodeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Join Match Room", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter the 6-character room code from your opponent (e.g. KNG-482):",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase() },
                        singleLine = true,
                        placeholder = { Text("KNG-123", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roomCodeInput.isNotBlank()) {
                            viewModel.joinOnlineRoom(roomCodeInput)
                            showJoinRoomDialog = false
                            onNavigateToGame()
                        }
                    },
                    enabled = roomCodeInput.trim().length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black)
                ) {
                    Text("Join Match", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinRoomDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Bot Difficulty Picker Dialog
    if (showBotDifficultyDialog) {
        AlertDialog(
            onDismissRequest = { showBotDifficultyDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Select Bot Difficulty", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val difficulties = listOf(
                        BotDifficulty.CASUAL to "Casual Novice (800 ELO)",
                        BotDifficulty.TACTICAL to "Tactical Adept (1500 ELO)",
                        BotDifficulty.MASTER to "Grandmaster AI (2200 ELO)"
                    )
                    for ((diff, title) in difficulties) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.startVsBot(diff)
                                    showBotDifficultyDialog = false
                                    onNavigateToGame()
                                }
                                .padding(14.dp),
                            color = ObsidianSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBotDifficultyDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Board Theme Selector Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Choose Board Theme", color = ChampagneGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (theme in BoardTheme.values()) {
                        val isSelected = theme == currentTheme
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.5.dp,
                                    if (isSelected) ChampagneGold else ObsidianBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    viewModel.setBoardTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(14.dp),
                            color = if (isSelected) ChampagneGold.copy(alpha = 0.15f) else ObsidianSurfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = theme.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ChampagneGold else Color.White
                                )
                                if (isSelected) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChampagneGold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Done", color = ChampagneGold)
                }
            }
        )
    }

    // Incoming direct request from another online player
    incomingChallenge?.let { challenge ->
        AlertDialog(
            onDismissRequest = { viewModel.declineChallenge(challenge) },
            containerColor = ObsidianSurface,
            title = {
                Text("Game request", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "${challenge.fromName} (${challenge.fromRating}) tumse khelna chahta hai - ${challenge.timeControlMinutes} min game.",
                    color = Color.White
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acceptChallenge(challenge)
                        onNavigateToGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black),
                    modifier = Modifier.testTag("accept_challenge_button")
                ) {
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineChallenge(challenge) }) {
                    Text("Decline", color = Color.White)
                }
            }
        )
    }

    // LiveKit Cloud Voice Settings Dialog
    if (showLiveKitDialog) {
        AlertDialog(
            onDismissRequest = { showLiveKitDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = if (liveKitState.isConfigured) EmeraldAccent else ElectricCyan
                        )
                        Text(
                            text = "LiveKit Cloud Voice",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Real-Time WebRTC Voice Chat",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Project info box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ObsidianSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PROJECT DETAILS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Project: Chess",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Project ID: ${liveKitState.projectId}",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "URL: ${liveKitState.serverUrl}",
                                fontSize = 11.sp,
                                color = ElectricCyan
                            )
                        }
                    }

                    Text(
                        text = "Enter your API Key and Secret from LiveKit Cloud (Settings > Keys) to enable cloud room tokens:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = liveKitApiKeyInput,
                        onValueChange = { liveKitApiKeyInput = it },
                        label = { Text("LiveKit API Key (e.g. API...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = liveKitApiSecretInput,
                        onValueChange = { liveKitApiSecretInput = it },
                        label = { Text("LiveKit API Secret") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChampagneGold,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.liveKitManager.saveCredentials(liveKitApiKeyInput, liveKitApiSecretInput)
                        showLiveKitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = Color.Black)
                ) {
                    Text("Save Credentials", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLiveKitDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun PlayOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ChessGreen,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = "\u203A", fontSize = 22.sp, color = Color.Gray)
    }
}

@Composable
fun OnlinePlayerRow(
    player: OnlinePlayer,
    onChallenge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp)),
        color = ObsidianSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ElectricCyan.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (player.inGame) "Rating ${player.rating} - game me hai" else "Rating ${player.rating} - online",
                    fontSize = 11.sp,
                    color = if (player.inGame) ChampagneGold else ChessGreen
                )
            }
            Button(
                onClick = onChallenge,
                enabled = !player.inGame,
                colors = ButtonDefaults.buttonColors(containerColor = ChessGreen, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("challenge_${player.id}")
            ) {
                Text("Challenge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModeSquareCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GameHistoryCard(game: GameRecordEntity) {
    val resultColor = when (game.result) {
        "WIN" -> EmeraldAccent
        "LOSS" -> CrimsonCheck
        else -> ElectricCyan
    }
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateString = remember(game.timestamp) { dateFormat.format(Date(game.timestamp)) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(resultColor.copy(alpha = 0.15f))
                        .border(1.dp, resultColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (game.result) {
                            "WIN" -> "W"
                            "LOSS" -> "L"
                            else -> "D"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = resultColor
                    )
                }

                Column {
                    Text(
                        text = "vs ${game.opponentName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${game.gameMode.replace("_", " ")} • ${game.movesCount} moves",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = game.result,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = resultColor
                )
                Text(
                    text = dateString,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
