package com.hoshiraflow.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshiraflow.domain.model.Board
import com.hoshiraflow.domain.model.Cell
import com.hoshiraflow.domain.model.Node
import com.hoshiraflow.domain.model.PuzzleColor
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository
import com.hoshiraflow.domain.usecase.CheckLevelCompleteUseCase
import com.hoshiraflow.domain.usecase.GenerateChallengeLevelUseCase
import com.hoshiraflow.domain.usecase.GenerateEmptyCubeUseCase
import com.hoshiraflow.domain.usecase.GenerateMasterLevelUseCase
import com.hoshiraflow.domain.usecase.GeneratePortalLevelUseCase
import com.hoshiraflow.domain.usecase.GenerateProceduralLevelUseCase
import com.hoshiraflow.domain.usecase.GenerateSwitchLevelUseCase
import com.hoshiraflow.domain.usecase.GetDailyChallengeSeedUseCase
import com.hoshiraflow.domain.usecase.ValidateMoveUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val generateProceduralLevel = GenerateProceduralLevelUseCase()
    private val generateChallengeLevel = GenerateChallengeLevelUseCase()
    private val generatePortalLevel = GeneratePortalLevelUseCase()
    private val generateSwitchLevel = GenerateSwitchLevelUseCase()
    private val generateMasterLevel = GenerateMasterLevelUseCase()
    private val generateEmptyCube = GenerateEmptyCubeUseCase()
    private val getDailyChallengeSeed = GetDailyChallengeSeedUseCase()
    private val validateMove = ValidateMoveUseCase()
    private val checkComplete = CheckLevelCompleteUseCase()

    private val generateTimedLevel = com.hoshiraflow.domain.usecase.GenerateTimedLevelUseCase()

    private var currentLevelId: Int? = null
    private var currentDailyEpochDay: Long? = null
    private var currentSurfaceKey: String? = null
    private var startTime: Long = 0

    private var timedType: com.hoshiraflow.domain.model.TimedModeType? = null
    private var timedRandom: kotlin.random.Random = kotlin.random.Random.Default
    private var timedElapsedSeconds: Int = 0
    private var timedCapMs: Long = 0L
    private var timedJob: Job? = null

    fun loadLevel(levelId: Int) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching { levelRepository.getLevel(levelId) }
                .onSuccess { board ->
                    _uiState.value = GameUiState(
                        isLoading = false,
                        board = board
                    )
                    startTime = System.currentTimeMillis()
                }
                .onFailure { e ->
                    _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
                }
        }
    }


    fun loadCubeLevel(levelId: Int) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching { levelRepository.getCubeLevel(levelId) }
                .onSuccess { board ->
                    _uiState.value = GameUiState(
                        isLoading = false,
                        board = board
                    )
                    startTime = System.currentTimeMillis()
                }
                .onFailure { e ->
                    _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun loadDailyChallenge(epochDay: Long) {
        currentLevelId = null
        currentDailyEpochDay = epochDay
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                val challenge = getDailyChallengeSeed(java.time.LocalDate.ofEpochDay(epochDay))
                generateProceduralLevel(seed = challenge.seed, index = challenge.index)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadSurfaceLevel(shapeIndex: Int, levelId: Int) {
        currentLevelId = null
        currentDailyEpochDay = null
        currentSurfaceKey = "${shapeIndex}_$levelId"
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching { levelRepository.getSurfaceLevel(shapeIndex, levelId) }
                .onSuccess { board ->
                    _uiState.value = GameUiState(isLoading = false, board = board)
                    startTime = System.currentTimeMillis()
                }
                .onFailure { e ->
                    _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
                }
        }
    }

    /**
     * Modo Infinito de superficies: no usa el catálogo fijo de SurfaceShapes.all.
     * N crece cada 3 niveles (tope 12) y alterna entre las dos familias paramétricas
     * (hipPyramid / diagonalRamp), ambas garantizadas sin solapamientos para cualquier N.
     * No marca progreso persistente (igual que loadDuoCube/loadEmptyCube): es infinito,
     * no hay "nivel completado" que guardar en disco.
     */
    fun loadInfiniteSurfaceLevel(index: Int, seed: Long) {
        currentLevelId = null
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                val n = (3 + index / 3).coerceAtMost(12)
                val model = if (index % 2 == 0) {
                    com.hoshiraflow.domain.model.SurfaceShapes.hipPyramid(n)
                } else {
                    com.hoshiraflow.domain.model.SurfaceShapes.diagonalRamp(n)
                }
                val numColors = (model.faces.size / 6).coerceIn(3, PuzzleColor.entries.size)
                var board: Board? = null
                for (attempt in 0 until 40) {
                    board = com.hoshiraflow.domain.usecase.GenerateSurfaceLevelUseCase()
                        .invoke(model, seed = seed + attempt, numColors = numColors)
                    if (board != null) break
                }
                board ?: error("No se pudo generar nivel infinito de superficie (n=$n)")
            }.onSuccess { board ->
                _uiState.value = GameUiState(isLoading = false, board = board)
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadInfiniteLevel(levelId: Int, seed: Long, shape: com.hoshiraflow.domain.model.BoardShape? = null) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                // For infinite mode, difficulty index scales with levelId
                generateProceduralLevel(seed = seed, index = levelId, shape = shape)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadChallengeLevel(levelId: Int, seed: Long) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateChallengeLevel(seed = seed, index = levelId)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadPortalLevel(levelId: Int, seed: Long = System.currentTimeMillis(), shape: com.hoshiraflow.domain.model.BoardShape? = null) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generatePortalLevel(seed = seed, index = levelId, shape = shape)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadSwitchLevel(levelId: Int, seed: Long = System.currentTimeMillis(), shape: com.hoshiraflow.domain.model.BoardShape? = null) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateSwitchLevel(seed = seed, index = levelId, shape = shape)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun loadMasterLevel(levelId: Int, seed: Long = System.currentTimeMillis(), shape: com.hoshiraflow.domain.model.BoardShape? = null) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateMasterLevel(seed = seed, index = levelId, shape = shape)
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }


    // ==========================================
    // HERRAMIENTAS DE DIAGNOSTICO
    // ==========================================

    fun loadDuoCube(levelId: Int, n: Int = 4) {
        currentLevelId = null
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching { com.hoshiraflow.domain.usecase.GenerateCubeLevelUseCase().invoke(seed = 777L, index = levelId, n = n, blocks = 2) }
                .onSuccess { _uiState.value = GameUiState(isLoading = false, board = it); startTime = System.currentTimeMillis() }
                .onFailure { _uiState.value = GameUiState(isLoading = false, errorMessage = it.message) }
        }
    }

    fun loadEmptyCube() {
        currentLevelId = null
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateEmptyCube.invoke(4) // 4x4 faces
            }.onSuccess { board ->
                _uiState.value = GameUiState(
                    isLoading = false,
                    board = board
                )
                startTime = System.currentTimeMillis()
            }.onFailure { e ->
                _uiState.value = GameUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    /**
     * Arranca Resistencia o Sprint 60. El reloj corre siempre, sin pausas, desde que
     * se arma el primer tablero hasta que llega a 0 (se detiene solo mientras genera
     * el siguiente nivel, lo cual es prácticamente instantáneo).
     */
    fun loadTimedMode(type: com.hoshiraflow.domain.model.TimedModeType, seed: Long) {
        currentLevelId = null
        currentDailyEpochDay = null
        currentSurfaceKey = null
        timedJob?.cancel()
        timedType = type
        timedRandom = kotlin.random.Random(seed)
        timedElapsedSeconds = 0

        val initialMs = when (type) {
            is com.hoshiraflow.domain.model.TimedModeType.Resistencia -> when (type.difficulty) {
                com.hoshiraflow.domain.model.TimedDifficulty.FACIL,
                com.hoshiraflow.domain.model.TimedDifficulty.MEDIO -> 5 * 60_000L
                com.hoshiraflow.domain.model.TimedDifficulty.DIFICIL -> 4 * 60_000L
                com.hoshiraflow.domain.model.TimedDifficulty.EXTREMO -> 3 * 60_000L
            }
            com.hoshiraflow.domain.model.TimedModeType.Sprint60 -> 45_000L
        }
        // Resistencia no suma tiempo, así que su "techo" es el mismo arranque fijo.
        timedCapMs = if (type is com.hoshiraflow.domain.model.TimedModeType.Sprint60) 60_000L else initialMs

        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            val bestScore = progressRepository.observeTimedBestScore(type.storageKey()).firstOrNull() ?: 0
            val level = generateTimedLevel.next(type, elapsedSeconds = 0, levelsSolved = 0, random = timedRandom)
            _uiState.value = GameUiState(
                isLoading = false,
                board = level.board,
                timedRemainingMs = initialMs,
                timedScore = 0,
                timedLevelsSolved = 0,
                timedBestScore = bestScore
            )
            startTime = System.currentTimeMillis()
            startTimedCountdown(initialMs)
        }
    }

    private fun startTimedCountdown(initialMs: Long) {
        timedJob = viewModelScope.launch {
            var remaining = initialMs
            val tick = 100L
            while (remaining > 0 && timedType != null) {
                delay(tick)
                remaining = (remaining - tick).coerceAtLeast(0)
                timedElapsedSeconds = ((timedCapMs - remaining).coerceAtLeast(0) / 1000).toInt()
                _uiState.value = _uiState.value.copy(timedRemainingMs = remaining)
            }
            if (timedType != null) finishTimedMode()
        }
    }

    private fun finishTimedMode() {
        timedJob?.cancel()
        val type = timedType ?: return
        val finalScore = _uiState.value.timedScore
        _uiState.value = _uiState.value.copy(timedFinished = true, timedRemainingMs = 0)
        viewModelScope.launch {
            progressRepository.saveTimedBestScoreIfHigher(type.storageKey(), finalScore)
        }
    }

    /** Llamado desde checkProgress en vez del flujo normal cuando estamos en modo cronometrado. */
    private fun advanceTimedMode() {
        val type = timedType ?: return
        val colors = _uiState.value.board?.nodes?.map { it.color }?.distinct()?.size ?: 0
        val points = colors * 10
        val newScore = _uiState.value.timedScore + points
        val newLevelsSolved = _uiState.value.timedLevelsSolved + 1

        val level = generateTimedLevel.next(
            type,
            elapsedSeconds = timedElapsedSeconds,
            levelsSolved = newLevelsSolved,
            random = timedRandom
        )
        val newRemaining = if (type is com.hoshiraflow.domain.model.TimedModeType.Sprint60) {
            ((_uiState.value.timedRemainingMs ?: 0L) + level.timeBonusMs).coerceAtMost(timedCapMs)
        } else {
            _uiState.value.timedRemainingMs
        }

        _uiState.value = _uiState.value.copy(
            board = level.board,
            connectedColors = emptySet(),
            activeColor = null,
            isLevelComplete = false,
            timedScore = newScore,
            timedLevelsSolved = newLevelsSolved,
            timedRemainingMs = newRemaining
        )
        startTime = System.currentTimeMillis()
    }

    fun onNodeTouched(color: PuzzleColor, cell: Cell) {
        val board = _uiState.value.board ?: return
        if (_uiState.value.isLevelComplete) return
        if (color in _uiState.value.connectedColors) return

        val path = board.paths[color] ?: emptyList()
        val node = board.nodes.firstOrNull { it.cell == cell && it.color == color }

        val newBoard = when {
            // Resume: Si tocamos la punta del camino, no cambiamos nada, solo activamos el color
            path.isNotEmpty() && path.last() == cell -> {
                board
            }
            // Reinicio: Si tocamos el nodo inicial y ya había camino, reiniciamos a solo ese nodo
            node != null && path.firstOrNull() == cell -> {
                board.withPath(color, listOf(cell))
            }
            // Trim/Resume: Si tocamos una casilla que ya está en el camino, recortamos hasta ahí
            path.contains(cell) -> {
                val index = path.indexOf(cell)
                board.withPath(color, path.subList(0, index + 1))
            }
            // Inicio: Si tocamos un nodo y no hay camino, empezamos
            node != null -> {
                board.withPath(color, listOf(cell))
            }
            else -> board
        }

        _uiState.value = _uiState.value.copy(
            board = newBoard,
            activeColor = color
        )
        
        if (newBoard !== board) {
            checkProgress(newBoard, color, cell)
        }
    }

    fun onDragBatch(cells: List<Cell>) {
        var board = _uiState.value.board ?: return
        val color = _uiState.value.activeColor ?: return
        if (_uiState.value.isLevelComplete) return
        if (color in _uiState.value.connectedColors) return

        for (cell in cells) {
            val result = validateMove(board, color, cell)
            when (result) {
                is ValidateMoveUseCase.Result.Extend -> {
                    board = board.withPath(color, result.newPath)
                    // FIX: checkProgress puede disparar advanceTimedMode() y dejar un
                    // tablero NUEVO en _uiState.value. Si eso pasó, hay que salir ya
                    // mismo: la línea de abajo (_uiState.value = ...copy(board=board))
                    // seguía pisando ese tablero nuevo con esta variable local vieja
                    // (el nivel que acaba de resolverse), y el reloj seguía corriendo
                    // sin que nunca se viera/jugara el siguiente nivel.
                    if (checkProgress(board, color, cell)) return
                    // If we just connected the color, stop dragging for this batch
                    if (color in _uiState.value.connectedColors) break
                }
                is ValidateMoveUseCase.Result.MutateColor -> {
                    // Actualizamos el tablero con el camino hasta el interruptor
                    board = board.withPath(color, result.newPath)
                    if (checkProgress(board, color, cell)) return
                    
                    // Mutamos el color activo para que el drag continúe con el nuevo color
                    _uiState.value = _uiState.value.copy(
                        board = board,
                        activeColor = result.newColor
                    )
                    // Si el color cambió, debemos actualizar la referencia local para el siguiente cell en el batch
                    onDragBatch(cells.subList(cells.indexOf(cell) + 1, cells.size))
                    return
                }
                is ValidateMoveUseCase.Result.Retreat -> {
                    board = board.withPath(color, result.newPath)
                    if (checkProgress(board, color, cell)) return
                }
                ValidateMoveUseCase.Result.Invalid -> {
                    viewModelScope.launch { _events.emit(GameEvent.InvalidMove) }
                    break
                }
            }
        }

        _uiState.value = _uiState.value.copy(board = board)
    }

    fun onDragEnd() {
        _uiState.value = _uiState.value.copy(activeColor = null)
    }

    fun clearColorPath(color: PuzzleColor) {
        val board = _uiState.value.board ?: return
        if (color in _uiState.value.connectedColors) return

        val newBoard = board.clearPath(color)
        _uiState.value = _uiState.value.copy(board = newBoard)
        checkProgress(newBoard, color, null)
    }

    fun restartLevel() {
        val board = _uiState.value.board ?: return
        val clearedBoard = board.copy(paths = emptyMap())
        _uiState.value = _uiState.value.copy(
            board = clearedBoard,
            connectedColors = emptySet(),
            isLevelComplete = false,
            elapsedMs = null
        )
        startTime = System.currentTimeMillis()
    }

    /** @return true si se acaba de cambiar a un tablero nuevo de modo cronometrado. */
    private fun checkProgress(board: Board, color: PuzzleColor, atCell: Cell?): Boolean {
        val isConnected = checkComplete.isColorConnected(board, color)
        val wasConnected = color in _uiState.value.connectedColors

        val newConnectedColors = if (isConnected) {
            _uiState.value.connectedColors + color
        } else {
            _uiState.value.connectedColors - color
        }

        if (isConnected && !wasConnected && atCell != null) {
            viewModelScope.launch {
                _events.emit(GameEvent.NodeSnapped(color, atCell))
                _events.emit(GameEvent.ColorCompleted(color, atCell))
            }
        }

        val isLevelComplete = checkComplete.isLevelComplete(board)

        if (isLevelComplete && timedType != null) {
            // Modo cronometrado: sin overlay de "¡Completado!" ni ripple (se comen
            // segundos) — pasa directo al siguiente tablero. El burst de partículas
            // por color (ColorCompleted) ya se emitió arriba, así que sigue habiendo
            // feedback visual en cada línea.
            advanceTimedMode()
            return true
        }

        if (isLevelComplete && !_uiState.value.isLevelComplete) {
            val elapsed = System.currentTimeMillis() - startTime
            _uiState.value = _uiState.value.copy(
                connectedColors = newConnectedColors,
                isLevelComplete = true,
                elapsedMs = elapsed
            )
            viewModelScope.launch {
                _events.emit(GameEvent.LevelCompleted)
                currentLevelId?.let { id ->
                    if (board.topology == com.hoshiraflow.domain.model.BoardTopology.CUBE) {
                        progressRepository.markCubeLevelCompleted(id)
                    } else {
                        progressRepository.markLevelCompleted(id)
                    }
                }
                currentDailyEpochDay?.let { dailyChallengeRepository.markCompleted(it) }
                currentSurfaceKey?.let { progressRepository.markSurfaceLevelCompleted(it) }
            }
        } else {
            _uiState.value = _uiState.value.copy(connectedColors = newConnectedColors)
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        timedJob?.cancel()
    }
}



