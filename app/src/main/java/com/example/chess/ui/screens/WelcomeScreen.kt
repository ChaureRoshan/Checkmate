package com.example.chess.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.auth.SignInResult
import com.example.chess.auth.findActivity
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.ui.components.ChessPieceVisual
import com.example.chess.viewmodel.GameViewModel
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import kotlinx.coroutines.launch

private val WelcomeGreen = Color(0xFF81B64C)

@Composable
fun WelcomeScreen(
    viewModel: GameViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3A3834), Color(0xFF1F1E1C))))
                .border(1.dp, ObsidianBorder, RoundedCornerShape(28.dp))
                .padding(14.dp)
        ) {
            ChessPieceVisual(piece = ChessPiece(id = "logo", type = PieceType.KNIGHT, color = PieceColor.WHITE))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Checkmate",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "Dosto ke saath chess, voice ke saath",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Continue with Google
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable(enabled = !loading) {
                    val activity = context.findActivity()
                    if (activity == null) {
                        error = "Screen nahi mili, app dobara kholo."
                        return@clickable
                    }
                    loading = true
                    error = null
                    scope.launch {
                        when (val result = viewModel.authManager.signInWithGoogle(activity)) {
                            is SignInResult.Success -> {
                                viewModel.onGoogleSignedIn(result.account)
                                viewModel.markWelcomeDone()
                                onContinue()
                            }
                            is SignInResult.Failure -> error = result.message
                            SignInResult.Cancelled -> Unit
                        }
                        loading = false
                    }
                }
                .testTag("google_login_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = "G",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF202124)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue as guest
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.5.dp, WelcomeGreen, RoundedCornerShape(16.dp))
                .clickable(enabled = !loading) {
                    viewModel.markWelcomeDone()
                    onContinue()
                }
                .testTag("guest_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Continue as guest",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WelcomeGreen
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = error ?: "",
                fontSize = 12.sp,
                color = Color(0xFFE57373),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Guest: koi login nahi, seedha khelo.\nGoogle: pehli baar me account apne aap ban jata hai, alag register nahi karna. Ek baar login ke baad dobara nahi poochega.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
