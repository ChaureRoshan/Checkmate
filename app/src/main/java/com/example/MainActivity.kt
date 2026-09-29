package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.ui.screens.GameScreen
import com.example.chess.ui.screens.LobbyScreen
import com.example.chess.viewmodel.GameViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg

enum class Screen {
    LOBBY,
    GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(Screen.LOBBY) }

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
