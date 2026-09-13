package com.zenflow.app.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenflow.data.level.LevelRepositoryImpl
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.ProgressRepository
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    levelId: Int,
    levelRepository: LevelRepositoryImpl,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    infiniteSeed: Long? = null,
    dailyEpochDay: Long? = null,
    onNextLevel: () -> Unit = {},
    onBackToLevelSelect: () -> Unit = {},
    onLevelRestarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel(
        key = "level=$levelId-infinite=$infiniteSeed-daily=$dailyEpochDay",
        factory = GameViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val hapticController = remember { HapticController(context) }

    remember(levelId, infiniteSeed, dailyEpochDay) {
        when {
            dailyEpochDay != null -> viewModel.loadDailyChallenge(dailyEpochDay)
            infiniteSeed != null -> viewModel.loadInfiniteLevel(levelId, infiniteSeed)
            else -> viewModel.loadLevel(levelId)
        }
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
                            Text(if (dailyEpochDay != null) "← Salir" else "← Niveles")
                        }
                        if (dailyEpochDay != null) {
                            Text("🔥 Reto Diario", color = Color(0xFFFFD700))
                        }
                        IconButton(onClick = {
                            hapticController.onInvalidMove()
                            viewModel.restartLevel()
                            onLevelRestarted()
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar nivel")
                        }
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BoardLayers(
                            board = uiState.board!!,
                            boardKey = "$levelId-$infiniteSeed-$dailyEpochDay",
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
                            onDrag = { cells ->
                                val color = viewModel.uiState.value.activeColor
                                val beforeLen = color?.let { viewModel.uiState.value.board?.paths?.get(it)?.size } ?: 0
                                viewModel.onDragBatch(cells)
                                val afterLen = color?.let { viewModel.uiState.value.board?.paths?.get(it)?.size } ?: 0
                                if (afterLen > beforeLen) hapticController.onCellAdded(afterLen)
                            },
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
    boardKey: Any,
    connectedColors: Set<PuzzleColor>,
    nodeScales: Map<Cell, Animatable<Float, AnimationVector1D>>,
    particles: List<ParticleBurst>,
    rippleProgress: Float,
    activeColor: PuzzleColor?,
    onNodeTouched: (PuzzleColor, Cell) -> Unit,
    onDrag: (List<Cell>) -> Unit,
    onDragEnd: () -> Unit
) {
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var lastRawPosition by remember { mutableStateOf<Offset?>(null) }

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

    fun collectCrossedCells(from: Offset, to: Offset): List<Cell> {
        val fromCell = offsetToCell(from) ?: return emptyList()
        val toCell = offsetToCell(to) ?: return emptyList()
        if (fromCell == toCell) return emptyList()

        val steps = mutableListOf<Cell>()
        var row = fromCell.row
        var col = fromCell.col
        while (row != toCell.row || col != toCell.col) {
            when {
                row != toCell.row -> row += if (toCell.row > row) 1 else -1
                else -> col += if (toCell.col > col) 1 else -1
            }
            steps.add(Cell(row, col))
        }
        return steps
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
                .pointerInput(boardKey) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val cell = offsetToCell(offset)
                            val node = cell?.let { c -> board.nodes.firstOrNull { it.cell == c } }
                            if (node != null) onNodeTouched(node.color, cell)
                            dragPosition = offset
                            lastRawPosition = offset
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val clamped = clampToBoard(change.position)
                            val previous = lastRawPosition ?: clamped
                            val crossed = collectCrossedCells(previous, clamped)
                            if (crossed.isNotEmpty()) onDrag(crossed)
                            dragPosition = clamped
                            lastRawPosition = clamped
                        },
                        onDragEnd = {
                            dragPosition = null
                            lastRawPosition = null
                            onDragEnd()
                        },
                        onDragCancel = {
                            dragPosition = null
                            lastRawPosition = null
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
        val isActiveDrag = color == activeColor && dragPosition != null && cells.isNotEmpty()
        if (cells.size < 2 && !isActiveDrag) return@forEach

        val path = Path()
        cells.forEachIndexed { i, cell ->
            val center = cellCenter(cell, cellSize)
            if (i == 0) path.moveTo(center.x, center.y) else path.lineTo(center.x, center.y)
        }

        if (isActiveDrag) {
            val lastCenter = cellCenter(cells.last(), cellSize)
            val dx = dragPosition!!.x - lastCenter.x
            val dy = dragPosition.y - lastCenter.y
            val snappedEnd = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                Offset(lastCenter.x + dx.coerceIn(-cellSize, cellSize), lastCenter.y)
            } else {
                Offset(lastCenter.x, lastCenter.y + dy.coerceIn(-cellSize, cellSize))
            }
            path.lineTo(snappedEnd.x, snappedEnd.y)
        }

        val baseColor = color.toComposeColor()
        val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
        val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor

        drawPath(
            path = path,
            color = drawColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
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

private fun DrawScope.drawRipple(board: Board, cellSize: Float, progress: Float) {
    val boardCenter = Offset(board.cols * cellSize / 2f, board.rows * cellSize / 2f)
    val maxRadius = kotlin.math.hypot(board.cols * cellSize, board.rows * cellSize) / 2f

    listOf(0f, 0.15f).forEach { delay ->
        val localProgress = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (localProgress <= 0f) return@forEach
        val radius = maxRadius * localProgress
        val alpha = (1f - localProgress) * 0.5f
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = alpha),
            radius = radius,
            center = boardCenter,
            style = Stroke(width = cellSize * 0.15f)
        )
    }
}

private fun cellCenter(cell: Cell, cellSize: Float): Offset =
    Offset(cell.col * cellSize + cellSize / 2f, cell.row * cellSize + cellSize / 2f)