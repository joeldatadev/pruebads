package com.hoshiraflow.app.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.CellType
import com.hoshiraflow.domain.model.PuzzleColor
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.GameSettings
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository
import com.hoshiraflow.domain.repository.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    levelId: Int,
    levelRepository: LevelRepository,
    progressRepository: ProgressRepository,
    dailyChallengeRepository: DailyChallengeRepository,
    settingsRepository: SettingsRepository,
    infiniteSeed: Long? = null,
    infiniteShape: com.hoshiraflow.domain.model.BoardShape? = null,
    dailyEpochDay: Long? = null,
    portalSeed: Long? = null,
    portalShape: com.hoshiraflow.domain.model.BoardShape? = null,
    switchSeed: Long? = null,
    switchShape: com.hoshiraflow.domain.model.BoardShape? = null,
    masterSeed: Long? = null,
    masterShape: com.hoshiraflow.domain.model.BoardShape? = null,
    isIsometricCampaign: Boolean = false,
    isIsometricInfinite: Boolean = false,
    onNextLevel: () -> Unit = {},
    onBackToLevelSelect: () -> Unit = {},
    onLevelRestarted: () -> Unit = {},
    onGoToMainMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel(
        key = "level=$levelId-infinite=$infiniteSeed-daily=$dailyEpochDay-portal=$portalSeed-switch=$switchSeed-master=$masterSeed-ishape=$infiniteShape-pshape=$portalShape-sshape=$switchShape-mshape=$masterShape",
        factory = GameViewModelFactory(levelRepository, progressRepository, dailyChallengeRepository)
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticController = remember { HapticController(context) }
    
    var isPaused by remember { mutableStateOf(false) }

    val settings by settingsRepository.observeSettings().collectAsState(initial = GameSettings())
    LaunchedEffect(settings.hapticsEnabled) {
        hapticController.enabled = settings.hapticsEnabled
    }

    LaunchedEffect(levelId, infiniteSeed, dailyEpochDay, portalSeed, switchSeed, masterSeed, isIsometricCampaign, isIsometricInfinite) {
        val selectedShape: com.hoshiraflow.domain.model.BoardShape? = when {
            infiniteSeed != null && !isIsometricInfinite -> infiniteShape
            portalSeed != null -> portalShape
            switchSeed != null -> switchShape
            masterSeed != null -> masterShape
            else -> null
        }
        when {
            dailyEpochDay != null -> viewModel.loadDailyChallenge(dailyEpochDay)
            isIsometricCampaign -> viewModel.loadIsometricCampaignLevel(levelId)
            isIsometricInfinite && infiniteSeed != null -> viewModel.loadIsometricLevel(levelId, infiniteSeed)
            infiniteSeed != null -> viewModel.loadInfiniteLevel(levelId, infiniteSeed, selectedShape)
            portalSeed != null -> viewModel.loadPortalLevel(levelId, portalSeed, selectedShape)
            switchSeed != null -> viewModel.loadSwitchLevel(levelId, switchSeed, selectedShape)
            masterSeed != null -> viewModel.loadMasterLevel(levelId, masterSeed, selectedShape)
            else -> viewModel.loadLevel(levelId)
        }
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
                val board = uiState.board!!
                val totalColors by remember(board) {
                    derivedStateOf { board.nodes.map { it.color }.distinct().size }
                }
                val progressText by remember(uiState.connectedColors, totalColors) {
                    derivedStateOf { "${uiState.connectedColors.size} / $totalColors" }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)) // Base background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isPaused = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pausa", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dailyEpochDay != null) {
                                Text("🔥 Daily ", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            } else {
                                Text("Nivel $levelId ", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    progressText,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                hapticController.onInvalidMove()
                                viewModel.restartLevel()
                                onLevelRestarted()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar nivel", tint = Color.White)
                        }
                    }

                    // Header clean block completed

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        BoardLayers(
                            board = board,
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
                                val beforeBoard = viewModel.uiState.value.board
                                val beforeLen = color?.let { beforeBoard?.paths?.get(it)?.size } ?: 0
                                
                                viewModel.onDragBatch(cells)
                                
                                val afterBoard = viewModel.uiState.value.board
                                val afterLen = color?.let { afterBoard?.paths?.get(it)?.size } ?: 0
                                
                                if (afterLen > beforeLen && color != null && beforeBoard != null && afterBoard != null) {
                                    val newCells = afterBoard.paths[color]!!.takeLast(afterLen - beforeLen)
                                    
                                    // Detectar Portales e Interruptores para Haptics
                                    var portalJumped = false
                                    var switchActivated = false
                                    
                                    newCells.forEach { cell ->
                                        val type = afterBoard.cellTypes[cell]
                                        if (type is CellType.Portal) portalJumped = true
                                        if (type is CellType.Switch) switchActivated = true
                                    }
                                    
                                    if (portalJumped) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (switchActivated) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    
                                    hapticController.onCellAdded(afterLen)
                                }
                            },
                            onDragEnd = viewModel::onDragEnd,
                            onClearColor = { color -> viewModel.clearColorPath(color) }
                        )

                        LevelCompleteOverlay(
                            visible = uiState.isLevelComplete,
                            elapsedMs = uiState.elapsedMs,
                            onNextLevel = onNextLevel,
                            onBackToLevelSelect = onBackToLevelSelect
                        )

                        PauseMenuOverlay(
                            visible = isPaused,
                            onResume = { isPaused = false },
                            onRestart = {
                                isPaused = false
                                viewModel.restartLevel()
                                onLevelRestarted()
                            },
                            onGoToLevelSelect = onBackToLevelSelect,
                            onGoToMainMenu = onGoToMainMenu
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
    onDragEnd: () -> Unit,
    onClearColor: (PuzzleColor) -> Unit
) {
    var cellWidthPx by remember { mutableFloatStateOf(0f) }
    var cellHeightPx by remember { mutableFloatStateOf(0f) }
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapCell by remember { mutableStateOf<Cell?>(null) }

    val currentBoardState by rememberUpdatedState(board)
    val currentConnectedState by rememberUpdatedState(connectedColors)

    // Pre-allocating paths to avoid Path() in DrawScope
    val pathMap = remember { mutableMapOf<Int, Path>() }

    var layoutWidth by remember { mutableFloatStateOf(0f) }
    var layoutHeight by remember { mutableFloatStateOf(0f) }

    fun offsetToCell(offset: Offset): Cell? {
        if (layoutWidth <= 0f || layoutHeight <= 0f) return null
        if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
            val originX = layoutWidth / 2f
            val originY = layoutHeight / 4f
            val layerHeight = cellSizePx * 0.5f
            return com.hoshiraflow.domain.util.IsometricProjection.getHitBlockFace(
                offset.x, offset.y, board, cellSizePx, cellSizePx, originX, originY, layerHeight
            )
        }
        val cellWidth = layoutWidth / board.width
        val cellHeight = layoutHeight / board.height
        val col = (offset.x / cellWidth).toInt()
        val row = (offset.y / cellHeight).toInt()
        if (row !in 0 until board.height || col !in 0 until board.width) return null
        return Cell(row, col)
    }

    fun offsetToCellClamped(offset: Offset): Cell? {
        if (layoutWidth <= 0f || layoutHeight <= 0f) return null
        if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
            val originX = layoutWidth / 2f
            val originY = layoutHeight / 4f
            val layerHeight = cellSizePx * 0.5f
            return com.hoshiraflow.domain.util.IsometricProjection.getHitBlockFace(
                offset.x, offset.y, board, cellSizePx, cellSizePx, originX, originY, layerHeight
            )
        }
        val cellWidth = layoutWidth / board.width
        val cellHeight = layoutHeight / board.height
        val col = (offset.x / cellWidth).toInt().coerceIn(0, board.width - 1)
        val row = (offset.y / cellHeight).toInt().coerceIn(0, board.height - 1)
        return Cell(row, col)
    }

    fun clampToBoard(offset: Offset): Offset {
        return Offset(offset.x.coerceIn(0f, layoutWidth), offset.y.coerceIn(0f, layoutHeight))
    }

    fun crossedCellsFrom(startCell: Cell, targetCell: Cell): List<Cell> {
        if (startCell == targetCell) return emptyList()
        val steps = mutableListOf<Cell>()
        var row = startCell.row
        var col = startCell.col
        while (row != targetCell.row || col != targetCell.col) {
            when {
                row != targetCell.row -> row += if (targetCell.row > row) 1 else -1
                else -> col += if (targetCell.col > col) 1 else -1
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
            layoutWidth = size.width
            layoutHeight = size.height
            cellWidthPx = size.width / board.width
            cellHeightPx = size.height / board.height
            cellSizePx = kotlin.math.min(cellWidthPx, cellHeightPx)
            drawPaths(board, cellWidthPx, cellHeightPx, cellSizePx, glow = true, activeColor, dragPosition, pathMap, layoutWidth, layoutHeight)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boardKey) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downCell = offsetToCell(down.position)
                        val now = System.currentTimeMillis()
                        
                        var touchedColor: PuzzleColor? = null
                        var lastTrackedCell: Cell? = null

                        if (downCell != null) {
                            val liveBoard = currentBoardState
                            val liveConnected = currentConnectedState
                            val node = liveBoard.nodes.firstOrNull { it.cell == downCell }
                            
                            val pathColor = liveBoard.paths.entries.firstOrNull { downCell in it.value }?.key
                            val candidate = node?.color ?: pathColor
                            
                            if (candidate != null && candidate !in liveConnected) {
                                val isDoubleTap = node != null && downCell == lastTapCell && (now - lastTapTime) < 300
                                if (isDoubleTap) {
                                    onClearColor(candidate)
                                } else {
                                    onNodeTouched(candidate, downCell)
                                    touchedColor = candidate
                                    lastTrackedCell = downCell
                                }
                            }
                            lastTapTime = now
                            lastTapCell = downCell
                        }
                        
                        dragPosition = down.position

                        var portalLockedCell: Cell? = null

                        drag(down.id) { change ->
                            change.consume()
                            val clamped = clampToBoard(change.position)
                            dragPosition = clamped
                            
                            val targetCell = offsetToCellClamped(clamped)
                            val color = touchedColor
                            
                            if (color != null && targetCell != null) {
                                // Gracefully ignore pointer movement over CellType.Void
                                if (currentBoardState.cellTypes[targetCell] == CellType.Void) {
                                    return@drag
                                }

                                // Handle Portal Lock to prevent drift
                                if (portalLockedCell != null) {
                                    if (targetCell == portalLockedCell) {
                                        return@drag // Finger is still inside the exit portal
                                    } else if (targetCell.isAdjacentTo(portalLockedCell!!)) {
                                        portalLockedCell = null // Explicit adjacent move detected, unlock
                                    } else {
                                        return@drag // Ignore diagonal or non-adjacent moves from exit
                                    }
                                }

                                val livePath = currentBoardState.paths[color]
                                val head = livePath?.lastOrNull() ?: lastTrackedCell
                                
                                if (head != null && targetCell != head) {
                                    val steps = crossedCellsFrom(head, targetCell)
                                    val validSteps = steps.filter { currentBoardState.cellTypes[it] != CellType.Void }
                                    if (validSteps.isNotEmpty()) {
                                        val beforeLen = currentBoardState.paths[color]?.size ?: 0
                                        onDrag(validSteps)
                                        
                                        // Detection of portal jump to freeze pointer vector
                                        val afterBoard = currentBoardState
                                        val afterLen = afterBoard.paths[color]?.size ?: 0
                                        if (afterLen > beforeLen + 1) {
                                            val lastCell = afterBoard.paths[color]?.lastOrNull()
                                            if (lastCell != null && afterBoard.cellTypes[lastCell] is CellType.Portal) {
                                                portalLockedCell = lastCell
                                            }
                                        }

                                        lastTrackedCell = targetCell
                                    }
                                }
                            }
                        }

                        dragPosition = null
                        onDragEnd()
                    }
                }
        ) {
            layoutWidth = size.width
            layoutHeight = size.height
            cellWidthPx = size.width / (if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) 10f else board.width.toFloat())
            cellHeightPx = size.height / (if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) 10f else board.height.toFloat())
            cellSizePx = kotlin.math.min(cellWidthPx, cellHeightPx)

            drawGrid(board, cellWidthPx, cellHeightPx, cellSizePx, layoutWidth, layoutHeight)
            drawPaths(board, cellWidthPx, cellHeightPx, cellSizePx, glow = false, activeColor, dragPosition, pathMap, layoutWidth, layoutHeight)
            drawNodes(board, connectedColors, nodeScales, cellWidthPx, cellHeightPx, cellSizePx, layoutWidth, layoutHeight)
            drawParticles(particles, cellWidthPx, cellHeightPx, cellSizePx, layoutWidth, layoutHeight)
            if (rippleProgress in 0f..1f && rippleProgress > 0f) {
                drawRipple(board, cellWidthPx, cellHeightPx, cellSizePx, rippleProgress)
            }
        }
    }
}

private fun DrawScope.drawGrid(
    board: Board, 
    cellWidth: Float, 
    cellHeight: Float, 
    cellSize: Float,
    layoutWidth: Float,
    layoutHeight: Float
) {
    val gridColor = Color(0xFF2A2A2A)
    val blockedBgColor = Color(0xFF1E293B)
    val voidBgColor = Color(0xFF0F172A) // Base dark background for non-playable Void cells

    if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
        val originX = layoutWidth / 2f
        val originY = layoutHeight / 4f
        val layerHeight = cellSize * 0.5f

        // 1. Draw ALL blocks first to ensure correct depth and silhouette
        val allCells = board.cellTypes.keys + board.nodes.map { it.cell } + board.paths.values.flatten()
        val sortedCells = allCells.distinct().sortedWith(compareBy({ it.z }, { it.row + it.col }))

        sortedCells.forEach { cell ->
            val type = board.cellTypes[cell] ?: CellType.Empty
            if (type == CellType.Void) return@forEach

            val heightZ = cell.z.toFloat()
            val screenPos = com.hoshiraflow.domain.util.IsometricProjection.gridToScreen(
                cell.col, cell.row, heightZ, cellSize, cellSize, originX, originY, layerHeight
            )
            val center = Offset(screenPos.first, screenPos.second)

            val w = cellSize * 0.8660254f
            val h = cellSize * 0.5f
            
            val topPath = Path().apply {
                moveTo(center.x, center.y - h)
                lineTo(center.x + w, center.y)
                lineTo(center.x, center.y + h)
                lineTo(center.x - w, center.y)
                close()
            }
            
            val leftWall = Path().apply {
                moveTo(center.x - w, center.y)
                lineTo(center.x, center.y + h)
                lineTo(center.x, center.y + h + layerHeight)
                lineTo(center.x - w, center.y + layerHeight)
                close()
            }
            val rightWall = Path().apply {
                moveTo(center.x + w, center.y)
                lineTo(center.x, center.y + h)
                lineTo(center.x, center.y + h + layerHeight)
                lineTo(center.x + w, center.y + layerHeight)
                close()
            }

            // High-end volumetric shading
            drawPath(leftWall, color = Color(0xFF0F172A)) // Dark side
            drawPath(rightWall, color = Color(0xFF1E293B)) // Medium side
            drawPath(topPath, color = if (type == CellType.Blocked) blockedBgColor else Color(0xFF1E293B).copy(alpha = 0.6f))
            
            // Neon Silhouette Borders
            val neonColor = Color(0xFF10B981).copy(alpha = 0.3f)
            drawPath(topPath, color = neonColor, style = Stroke(width = 1.5f))
            drawPath(leftWall, color = neonColor.copy(alpha = 0.2f), style = Stroke(width = 1f))
            drawPath(rightWall, color = neonColor.copy(alpha = 0.2f), style = Stroke(width = 1f))

            if (type == CellType.Blocked) {
                drawLine(gridColor, Offset(center.x - w*0.4f, center.y - h*0.4f), Offset(center.x + w*0.4f, center.y + h*0.4f), 3f)
                drawLine(gridColor, Offset(center.x + w*0.4f, center.y - h*0.4f), Offset(center.x - w*0.4f, center.y + h*0.4f), 3f)
            }
        }
        return
    }

    // 1. Draw each cell background and types
    for (row in 0 until board.height) {
        for (col in 0 until board.width) {
            val cell = Cell(row, col)
            val type = board.cellTypes[cell] ?: CellType.Empty
            val left = col * cellWidth
            val top = row * cellHeight
            val center = Offset(left + cellWidth / 2f, top + cellHeight / 2f)

            when (type) {
                CellType.Void -> {
                    drawRect(
                        color = voidBgColor,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                    )
                }
                CellType.Blocked -> {
                    drawRect(
                        color = blockedBgColor,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                    )

                    val paddingX = cellWidth * 0.35f
                    val paddingY = cellHeight * 0.35f
                    drawLine(
                        color = gridColor,
                        start = Offset(left + paddingX, top + paddingY),
                        end = Offset(left + cellWidth - paddingX, top + cellHeight - paddingY),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(left + cellWidth - paddingX, top + paddingY),
                        end = Offset(left + paddingX, top + cellHeight - paddingY),
                        strokeWidth = 3f
                    )
                }
                is CellType.Portal -> {
                    // Colores para distinguir portales según su ID
                    val portalColors = listOf(
                        Color(0xFF8B5CF6), // Violeta
                        Color(0xFFEC4899), // Rosa
                        Color(0xFF06B6D4), // Cyan
                        Color(0xFFF59E0B)  // Ámbar
                    )
                    val portalColor = portalColors[type.portalId % portalColors.size]
                    
                    val isOccupied = board.isCellOccupiedByAnyColor(cell) != null
                    val pulseAlpha = if (isOccupied) 0.5f else 0.3f
                    val pulseRadius = if (isOccupied) cellSize * 0.45f else cellSize * 0.42f

                    drawCircle(
                        color = portalColor.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = center
                    )
                    
                    drawCircle(
                        color = portalColor,
                        radius = cellSize * 0.35f,
                        center = center,
                        style = Stroke(width = cellSize * 0.08f)
                    )

                    drawCircle(
                        color = portalColor.copy(alpha = 0.6f),
                        radius = cellSize * 0.15f,
                        center = center
                    )
                }
                is CellType.Switch -> {
                    val targetColor = type.targetColor.toComposeColor()
                    val isOccupied = board.isCellOccupiedByAnyColor(cell) != null
                    
                    drawCircle(
                        color = if (isOccupied) Color(0xFF475569) else Color(0xFF334155),
                        radius = if (isOccupied) cellSize * 0.42f else cellSize * 0.4f,
                        center = center
                    )

                    drawCircle(
                        color = targetColor.copy(alpha = if (isOccupied) 0.6f else 0.4f),
                        radius = cellSize * 0.32f,
                        center = center,
                        style = Stroke(width = cellSize * 0.1f)
                    )

                    drawCircle(
                        color = targetColor,
                        radius = cellSize * 0.12f,
                        center = center
                    )
                    
                    for (i in 0 until 6) {
                        val angle = (i * 60) * (Math.PI / 180).toFloat()
                        val length = if (isOccupied) 0.25f else 0.22f
                        val start = center + Offset(
                            (kotlin.math.cos(angle) * cellSize * 0.12f).toFloat(),
                            (kotlin.math.sin(angle) * cellSize * 0.12f).toFloat()
                        )
                        val end = center + Offset(
                            (kotlin.math.cos(angle) * cellSize * length).toFloat(),
                            (kotlin.math.sin(angle) * cellSize * length).toFloat()
                        )
                        drawLine(
                            color = targetColor,
                            start = start,
                            end = end,
                            strokeWidth = if (isOccupied) 5f else 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                CellType.Empty -> { /* No dibujamos nada especial */ }
            }
        }
    }

    // 2. Draw only standard grid lines between valid playable cells
    for (row in 0..board.height) {
        val y = row * cellHeight
        for (col in 0 until board.width) {
            val cellAbove = Cell(row - 1, col)
            val cellBelow = Cell(row, col)
            val aboveIsPlayable = row > 0 && board.cellTypes[cellAbove] != CellType.Void
            val belowIsPlayable = row < board.height && board.cellTypes[cellBelow] != CellType.Void
            if (aboveIsPlayable || belowIsPlayable) {
                drawLine(gridColor, Offset(col * cellWidth, y), Offset((col + 1) * cellWidth, y), 2f)
            }
        }
    }

    for (col in 0..board.width) {
        val x = col * cellWidth
        for (row in 0 until board.height) {
            val cellLeft = Cell(row, col - 1)
            val cellRight = Cell(row, col)
            val leftIsPlayable = col > 0 && board.cellTypes[cellLeft] != CellType.Void
            val rightIsPlayable = col < board.width && board.cellTypes[cellRight] != CellType.Void
            if (leftIsPlayable || rightIsPlayable) {
                drawLine(gridColor, Offset(x, row * cellHeight), Offset(x, (row + 1) * cellHeight), 2f)
            }
        }
    }
}

private fun DrawScope.drawPaths(
    board: Board,
    cellWidth: Float,
    cellHeight: Float,
    cellSize: Float,
    glow: Boolean,
    activeColor: PuzzleColor?,
    dragPosition: Offset?,
    pathMap: MutableMap<Int, Path>,
    layoutWidth: Float,
    layoutHeight: Float
) {
    var pathCount = 0
    board.paths.forEach { (color, cells) ->
        val isActiveDrag = color == activeColor && dragPosition != null && cells.isNotEmpty()
        if (cells.size < 2 && !isActiveDrag) return@forEach

        var currentColor = color
        val firstType = board.cellTypes[cells.first()]
        if (firstType is CellType.Switch) {
            currentColor = firstType.targetColor
        }

        var currentPath = pathMap.getOrPut(pathCount++) { Path() }
        currentPath.rewind()
        
        var pathHasPoints = false
        if (cells.isNotEmpty()) {
            val center = cellCenter(cells[0], cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
            currentPath.moveTo(center.x, center.y)
            pathHasPoints = true
        }

        var pathColor = currentColor

        for (i in 1 until cells.size) {
            val prevCell = cells[i - 1]
            val cell = cells[i]
            val isPortalJump = if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
                !prevCell.isIsometricAdjacentTo(cell)
            } else {
                !prevCell.isAdjacentTo(cell)
            }

            if (currentColor != pathColor || isPortalJump) {
                if (pathHasPoints) {
                    val baseColor = pathColor.toComposeColor()
                    val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
                    val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor

                    drawPath(
                        path = currentPath,
                        color = drawColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                currentPath = pathMap.getOrPut(pathCount++) { Path() }
                currentPath.rewind()
                
                val startCenter = if (isPortalJump) {
                    cellCenter(cell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
                } else {
                    cellCenter(prevCell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
                }
                
                currentPath.moveTo(startCenter.x, startCenter.y)
                pathColor = currentColor
            }

            val center = cellCenter(cell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
            currentPath.lineTo(center.x, center.y)
            pathHasPoints = true

            val cellType = board.cellTypes[cell]
            if (cellType is CellType.Switch) {
                currentColor = cellType.targetColor
            }
        }

        if (isActiveDrag) {
            val lastCell = cells.last()
            
            if (currentColor != pathColor) {
                if (pathHasPoints) {
                    val baseColor = pathColor.toComposeColor()
                    val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
                    val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor

                    drawPath(
                        path = currentPath,
                        color = drawColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                currentPath = pathMap.getOrPut(pathCount++) { Path() }
                currentPath.rewind()
                val lastCenter = cellCenter(lastCell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
                currentPath.moveTo(lastCenter.x, lastCenter.y)
                pathColor = currentColor
            }

            val lastCenter = cellCenter(lastCell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
            val dx = dragPosition!!.x - lastCenter.x
            val dy = dragPosition.y - lastCenter.y
            
            val reachX = cellWidth * 0.5f
            val reachY = cellHeight * 0.5f
            
            if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
                // For isometric drag, we just point towards the finger
                currentPath.lineTo(dragPosition.x, dragPosition.y)
            } else {
                val snappedEnd = if (kotlin.math.abs(dx) / cellWidth >= kotlin.math.abs(dy) / cellHeight) {
                    Offset(lastCenter.x + dx.coerceIn(-reachX, reachX), lastCenter.y)
                } else {
                    Offset(lastCenter.x, lastCenter.y + dy.coerceIn(-reachY, reachY))
                }
                currentPath.lineTo(snappedEnd.x, snappedEnd.y)
            }
            pathHasPoints = true
        }

        if (pathHasPoints) {
            val baseColor = pathColor.toComposeColor()
            val strokeWidth = if (glow) cellSize * 0.55f else cellSize * 0.28f
            val drawColor = if (glow) baseColor.copy(alpha = 0.55f) else baseColor

            drawPath(
                path = currentPath,
                color = drawColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

private fun DrawScope.drawNodes(
    board: Board,
    connectedColors: Set<PuzzleColor>,
    nodeScales: Map<Cell, Animatable<Float, AnimationVector1D>>,
    cellWidth: Float,
    cellHeight: Float,
    cellSize: Float,
    layoutWidth: Float,
    layoutHeight: Float
) {
    board.nodes.forEach { node ->
        val center = cellCenter(node.cell, cellWidth, cellHeight, cellSize, board, layoutWidth, layoutHeight)
        
        if (board.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
            // Circular endpoints centered on top faces at their respective heightZ
            val baseRadius = if (node.color in connectedColors) cellSize * 0.35f else cellSize * 0.3f
            val scale = nodeScales[node.cell]?.value ?: 1f
            
            // Outer glow
            drawCircle(color = node.color.toComposeColor().copy(alpha = 0.4f), radius = baseRadius * scale * 1.2f, center = center)
            // Main node
            drawCircle(color = node.color.toComposeColor(), radius = baseRadius * scale, center = center)
            // Inner highlight
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = baseRadius * scale * 0.35f, center = center)
        } else {
            val baseRadius = if (node.color in connectedColors) cellSize * 0.38f else cellSize * 0.32f
            val scale = nodeScales[node.cell]?.value ?: 1f
            drawCircle(color = node.color.toComposeColor(), radius = baseRadius * scale, center = center)
        }
    }
}

private fun DrawScope.drawParticles(
    particles: List<ParticleBurst>, 
    cellWidth: Float, 
    cellHeight: Float, 
    cellSize: Float,
    layoutWidth: Float,
    layoutHeight: Float
) {
    val particleCount = 8
    particles.forEach { burst ->
        val progress = burst.progress.value
        if (progress <= 0f) return@forEach
        val center = cellCenter(burst.cell, cellWidth, cellHeight, cellSize, null, layoutWidth, layoutHeight)
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

private fun DrawScope.drawRipple(board: Board, cellWidth: Float, cellHeight: Float, cellSize: Float, progress: Float) {
    val boardCenter = Offset(board.width * cellWidth / 2f, board.height * cellHeight / 2f)
    val maxRadius = kotlin.math.hypot(board.width * cellWidth, board.height * cellHeight) / 2f

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

private fun cellCenter(
    cell: Cell, 
    cellWidth: Float, 
    cellHeight: Float, 
    cellSize: Float, 
    board: Board?, 
    layoutWidth: Float, 
    layoutHeight: Float
): Offset {
    if (board?.topology == com.hoshiraflow.domain.model.BoardTopology.ISOMETRIC) {
        val originX = layoutWidth / 2f
        val originY = layoutHeight / 4f
        val layerHeight = cellSize * 0.5f
        val heightZ = cell.z.toFloat()
        val screenPos = com.hoshiraflow.domain.util.IsometricProjection.gridToScreen(
            cell.col, cell.row, heightZ, cellSize, cellSize, originX, originY, layerHeight
        )
        return Offset(screenPos.first, screenPos.second)
    }
    return Offset(cell.col * cellWidth + cellWidth / 2f, cell.row * cellHeight + cellHeight / 2f)
}



