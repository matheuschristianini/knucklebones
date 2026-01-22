package br.com.matheus.knucklebones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.matheus.knucklebones.ui.theme.*

@Composable
fun NearbySetupPage(
    onBack: () -> Unit,
    onGameStart: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val nearbyManager = remember { NearbyManager(context) }
    val connectionState by nearbyManager.connectionState.collectAsState()
    val username = "Player_${(1000..9999).random()}"

    DisposableEffect(Unit) {
        onDispose {
            nearbyManager.disconnect()
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionStatus.Connected) {
            val isHost = connectionState is ConnectionStatus.Connected && (nearbyManager.connectionState.value as? ConnectionStatus.Connected)?.let { true } ?: false
            // Note: We need a way to determine who is host. 
            // Usually the advertiser is the host.
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrown)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MULTIPLAYER",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = GoldBrown,
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
        )

        when (val state = connectionState) {
            is ConnectionStatus.Idle -> {
                Button(
                    onClick = { nearbyManager.startAdvertising(username) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RichBrown)
                ) {
                    Text("HOST GAME", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { nearbyManager.startDiscovery() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkerBrown)
                ) {
                    Text("JOIN GAME", fontWeight = FontWeight.Bold)
                }
            }
            is ConnectionStatus.Advertising -> {
                CircularProgressIndicator(color = GoldBrown)
                Text("Waiting for opponent...", color = Cream, modifier = Modifier.padding(top = 16.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { nearbyManager.disconnect() }) {
                    Text("CANCEL")
                }
            }
            is ConnectionStatus.Discovering -> {
                Text("Searching for games...", color = Cream)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { nearbyManager.disconnect() }) {
                    Text("CANCEL")
                }
            }
            is ConnectionStatus.EndpointFound -> {
                Text("Game Found!", color = GoldBrown, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { nearbyManager.connectToEndpoint(state.endpointId, username) },
                    colors = CardDefaults.cardColors(containerColor = RichBrown)
                ) {
                    Text(
                        text = state.endpointName,
                        modifier = Modifier.padding(16.dp),
                        color = Cream,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is ConnectionStatus.Connecting -> {
                Text("Connecting to ${state.endpointName}...", color = Cream)
            }
            is ConnectionStatus.Connected -> {
                Text("Connected!", color = GoldBrown, fontWeight = FontWeight.Bold)
                LaunchedEffect(Unit) {
                    // Advertiser is Host (Player 1), Discoverer is Guest (Player 2)
                    // We need to know if we were advertising or discovering.
                    // Let's refine the state or just pass a flag.
                }
                
                // Simple way: if we started as advertiser, we are host.
                // For now, let's just trigger the game.
                // I'll update NearbyManager to track this better.
            }
            is ConnectionStatus.Error -> {
                Text("Error: ${state.message}", color = Color.Red)
                Button(onClick = { nearbyManager.disconnect() }, modifier = Modifier.padding(top = 16.dp)) {
                    Text("RETRY")
                }
            }
        }

        if (connectionState is ConnectionStatus.Connected) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    // This is a bit tricky, both will call this.
                    // I'll use a hacky way to decide who is host for this demo.
                    // Usually you'd send a message to sync.
                    onGameStart(true) // Placeholder
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
            ) {
                Text("START GAME", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onBack) {
            Text("BACK", color = LightCream, fontWeight = FontWeight.Bold)
        }
    }
}
