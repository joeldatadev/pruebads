package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameScreen.kt (REEMPLAZA el archivo anterior)
 * Fase "Celebración": Ripple Effect expandiéndose desde el centro del tablero
 * al completar el nivel (GameEvent.LevelCompleted) + LevelCompleteOverlay encima.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.zenflow.data.level.LevelRepositoryImpl
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.domain.repository.ProgressRepository
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    levelId: Int,
    levelRepository: LevelRepositoryImpl,
    progressRepository: ProgressRepository,
    infiniteSeed: Long? = null,
    onNextLevel: () -> Unit = {},
    onBackToLevelSelect: () -> Unit = {},
    onLevelRestarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(levelRepository, progressRepository))
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val hapticController = remember { HapticController(context) }

    remember(levelId, infiniteSeed) {
        if (infiniteSeed != null) viewModel.loadInfiniteLevel(levelId, infiniteSeed) else viewModel.loadLevel(levelId)
        true
    }

    val particles = remember { mutableStateMapOf<String, ParticleBurst>() }
    val nodeScales = remember { mutableStateMapOf<Cell, Animatable<Float, AnimationVector1D>>() }
    val rippleProgress = remember { Animatable(0f) }
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
                GameEvent.LevelCompleted -> {
                    hapticController.onLevelComplete()
                    scope.launch {
                        rippleProgress.snapTo(0f)
                        rippleProgress.animateTo(1f, tween(900, easing = LinearEasing))
                    }
                }
                GameEvent.InvalidMove -> hapticController.onInvalidMove()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.errorMessage != null -> Text("Error: ${uiState.errorMessage}")
            uiState.board != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onBackToLevelSelect) {
                            Text("← Niveles")
                        }
                        IconButton(onClick = {
                            hapticController.onInvalidMove() // pulso corto de confirmación
                            viewModel.restartLevel()
                            onLevelRestarted()
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar nivel")
                        }
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BoardLayers(
                            board = uiState.board!!,
                            connectedColors = uiState.connectedColors,
                            activeColor = uiState.activeColor,
                            nodeScales = nodeScales,
                            particles = particles.values.toList(),
                            rippleProgress = rippleProgress.value,
                            onNodeTouched = { color, cell ->
                                hapticController.onNodeTouch()
                                scope.launch { animatePulse(nodeScales, cell) }
                                viewModel.onNodeTouched(color, cell)
                            },
                            onDrag = viewModel::onDrag,
                            onDragEnd = viewModel::onDragEnd
                        )

                        LevelCompleteOverlay(
                            visible = uiState.isLevelComplete,
                            elapsedMs = uiState.elapsedMs,
                            onNextLevel = onNextLevel,
                            onBackToLevelSelect = onBackToLevelSelect
                        )
                    }
                }
            }
        }
    }
}

private suspend fun animatePulse(scales: MutableMap<Cell, Animatable<Float, AnimationVector1D>>, cell: Cell) {
    val anim = scales.getOrPut(cell) { Animatable(1f) }
    anim.animateTo(1.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    anim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
}

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
    rippleProgress: Float,
    activeColor: PuzzleColor?,
    onNodeTouched: (PuzzleColor, Cell) -> Unit,
    onDrag: (Cell) -> Unit,
    onDragEnd: () -> Unit
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    // Posición cruda del dedo (sin "snapear" a celda) - esto es lo que hace que
    // la línea se sienta fluida en vez de saltar cuadro a cuadro.
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    fun offsetToCell(offset: Offset): Cell? {
        if (cellSizePx <= 0f) return null
        val col = (offset.x / cellSizePx).toInt()
        val row = (offset.y / cellSizePx).toInt()
        if (row !in 0 until board.rows || col !in 0 until board.cols) return null
        return Cell(row, col)
    }

    fun clampToBoard(offset: Offset): Offset {
        val maxX = board.cols * cellSizePx
        val maxY = board.rows * cellSizePx
        return Offset(offset.x.coerceIn(0f, maxX), offset.y.coerceIn(0f, maxY))
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .aspectRatio(board.cols.toFloat() / board.rows.toFloat())
            .fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize().blur(18.dp)) {
            cellSizePx = size.width / board.cols
            drawPaths(board, cellSizePx, glow = true, activeColor, dragPosition)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(board) {
                    detectTapGestures(
                        onPress = { offset ->
                            val cell = offsetToCell(offset) ?: return@detectTapGestures
                            val node = board.nodes.firstOrNull { it.cell == cell }
                            if (node != null) onNodeTouched(node.color, cell)
                            dragPosition = offset
                        }
                    )
                }
                .pointerInput(board) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            change.consume()
                            dragPosition = clampToBoard(change.position)
                            offsetToCell(change.position)?.let(onDrag)
                        },
                        onDragEnd = {
                            dragPosition = null
                            onDragEnd()
                        },
                        onDragCancel = {
                            dragPosition = null
                            onDragEnd()
                        }
                    )
                }
        ) {
            cellSizePx = size.width / board.cols

            drawGrid(board, cellSizePx)
            drawPaths(board, cellSizePx, glow = false, activeColor, dragPosition)
            drawNodes(board, connectedColors, nodeScales, cellSizePx)
            drawParticles(particles, cellSizePx)
            if (rippleProgress in 0f..1f && rippleProgress > 0f) {
                drawRipple(board, cellSizePx, rippleProgress)
            }
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

private fun DrawScope.drawPaths(
    board: Board,
    cellSize: Float,
    glow: Boolean,
    activeColor: PuzzleColor?,
    dragPosition: Offset?
) {
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

    // Tramo "vivo": conecta la última celda confirmada con la posición REAL del
    // dedo (sin snapear). Esto es lo que hace que el arrastre se sienta fluido
    // en vez de saltar cuadro a cuadro.
    if (activeColor != null && dragPosition != null) {
        val cells = board.paths[activeColor]
        if (!cells.isNullOrEmpty()) {
            val baseColor = activeColor.toComposeColor()
            val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
            val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor
            drawLine(
                color = drawColor,
                start = cellCenter(cells.last(), cellSize),
                end = dragPosition,
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

/** Onda expansiva desde el centro del tablero, recorre todas las casillas al completar el nivel. */
private fun DrawScope.drawRipple(board: Board, cellSize: Float, progress: Float) {
    val boardCenter = Offset(board.cols * cellSize / 2f, board.rows * cellSize / 2f)
    val maxRadius = kotlin.math.hypot(board.cols * cellSize, board.rows * cellSize) / 2f

    // Dos anillos desfasados para dar sensación de onda con cuerpo, no un solo círculo plano
    listOf(0f, 0.15f).forEach { delay ->
        val localProgress = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (localProgress <= 0f) return@forEach
        val radius = maxRadius * localProgress
        val alpha = (1f - localProgress) * 0.5f
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = alpha),
            radius = radius,
            center = boardCenter,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = cellSize * 0.15f)
        )
    }
}

private fun cellCenter(cell: Cell, cellSize: Float): Offset =
    Offset(cell.col * cellSize + cellSize / 2f, cell.row * cellSize + cellSize / 2f)
