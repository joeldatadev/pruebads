package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectScreen.kt
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenflow.data.level.LevelRepository

@Composable
fun LevelSelectScreen(
    levelRepository: LevelRepository,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LevelSelectViewModel = viewModel(
        factory = LevelSelectViewModelFactory(levelRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.errorMessage != null -> Text("Error: ${uiState.errorMessage}")
            uiState.totalLevels == 0 -> Text("No hay niveles todavía")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                items(uiState.totalLevels) { index ->
                    val levelNumber = index + 1
                    LevelCell(
                        levelNumber = levelNumber,
                        isCompleted = levelNumber in uiState.completedLevels,
                        onClick = { onLevelSelected(levelNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCell(
    levelNumber: Int,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCompleted) Color(0xFF43A047) else Color(0xFF2A2A2A)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = levelNumber.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
