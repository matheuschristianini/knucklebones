package br.com.matheus.knucklebones

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.matheus.knucklebones.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun KnucklebonesGame(
    vsAI: Boolean,
    isNearby: Boolean = false,
    isHost: Boolean = true,
    difficulty: Difficulty,
    onBackToHome: () -> Unit,
    viewModel: KnucklebonesViewModel = viewModel()
) {
    val state = viewModel.state
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val nearbyManager = remember { if (isNearby) NearbyManager(context) else null }
    val receivedMessage by nearbyManager?.receivedMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(vsAI, difficulty, isNearby, isHost) {
        viewModel.initGame(vsAI, difficulty, isNearby, isHost)
        
        if (isNearby) {
            viewModel.setOnMovePerformed { col, roll ->
                nearbyManager?.sendMessage("$col:$roll")
            }
        }
    }

    LaunchedEffect(receivedMessage) {
        receivedMessage?.let { msg ->
            val parts = msg.split(":")
            if (parts.size == 2) {
                val col = parts[0].toIntOrNull()
                val roll = parts[1].toIntOrNull()
                if (col != null && roll != null) {
                    viewModel.receiveRemoteMove(col, roll)
                }
            }
            nearbyManager?.clearReceivedMessage()
        }
    }

    LaunchedEffect(state.currentPlayer, state.gameOver) {
        if (vsAI && state.currentPlayer == Player.Player2 && !state.gameOver) {
            delay(1000)
            viewModel.aiTurn(context)
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Game", color = GoldBrown) },
            text = { Text("Are you sure you want to get back to homepage?", color = DarkBrown) },
            containerColor = Cream,
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    nearbyManager?.disconnect()
                    onBackToHome()
                }) {
                    Text("Yes", color = RichBrown, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No", color = RichBrown)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBrown)) {
        // Exit Button
        IconButton(
            onClick = { showExitDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(RichBrown.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Exit", tint = Cream)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Player 2 Area (Top - Opponent/AI/Remote)
            val p2Label = when {
                vsAI -> "AI (${state.difficulty})"
                isNearby -> if (isHost) "REMOTE PLAYER" else "YOU"
                else -> "PLAYER 2"
            }
            val isP2Local = !vsAI && (!isNearby || !isHost)

            PlayerArea(
                playerLabel = p2Label,
                board = state.player2Board,
                isCurrentPlayer = state.currentPlayer == Player.Player2,
                onColumnClick = { if (isP2Local && state.currentPlayer == Player.Player2) viewModel.placeDie(it, context) },
                reverse = true,
                themeColor = MediumBrown
            )

            // Middle: Game Status and Current Roll
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.gameOver) {
                    val p1Score = state.player1Board.calculateTotalScore()
                    val p2Score = state.player2Board.calculateTotalScore()
                    val resultText = when {
                        p1Score > p2Score -> if (isNearby && !isHost) "REMOTE WINS..." else "VICTORY!"
                        p2Score > p1Score -> if (vsAI) "AI WINS..." else if (isNearby && isHost) "REMOTE WINS..." else "PLAYER 2 WINS!"
                        else -> "DRAW!"
                    }
                    Text(resultText, fontSize = 36.sp, fontWeight = FontWeight.Black, color = GoldBrown)
                    Text("Score: $p1Score - $p2Score", fontSize = 20.sp, color = LightCream)
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Button(
                            onClick = { 
                                viewModel.resetGame()
                                // In nearby, maybe send a reset signal? For now just reset local.
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RichBrown, contentColor = Cream)
                        ) {
                            Text("REMATCH")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                nearbyManager?.disconnect()
                                onBackToHome()
                            },
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(RichBrown))
                        ) {
                            Text("HOME", color = Cream)
                        }
                    }
                } else {
                    val turnText = when {
                        state.currentPlayer == Player.Player1 -> if (isNearby && !isHost) "REMOTE TURN" else "YOUR TURN"
                        state.currentPlayer == Player.Player2 -> if (isNearby && isHost) "REMOTE TURN" else if (vsAI) "AI THINKING..." else "PLAYER 2 TURN"
                        else -> ""
                    }
                    Text(
                        text = turnText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.currentPlayer == Player.Player1) GoldBrown else MediumBrown
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DieView(
                        value = state.currentRoll,
                        size = 80.dp,
                        dotColor = VeryDarkBrown,
                        backgroundColor = Cream,
                        shadowColor = Tan
                    )
                }
            }

            // Player 1 Area (Bottom - You/Remote Host)
            val p1Label = when {
                isNearby -> if (isHost) "YOU" else "REMOTE PLAYER"
                else -> "YOU"
            }
            val isP1Local = !isNearby || isHost

            PlayerArea(
                playerLabel = p1Label,
                board = state.player1Board,
                isCurrentPlayer = state.currentPlayer == Player.Player1,
                onColumnClick = { if (isP1Local && state.currentPlayer == Player.Player1) viewModel.placeDie(it, context) },
                reverse = false,
                themeColor = GoldBrown
            )
        }
    }
}

@Composable
fun PlayerArea(
    playerLabel: String,
    board: Board,
    isCurrentPlayer: Boolean,
    onColumnClick: (Int) -> Unit,
    reverse: Boolean,
    themeColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!reverse) {
            Text("$playerLabel: ${board.calculateTotalScore()}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = themeColor)
        }
        
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (i in 0..2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (reverse) {
                        BoardColumn(
                            column = board.columns[i],
                            onClick = { onColumnClick(i) },
                            isCurrentPlayer = isCurrentPlayer,
                            reverse = true,
                            themeColor = themeColor
                        )
                        Text("${board.calculateColumnScore(i)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LightCream, modifier = Modifier.padding(top = 4.dp))
                    } else {
                        Text("${board.calculateColumnScore(i)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = LightCream, modifier = Modifier.padding(bottom = 4.dp))
                        BoardColumn(
                            column = board.columns[i],
                            onClick = { onColumnClick(i) },
                            isCurrentPlayer = isCurrentPlayer,
                            reverse = false,
                            themeColor = themeColor
                        )
                    }
                }
            }
        }

        if (reverse) {
            Text("$playerLabel: ${board.calculateTotalScore()}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = themeColor)
        }
    }
}

@Composable
fun BoardColumn(
    column: List<Int>,
    onClick: () -> Unit,
    isCurrentPlayer: Boolean,
    reverse: Boolean,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .height(240.dp)
            .background(
                DarkGreen,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isCurrentPlayer) 3.dp else 1.dp,
                color = if (isCurrentPlayer) themeColor else VeryDarkBrown,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isCurrentPlayer && column.size < 3) { onClick() }
            .padding(6.dp),
        verticalArrangement = if (reverse) Arrangement.Top else Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val displayDice = if (reverse) column else column.reversed()
        displayDice.forEach { value ->
            DieView(
                value = value, 
                size = 60.dp, 
                modifier = Modifier.padding(vertical = 4.dp),
                dotColor = VeryDarkBrown,
                backgroundColor = if (column.count { it == value } > 1) Tan else Cream,
                shadowColor = if (column.count { it == value } > 1) MediumBrown else Tan
            )
        }
    }
}

@Composable
fun DieView(
    value: Int, 
    size: Dp, 
    modifier: Modifier = Modifier,
    dotColor: Color = VeryDarkBrown,
    backgroundColor: Color = Cream,
    shadowColor: Color = Tan
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(size * 0.2f))
            .border(2.dp, VeryDarkBrown, RoundedCornerShape(size * 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (value > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = shadowColor.copy(alpha = 0.3f),
                    radius = size.toPx() * 0.4f,
                    center = Offset(size.toPx() * 0.6f, size.toPx() * 0.6f)
                )
            }

            Canvas(modifier = Modifier.size(size * 0.7f)) {
                val canvasSize = this.size.width
                val dotRadius = canvasSize * 0.12f
                
                val center = canvasSize / 2
                val left = canvasSize * 0.25f
                val right = canvasSize * 0.75f
                val top = canvasSize * 0.25f
                val bottom = canvasSize * 0.75f

                fun drawDot(x: Float, y: Float) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                }

                when (value) {
                    1 -> drawDot(center, center)
                    2 -> {
                        drawDot(left, top)
                        drawDot(right, bottom)
                    }
                    3 -> {
                        drawDot(left, top)
                        drawDot(center, center)
                        drawDot(right, bottom)
                    }
                    4 -> {
                        drawDot(left, top)
                        drawDot(right, top)
                        drawDot(left, bottom)
                        drawDot(right, bottom)
                    }
                    5 -> {
                        drawDot(left, top)
                        drawDot(right, top)
                        drawDot(center, center)
                        drawDot(left, bottom)
                        drawDot(right, bottom)
                    }
                    6 -> {
                        drawDot(left, top)
                        drawDot(right, top)
                        drawDot(left, center)
                        drawDot(right, center)
                        drawDot(left, bottom)
                        drawDot(right, bottom)
                    }
                }
            }
        }
    }
}
