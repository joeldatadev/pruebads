package com.hoshiraflow.app.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PauseMenuOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGoToLevelSelect: () -> Unit,
    onGoToMainMenu: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .blur(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Empty box to catch clicks if needed, but blur is applied to background
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 400.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Pausa",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        PauseButton(
                            text = "Continuar",
                            icon = Icons.Filled.PlayArrow,
                            gradient = Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4))),
                            onClick = onResume
                        )

                        PauseButton(
                            text = "Reiniciar",
                            icon = Icons.Filled.Refresh,
                            isOutlined = true,
                            onClick = onRestart
                        )

                        PauseButton(
                            text = "Niveles",
                            icon = Icons.Filled.GridView,
                            onClick = onGoToLevelSelect
                        )

                        TextButton(
                            onClick = onGoToMainMenu,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Home, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Menú Principal", color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseButton(
    text: String,
    icon: ImageVector,
    gradient: Brush? = null,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    val buttonModifier = Modifier
        .fillMaxWidth()
        .height(56.dp)

    if (gradient != null) {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            border = borderStroke(1.dp, Color(0xFF475569))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)



