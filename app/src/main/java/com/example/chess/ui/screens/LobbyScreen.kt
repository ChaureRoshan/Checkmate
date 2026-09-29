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
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header & Branding
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MINIMAL CHESS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = ChampagneGold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                        )
                        Text(
                            text = "Cloud Multiplayer & Real-Time Voice Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldAccent
                        )
                    }
                }

                // Theme selector button
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ObsidianSurfaceVariant)
                        .border(1.dp, ObsidianBorder, CircleShape)
                        .testTag("theme_selector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Themes",
                        tint = ChampagneGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Guest Profile Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ChampagneGold.copy(alpha = 0.2f))
                                    .border(1.5.dp, ChampagneGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = userProfile.displayName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { showEditNameDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Name",
                                            tint = ChampagneGold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ChampagneGold.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "GUEST MODE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChampagneGold
                                        )
                                    }

                                    Text(
                                        text = "No account needed",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Rating Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ChampagneGold.copy(alpha = 0.15f))
                                .border(1.dp, ChampagneGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${userProfile.rating}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ChampagneGold
                                )
                                Text(
                                    text = "RATING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGoldDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceVariant)
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(label = "Wins", value = "${userProfile.wins}", color = EmeraldAccent)
                        StatItem(label = "Losses", value = "${userProfile.losses}", color = CrimsonCheck)
                        StatItem(label = "Draws", value = "${userProfile.draws}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val total = userProfile.wins + userProfile.losses + userProfile.draws
                        val winPct = if (total > 0) "${(userProfile.wins * 100) / total}%" else "--"
                        StatItem(label = "Win Rate", value = winPct, color = ElectricCyan)
                    }
                }
            }
        }

        // Voice Chat Highlight Banner
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, if (liveKitState.isConfigured) EmeraldAccent.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable { showLiveKitDialog = true },
                color = if (liveKitState.isConfigured) EmeraldAccent.copy(alpha = 0.08f) else ElectricCyan.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (liveKitState.isConfigured) EmeraldAccent.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = if (liveKitState.isConfigured) EmeraldAccent else ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LiveKit Cloud Voice",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (liveKitState.isConfigured) EmeraldAccent.copy(alpha = 0.2f) else ChampagneGold.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (liveKitState.isConfigured) "LIVEKIT ACTIVE" else "TAP TO CONFIGURE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (liveKitState.isConfigured) EmeraldAccent else ChampagneGold
                                )
                            }
                        }
                        Text(
                            text = if (liveKitState.isConfigured)
                                "Project: Chess (${liveKitState.serverUrl.replace("wss://", "")})"
                            else
                                "Configured for ${liveKitState.serverUrl.replace("wss://", "")}. Tap to add API keys.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Time Control Selector Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHESS CLOCK (TIME CONTROL)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ChampagneGold
                    )
                    Text(
                        text = "${selectedPreset.title} • ${selectedPreset.category}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ChampagneGold else ObsidianSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) ChampagneGold else ObsidianBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectTimeControl(preset) }
                                .padding(vertical = 8.dp)
                                .testTag("time_control_${preset.title}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = preset.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                                Text(
                                    text = preset.category,
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.Black.copy(alpha = 0.8f) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Game Mode Section Title
        item {
            Text(
                text = "PLAY CHESS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ChampagneGold
            )
        }

        // Quick Match Primary Hero Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(ChampagneGoldDark, ChampagneGold)
                        )
                    )
                    .clickable {
                        viewModel.startQuickMatch()
                        onNavigateToGame()
                    }
                    .padding(20.dp)
                    .testTag("quick_match_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "QUICK MATCH",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Instant 10-minute game with live guest players & voice chat",
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = ChampagneGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Secondary Modes Grid / Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Host / Join Private Room
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeSquareCard(
                        title = "Host Room",
                        subtitle = "Create code to invite a friend",
                        icon = Icons.Default.AddCircleOutline,
                        accentColor = ElectricCyan,
                        onClick = {
                            viewModel.createOnlineRoom(timeMinutes = 10)
                            onNavigateToGame()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "host_room_button"
                    )

                    ModeSquareCard(
                        title = "Join Room",
                        subtitle = "Enter 6-digit match code",
                        icon = Icons.Default.Group,
                        accentColor = ElectricCyan,
                        onClick = { showJoinRoomDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "join_room_button"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeSquareCard(
                        title = "Vs Bot",
                        subtitle = "Novice, Adept, or Master",
                        icon = Icons.Default.SmartToy,
                        accentColor = ChampagneGold,
                        onClick = { showBotDifficultyDialog = true },
                        modifier = Modifier.weight(1f),
                        testTag = "vs_bot_button"
                    )

                    ModeSquareCard(
                        title = "Pass & Play",
                        subtitle = "2 players, 1 screen",
                        icon = Icons.Default.Psychology,
                        accentColor = EmeraldAccent,
                        onClick = {
                            viewModel.startPassAndPlay()
                            onNavigateToGame()
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "pass_and_play_button"
                    )
                }
            }
        }

        // Recent Games Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATCH HISTORY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ChampagneGold
                )

                Text(
                    text = "${recentGames.size} Games",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recentGames.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "No matches",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Games Played Yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Play a quick online match or practice against the bot to start recording your history.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(recentGames.take(8)) { game ->
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
