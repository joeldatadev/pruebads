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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de resultados de Resistencia / Sprint 60, al llegar el reloj a 0.
 * Reemplaza (no reemplaza, convive junto a) LevelCompleteOverlay: en modo
 * cronometrado nunca se muestra LevelCompleteOverlay, solo este.
 */
@Composable
fun TimedResultsOverlay(
    visible: Boolean,
    score: Int,
    bestScore: Int,
    levelsSolved: Int,
    isNewBest: Boolean,
    onBackToLevelSelect: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .blur(8.dp),
            contentAlignment = Alignment.Center
        ) {}

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "⏱ Se acabó el tiempo",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        if (isNewBest) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "¡Nuevo récord!",
                                color = Color(0xFFF59E0B),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "$score",
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "puntos",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatColumn(label = "Niveles", value = "$levelsSolved")
                            StatColumn(label = "Mejor", value = "$bestScore")
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onBackToLevelSelect,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF06B6D4)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.GridView, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Volver", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp)
    }
}
