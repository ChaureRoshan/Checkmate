package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.chess.ui.screens.GameScreen
import com.example.chess.ui.screens.LobbyScreen
import com.example.chess.ui.screens.WelcomeScreen
import com.example.chess.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg

enum class Screen {
    WELCOME,
    LOBBY,
    GAME
}

class MainActivity : ComponentActivity() {

    // Same activity-scoped instance the UI uses, so we can pause "online" status in the background
    private val gameViewModel: GameViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        gameViewModel.setAppVisible(true)
    }

    override fun onStop() {
        super.onStop()
        gameViewModel.setAppVisible(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember {
                    mutableStateOf(if (gameViewModel.authManager.welcomeDone) Screen.LOBBY else Screen.WELCOME)
                }
                val context = LocalContext.current
                val notice by gameViewModel.notice.collectAsState()

                LaunchedEffect(Unit) {
                    gameViewModel.exitToLobby.collect { currentScreen = Screen.LOBBY }
                }
                LaunchedEffect(notice) {
                    notice?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        gameViewModel.clearNotice()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ObsidianBg)
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                            when (screen) {
                                Screen.WELCOME -> {
                                    WelcomeScreen(
                                        viewModel = gameViewModel,
                                        onContinue = { currentScreen = Screen.LOBBY }
                                    )
                                }
                                Screen.LOBBY -> {
                                    LobbyScreen(
                                        viewModel = gameViewModel,
                                        onNavigateToGame = {
                                            currentScreen = Screen.GAME
                                        }
                                    )
                                }
                                Screen.GAME -> {
                                    GameScreen(
                                        viewModel = gameViewModel,
                                        onNavigateBack = {
                                            gameViewModel.leaveOnlineRoom()
                                            currentScreen = Screen.LOBBY
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
