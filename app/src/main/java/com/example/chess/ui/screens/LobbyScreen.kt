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
import kotlinx.coroutines.launch
import com.example.chess.auth.SignInResult
import com.example.chess.auth.findActivity
import com.example.chess.data.GameRecordEntity
import com.example.chess.model.BoardTheme
import com.example.chess.model.BotDifficulty
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.ui.components.ChessPieceVisual
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
    val account by viewModel.account.collectAsState()
    val nameChangesLeft by viewModel.nameChangesLeft.collectAsState()
    var showSignOutConfirm by remember { mutableStateOf(false) }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val authScope = androidx.compose.runtime.rememberCoroutineScope()

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
        // Header: logo, name, online counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF3A3834), Color(0xFF1F1E1C))))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    ChessPieceVisual(piece = ChessPiece(id = "logo", type = PieceType.KNIGHT, color = PieceColor.WHITE))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Checkmate",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Dosto ke saath chess, voice ke saath",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (onlinePlayers.isNotEmpty()) ChessGreen else Color.Gray)
                    )
                    Text(
                        text = "${onlinePlayers.size} online",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Profile card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                    .clickable { showEditNameDialog = true }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ChessGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ChessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
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
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name",
                            tint = if (nameChangesLeft > 0) Color.Gray else Color.Gray.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "Rating ${userProfile.rating}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChessGreen
                    )
                }
                StatPill(label = "W", value = userProfile.wins, color = ChessGreen)
                StatPill(label = "L", value = userProfile.losses, color = CrimsonCheck)
                StatPill(label = "D", value = userProfile.draws, color = Color.Gray)
            }
        }

        // Big Play card + time control
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF8CC152), Color(0xFF5F9A2F))))
                        .clickable {
                            viewModel.startQuickMatch()
                            onNavigateToGame()
                        }
                        .padding(20.dp)
                        .testTag("quick_match_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Play online",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${selectedPreset.title} • ${selectedPreset.category} • random opponent",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF5F9A2F),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (preset in viewModel.timeControlPresets) {
                        val isSelected = preset == selectedPreset
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) ChessGreen else ObsidianSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) ChessGreen else ObsidianBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectTimeControl(preset) }
                                .padding(vertical = 9.dp)
                                .testTag("time_control_${preset.title}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = preset.category,
                                fontSize = 9.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Voice setup reminder: only shown while the LiveKit keys are missing
        if (!liveKitState.isConfigured) item {
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

        // Google account row: sign in (guests) or sign out
        item {
            val acc = account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .clickable {
                        if (acc == null) {
                            val activity = appContext.findActivity()
                            if (activity != null) {
                                authScope.launch {
                                    when (val r = viewModel.authManager.signInWithGoogle(activity)) {
                                        is SignInResult.Success -> viewModel.onGoogleSignedIn(r.account)
                                        is SignInResult.Failure ->
                                            android.widget.Toast.makeText(appContext, r.message, android.widget.Toast.LENGTH_LONG).show()
                                        SignInResult.Cancelled -> Unit
                                    }
                                }
                            }
                        } else {
                            showSignOutConfirm = true
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("account_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "G",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4285F4)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (acc == null) "Google se login karo" else acc.email,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (acc == null) "Rating aur stats kisi bhi phone par mil jayenge"
                        else "Login hai. Rating aur stats cloud me save hain.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (acc == null) "Login" else "Sign out",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (acc == null) ChessGreen else CrimsonCheck
                )
            }
        }

        // Online players
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(title = "Online now", count = onlinePlayers.size)

                if (onlinePlayers.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianSurface)
                            .padding(14.dp)
                            .testTag("no_online_players"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Abhi koi aur online nahi hai. Dost ko app khulwao, wo yahan dikhega.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

        // More ways to play: 2 x 2 tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(title = "More ways to play")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AddCircleOutline,
                        title = "Play a friend",
                        subtitle = "Room code banao",
                        accent = ChessGreen,
                        onClick = {
                            viewModel.createOnlineRoom(timeMinutes = selectedPreset.minutes)
                            onNavigateToGame()
                        },
                        testTag = "host_room_button"
                    )
                    PlayTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Group,
                        title = "Join with code",
                        subtitle = "Dost ka code daalo",
                        accent = ElectricCyan,
                        onClick = { showJoinRoomDialog = true },
                        testTag = "join_room_button"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlayTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SmartToy,
                        title = "Play bots",
                        subtitle = "Novice se Master tak",
                        accent = ChampagneGold,
                        onClick = { showBotDifficultyDialog = true },
                        testTag = "vs_bot_button"
                    )
                    PlayTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Psychology,
                        title = "Pass & play",
                        subtitle = "2 players, 1 phone",
                        accent = Color(0xFFB39DDB),
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
        item { SectionTitle(title = "Recent games") }
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

    // Change name: only 2 changes per person
    if (showEditNameDialog) {
        var newNameInput by remember { mutableStateOf(userProfile.displayName) }
        val canEdit = nameChangesLeft > 0
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text(
                    text = if (canEdit) "Naam badlo" else "Naam badal nahi sakte",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canEdit) {
                        Text(
                            text = "Naam sirf 2 baar badal sakte ho. Abhi $nameChangesLeft chance bacha hai, soch ke likho.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = newNameInput,
                            onValueChange = { if (it.length <= 16) newNameInput = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ChessGreen,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guest_name_input")
                        )
                    } else {
                        Text(
                            text = "Naam badalne ke 2 chance khatam ho gaye. Ab naam nahi badal sakte.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (canEdit) {
                    Button(
                        onClick = {
                            viewModel.updateGuestDisplayName(newNameInput)
                            showEditNameDialog = false
                        },
                        enabled = newNameInput.trim().isNotEmpty() &&
                            newNameInput.trim() != userProfile.displayName,
                        colors = ButtonDefaults.buttonColors(containerColor = ChessGreen, contentColor = Color.White)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("OK", color = ChessGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (canEdit) {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }

    // Join a friend's room
    if (showJoinRoomDialog) {
        var roomCodeInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJoinRoomDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Room code daalo", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Dost ne jo code bheja hai wo likho (jaise KNG-482):",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { roomCodeInput = it.uppercase() },
                        singleLine = true,
                        placeholder = { Text("KNG-123", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChessGreen,
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
                    colors = ButtonDefaults.buttonColors(containerColor = ChessGreen, contentColor = Color.White)
                ) {
                    Text("Join", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinRoomDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Bot level
    if (showBotDifficultyDialog) {
        AlertDialog(
            onDismissRequest = { showBotDifficultyDialog = false },
            containerColor = ObsidianSurface,
            title = {
                Text("Bot ka level chuno", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val difficulties = listOf(
                        BotDifficulty.CASUAL to "Novice (800)",
                        BotDifficulty.TACTICAL to "Adept (1500)",
                        BotDifficulty.MASTER to "Master (2200)"
                    )
                    for ((diff, title) in difficulties) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.startVsBot(diff)
                                    showBotDifficultyDialog = false
                                    onNavigateToGame()
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = ChessGreen,
                                modifier = Modifier.size(20.dp)
                            )
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

    // Sign out needs a second tap, so it can't happen by accident
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = ObsidianSurface,
            title = { Text("Sign out karna hai?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Dobara Google se login karoge to rating aur stats wapas mil jayenge.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut()
                        showSignOutConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonCheck, contentColor = Color.White)
                ) {
                    Text("Sign out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Nahi", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SectionTitle(title: String, count: Int? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChessGreen.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChessGreen
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: Int, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "$value", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
    }
}

@Composable
private fun PlayTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(128.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
