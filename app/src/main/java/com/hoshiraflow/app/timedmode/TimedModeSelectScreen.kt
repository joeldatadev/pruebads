package com.hoshiraflow.app.timedmode

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoshiraflow.domain.model.TimedDifficulty
import com.hoshiraflow.domain.model.TimedModeType
import com.hoshiraflow.domain.repository.ProgressRepository

private val BackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
)

private data class DifficultyItem(
    val difficulty: TimedDifficulty,
    val label: String,
    val subtitle: String,
    val gradient: Brush
)

private val DIFFICULTIES = listOf(
    DifficultyItem(TimedDifficulty.FACIL, "Fácil", "5 min · tableros chicos",
        Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))),
    DifficultyItem(TimedDifficulty.MEDIO, "Medio", "5 min · tableros más grandes",
        Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF6366F1)))),
    DifficultyItem(TimedDifficulty.DIFICIL, "Difícil", "4 min · Maestro y Cubo",
        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEC4899)))),
    DifficultyItem(TimedDifficulty.EXTREMO, "Extremo", "3 min · todo mezclado",
        Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFF7C3AED))))
)

@Composable
fun TimedModeSelectScreen(
    progressRepository: ProgressRepository,
    onResistenciaSelected: (TimedDifficulty) -> Unit,
    onSprint60Selected: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundGradient)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF334155).copy(alpha = 0.5f))
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Contra Reloj",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Resuelve el mayor número de niveles antes de que se acabe el tiempo",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sprint 60: destacado, arriba de las dificultades de Resistencia.
        val sprintBest by progressRepository.observeTimedBestScore(TimedModeType.Sprint60.storageKey())
            .collectAsState(initial = 0)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFEF4444))))
                .clickable(onClick = onSprint60Selected)
                .padding(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("⚡ Sprint 60", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Arranca en 45s, suma tiempo, tope 60s", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White)
                    Text("Mejor: $sprintBest", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Resistencia",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        DIFFICULTIES.forEach { item ->
            val best by progressRepository.observeTimedBestScore(
                TimedModeType.Resistencia(item.difficulty).storageKey()
            ).collectAsState(initial = 0)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(item.gradient)
                    .clickable { onResistenciaSelected(item.difficulty) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(item.subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                    Text("Mejor: $best", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
