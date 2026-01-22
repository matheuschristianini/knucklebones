package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.*

@Composable
fun HomePage(onStartGame: (Boolean, Difficulty?) -> Unit) {
    val context = LocalContext.current
    var showDifficultySelection by remember { mutableStateOf(false) }
    
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }
    val maxUnlocked = prefs.getInt("max_unlocked_difficulty", 0)

    // Load the mipmap icon and convert it to a Bitmap at runtime to avoid crashes
    val iconBitmap = remember {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        drawable?.let {
            val bitmap = Bitmap.createBitmap(
                it.intrinsicWidth.coerceAtLeast(1),
                it.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBrown)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!showDifficultySelection) {
            iconBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Knucklebones Icon",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "KNUCKLEBONES",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = GoldBrown,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "ANCIENT GAME OF CHANCE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LightCream,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            Button(
                onClick = { onStartGame(false, null) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RichBrown,
                    contentColor = Cream
                )
            ) {
                Text("PLAYER VS PLAYER", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showDifficultySelection = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkerBrown,
                    contentColor = Cream
                )
            ) {
                Text("PLAYER VS AI", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "SELECT DIFFICULTY",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = GoldBrown,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Difficulty.values().forEach { difficulty ->
                val isUnlocked = difficulty.order <= maxUnlocked
                
                Button(
                    onClick = { if (isUnlocked) onStartGame(true, difficulty) },
                    enabled = isUnlocked,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when(difficulty) {
                            Difficulty.Easy -> DarkGreen
                            Difficulty.Medium -> RichBrown
                            Difficulty.Hard -> DarkerBrown
                            Difficulty.Expert -> VeryDarkBrown
                        },
                        contentColor = Cream,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledContentColor = Color.DarkGray
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(difficulty.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (!isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { showDifficultySelection = false }) {
                Text("BACK", color = LightCream, fontWeight = FontWeight.Bold)
            }
        }
    }
}
