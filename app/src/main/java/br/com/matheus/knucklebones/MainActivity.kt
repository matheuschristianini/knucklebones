package br.com.matheus.knucklebones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import br.com.matheus.knucklebones.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide status bars and navigation bars for a full-screen immersive experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            MyApplicationTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var vsAI by remember { mutableStateOf(false) }
                var difficulty by remember { mutableStateOf(Difficulty.Easy) }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (screen) {
                        Screen.Home -> HomePage(onStartGame = { ai, diff ->
                            vsAI = ai
                            difficulty = diff ?: Difficulty.Easy
                            screen = Screen.Game
                        })
                        Screen.Game -> KnucklebonesGame(
                            vsAI = vsAI,
                            difficulty = difficulty,
                            onBackToHome = { screen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object Game : Screen()
}
