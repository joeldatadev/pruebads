package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameScreen.kt (REEMPLAZA el archivo anterior)
 *
 * Fase 3 del roadmap. Arquitectura de render en 2 capas apiladas:
 *  - Canvas de GLOW (abajo): las mismas líneas pero más gruesas + Modifier.blur.
 *    Modifier.blur usa RenderEffect nativo en API 31+; en versiones anteriores
 *    Compose lo ignora silenciosamente (degradación agraciada, sin crash).
 *  - Canvas NÍTIDO (arriba): grid, líneas finas brillantes, nodos, partículas.
 *    También es el que captura los gestos.
 */
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.data.level.LevelRepository
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    levelId: Int,
    levelRepository: LevelRepository,
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(levelRepository))
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val hapticController = remember { HapticController(context) }

    remember(levelId) {
        viewModel.loadLevel(levelId)
        true
    }

    // Escucha eventos de un solo disparo (snap, partículas, haptics) - NO viven en el StateFlow
    val particles = remember { mutableStateMapOf<String, ParticleBurst>() }
    val nodeScales = remember { mutableStateMapOf<Cell, Animatable<Float, AnimationVector1D>>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.NodeSnapped -> {
                    hapticController.onColorConnected()
                    scope.launch { animateSnap(nodeScales, event.cell) }
                }
                is GameEvent.ColorCompleted -> {
                    val burstId = "${event.color}-${System.nanoTime()}"
                    particles[burstId] = ParticleBurst(cell = event.atCell, color = event.color)
                    scope.launch {
                        particles[burstId]?.progress?.animateTo(1f, tween(500, easing = LinearEasing))
                        particles.remove(burstId)
                    }
                }
                GameEvent.LevelCompleted -> hapticController.onLevelComplete()
                GameEvent.InvalidMove -> hapticController.onInvalidMove()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.errorMessage != null -> Text("Error: ${uiState.errorMessage}")
            uiState.board != null -> BoardLayers(
                board = uiState.board!!,
                connectedColors = uiState.connectedColors,
                nodeScales = nodeScales,
                particles = particles.values.toList(),
                onNodeTouched = { color, cell ->
                    hapticController.onNodeTouch()
                    scope.launch { animatePulse(nodeScales, cell) }
                    viewModel.onNodeTouched(color, cell)
                },
                onDrag = viewModel::onDrag,
                onDragEnd = viewModel::onDragEnd
            )
        }
    }
}

/** Bump de escala 1.0 -> 1.3 -> 1.0 al tocar un nodo inicial. */
private suspend fun animatePulse(scales: MutableMap<Cell, Animatable<Float, AnimationVector1D>>, cell: Cell) {
    val anim = scales.getOrPut(cell) { Animatable(1f) }
    anim.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
}

/** Mismo bump pero disparado cuando la línea ALCANZA el nodo destino (Snap Effect). */
private suspend fun animateSnap(scales: MutableMap<Cell, Animatable<Float, AnimationVector1D>>, cell: Cell) {
    val anim = scales.getOrPut(cell) { Animatable(1f) }
    anim.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
}

private data class ParticleBurst(
    val cell: Cell,
    val color: PuzzleColor,
    val progress: Animatable<Float, AnimationVector1D> = Animatable(0f)
)

@Composable
private fun BoardLayers(
    board: Board,
    connectedColors: Set<PuzzleColor>,
    nodeScales: Map<Cell, Animatable<Float, AnimationVector1D>>,
    particles: List<ParticleBurst>,
    onNodeTouched: (PuzzleColor, Cell) -> Unit,
    onDrag: (Cell) -> Unit,
    onDragEnd: () -> Unit
) {
    var cellSizePx by remember { mutableStateOf(0f) }

    fun offsetToCell(offset: Offset): Cell? {
        if (cellSizePx <= 0f) return null
        val col = (offset.x / cellSizePx).toInt()
        val row = (offset.y / cellSizePx).toInt()
        if (row !in 0 until board.rows || col !in 0 until board.cols) return null
        return Cell(row, col)
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .aspectRatio(board.cols.toFloat() / board.rows.toFloat())
            .fillMaxSize()
    ) {
        // Capa 1: GLOW (desenfocada, más ancha, más translúcida)
        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            cellSizePx = size.width / board.cols
            drawPaths(board, cellSizePx, glow = true)
        }

        // Capa 2: NÍTIDA (línea central + grid + nodos + partículas) + gestos
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board) {
                    detectTapGestures(
                        onPress = { offset ->
                            val cell = offsetToCell(offset) ?: return@detectTapGestures
                            val node = board.nodes.firstOrNull { it.cell == cell }
                            if (node != null) onNodeTouched(node.color, cell)
                        }
                    )
                }
                .pointerInput(board) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            offsetToCell(change.position)?.let(onDrag)
                        },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
        ) {
            cellSizePx = size.width / board.cols

            drawGrid(board, cellSizePx)
            drawPaths(board, cellSizePx, glow = false)
            drawNodes(board, connectedColors, nodeScales, cellSizePx)
            drawParticles(particles, cellSizePx)
        }
    }
}

private fun DrawScope.drawGrid(board: Board, cellSize: Float) {
    val gridColor = Color(0xFF2A2A2A)
    for (row in 0..board.rows) {
        drawLine(gridColor, Offset(0f, row * cellSize), Offset(board.cols * cellSize, row * cellSize), 2f)
    }
    for (col in 0..board.cols) {
        drawLine(gridColor, Offset(col * cellSize, 0f), Offset(col * cellSize, board.rows * cellSize), 2f)
    }
}

/**
 * glow = true  -> capa ancha y translúcida (se dibuja en el Canvas con blur)
 * glow = false -> capa central nítida y brillante
 */
private fun DrawScope.drawPaths(board: Board, cellSize: Float, glow: Boolean) {
    board.paths.forEach { (color, cells) ->
        if (cells.size < 2) return@forEach
        val baseColor = color.toComposeColor()
        val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
        val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor

        for (i in 0 until cells.size - 1) {
            drawLine(
                color = drawColor,
                start = cellCenter(cells[i], cellSize),
                end = cellCenter(cells[i + 1], cellSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawNodes(
    board: Board,
    connectedColors: Set<PuzzleColor>,
    nodeScales: Map<Cell, Animatable<Float, AnimationVector1D>>,
    cellSize: Float
) {
    board.nodes.forEach { node ->
        val center = cellCenter(node.cell, cellSize)
        val baseRadius = if (node.color in connectedColors) cellSize * 0.38f else cellSize * 0.32f
        val scale = nodeScales[node.cell]?.value ?: 1f
        drawCircle(color = node.color.toComposeColor(), radius = baseRadius * scale, center = center)
    }
}

/** Micro-partículas que explotan desde el nodo destino al conectar un color. */
private fun DrawScope.drawParticles(particles: List<ParticleBurst>, cellSize: Float) {
    val particleCount = 8
    particles.forEach { burst ->
        val progress = burst.progress.value
        if (progress <= 0f) return@forEach
        val center = cellCenter(burst.cell, cellSize)
        val color = burst.color.toComposeColor().copy(alpha = (1f - progress).coerceIn(0f, 1f))
        val travelDistance = cellSize * 0.9f * progress
        val particleRadius = cellSize * 0.06f * (1f - progress * 0.5f)

        for (i in 0 until particleCount) {
            val angle = (2 * Math.PI / particleCount) * i
            val dx = (kotlin.math.cos(angle) * travelDistance).toFloat()
            val dy = (kotlin.math.sin(angle) * travelDistance).toFloat()
            drawCircle(color = color, radius = particleRadius, center = center + Offset(dx, dy))
        }
    }
}

private fun cellCenter(cell: Cell, cellSize: Float): Offset =
    Offset(cell.col * cellSize + cellSize / 2f, cell.row * cellSize + cellSize / 2f)
