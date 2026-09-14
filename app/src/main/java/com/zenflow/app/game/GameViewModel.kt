package com.zenflow.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.Node
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository
import com.zenflow.domain.usecase.CheckLevelCompleteUseCase
import com.zenflow.domain.usecase.GenerateProceduralLevelUseCase
import com.zenflow.domain.usecase.GetDailyChallengeSeedUseCase
import com.zenflow.domain.usecase.ValidateMoveUseCase
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

    fun loadInfiniteLevel(levelId: Int, seed: Long) {
        currentLevelId = levelId
        currentDailyEpochDay = null
        viewModelScope.launch {
            _uiState.value = GameUiState(isLoading = true)
            runCatching {
                // For infinite mode, difficulty index scales with levelId
                generateProceduralLevel(seed = seed, index = levelId)
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
                currentLevelId?.let { progressRepository.markLevelCompleted(it) }
                currentDailyEpochDay?.let { dailyChallengeRepository.markCompleted(it) }
            }
        } else {
            _uiState.value = _uiState.value.copy(connectedColors = newConnectedColors)
        }
    }
}
