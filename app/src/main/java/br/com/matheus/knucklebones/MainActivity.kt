package br.com.matheus.knucklebones

import android.Manifest
import android.content.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                var isNearby by remember { mutableStateOf(false) }
                var isHost by remember { mutableStateOf(true) }
                var difficulty by remember { mutableStateOf(Difficulty.Easy) }

                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    )
                } else {
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    if (results.values.all { it }) {
                        screen = Screen.NearbySetup
                    } else {
                        Toast.makeText(this, "Permissions required for multiplayer", Toast.LENGTH_SHORT).show()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (screen) {
                        Screen.Home -> HomePage(
                            onStartGame = { ai, diff ->
                                vsAI = ai
                                isNearby = false
                                difficulty = diff ?: Difficulty.Easy
                                screen = Screen.Game
                            },
                            onNearbyClick = {
                                launcher.launch(permissions)
                            }
                        )
                        Screen.NearbySetup -> NearbySetupPage(
                            onBack = { screen = Screen.Home },
                            onGameStart = { host ->
                                vsAI = false
                                isNearby = true
                                isHost = host
                                screen = Screen.Game
                            }
                        )
                        Screen.Game -> KnucklebonesGame(
                            vsAI = vsAI,
                            isNearby = isNearby,
                            isHost = isHost,
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
    object NearbySetup : Screen()
    object Game : Screen()
}
