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
import com.hoshiraflow.domain.usecase.GenerateSimpleCubeUseCase
import com.hoshiraflow.domain.usecase.GenerateSwitchLevelUseCase
import com.hoshiraflow.domain.usecase.GenerateIsometricLevelUseCase
import com.hoshiraflow.domain.usecase.GetDailyChallengeSeedUseCase
import com.hoshiraflow.domain.usecase.ValidateMoveUseCase
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
    private val generateIsometricLevel = GenerateIsometricLevelUseCase()
    private val generateSimpleCube = GenerateSimpleCubeUseCase()
    private val generateEmptyCube = GenerateEmptyCubeUseCase()
    private val getDailyChallengeSeed = GetDailyChallengeSeedUseCase()
    private val validateMove = ValidateMoveUseCase()
    private val checkComplete = CheckLevelCompleteUseCase()

    private var currentLevelId: Int? = null
    private var currentDailyEpochDay: Long? = null
    private var startTime: Long = 0

    fun loadLevel(levelId: Int) {
        currentLevelId = levelId
        currentDailyEpochDay = null
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

    fun loadIsometricCampaignLevel(levelId: Int) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching { levelRepository.getIsometricLevel(levelId) }
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

    fun loadInfiniteLevel(levelId: Int, seed: Long, shape: com.hoshiraflow.domain.model.BoardShape? = null) {
        currentLevelId = levelId
        currentDailyEpochDay = null
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

    fun loadIsometricLevel(levelId: Int, seed: Long = System.currentTimeMillis()) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateIsometricLevel(seed = seed, index = levelId)
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

    fun loadSimpleCube(seed: Long = System.currentTimeMillis()) {
        currentLevelId = null
        currentDailyEpochDay = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                generateSimpleCube(seed = seed, index = 1)
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

    fun loadEmptyCube() {
        currentLevelId = null
        currentDailyEpochDay = null
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
                    checkProgress(board, color, cell)
                    // If we just connected the color, stop dragging for this batch
                    if (color in _uiState.value.connectedColors) break
                }
                is ValidateMoveUseCase.Result.MutateColor -> {
                    // Actualizamos el tablero con el camino hasta el interruptor
                    board = board.withPath(color, result.newPath)
                    checkProgress(board, color, cell)
                    
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
                    checkProgress(board, color, cell)
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

    private fun checkProgress(board: Board, color: PuzzleColor, atCell: Cell?) {
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
            }
        } else {
            _uiState.value = _uiState.value.copy(connectedColors = newConnectedColors)
        }
    }
}



