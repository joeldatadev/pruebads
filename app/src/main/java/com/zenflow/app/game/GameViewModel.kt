package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/kotlin/com/zenflow/app/game/GameViewModel.kt
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.domain.model.Cell
import com.zenflow.domain.model.PuzzleColor
import com.zenflow.data.level.LevelRepository
import com.zenflow.domain.usecase.CheckLevelCompleteUseCase
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
    private val validateMove: ValidateMoveUseCase = ValidateMoveUseCase(),
    private val checkLevelComplete: CheckLevelCompleteUseCase = CheckLevelCompleteUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    fun loadLevel(levelId: Int) {
        _uiState.value = GameUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { levelRepository.getLevel(levelId) }
                .onSuccess { board -> _uiState.value = GameUiState(isLoading = false, board = board) }
                .onFailure { e -> _uiState.value = GameUiState(isLoading = false, errorMessage = e.message) }
        }
    }

    /** Llamar cuando el usuario TOCA un nodo inicial (dispara el Pulse Effect en presentation). */
    fun onNodeTouched(color: PuzzleColor, cell: Cell) {
        val board = _uiState.value.board ?: return
        val updatedBoard = board.withPath(color, listOf(cell))
        _uiState.value = _uiState.value.copy(board = updatedBoard, activeColor = color)
    }

    /** Llamar en cada nueva celda que el dedo cruza mientras arrastra. */
    fun onDrag(targetCell: Cell) {
        val state = _uiState.value
        val board = state.board ?: return
        val color = state.activeColor ?: return

        when (val result = validateMove(board, color, targetCell)) {
            is ValidateMoveUseCase.Result.Extend -> applyPath(board, color, result.newPath, targetCell)
            is ValidateMoveUseCase.Result.Retreat -> applyPath(board, color, result.newPath, targetCell)
            ValidateMoveUseCase.Result.Invalid -> _events.tryEmit(GameEvent.InvalidMove)
        }
    }

    /** Llamar cuando el usuario levanta el dedo. */
    fun onDragEnd() {
        _uiState.value = _uiState.value.copy(activeColor = null)
    }

    private fun applyPath(board: com.zenflow.domain.model.Board, color: PuzzleColor, newPath: List<Cell>, lastTouchedCell: Cell) {
        val updatedBoard = board.withPath(color, newPath)
        val wasConnected = color in _uiState.value.connectedColors
        val isNowConnected = checkLevelComplete.isColorConnected(updatedBoard, color)

        val updatedConnected = if (isNowConnected) {
            _uiState.value.connectedColors + color
        } else {
            _uiState.value.connectedColors - color
        }

        _uiState.value = _uiState.value.copy(board = updatedBoard, connectedColors = updatedConnected)

        if (isNowConnected && !wasConnected) {
            _events.tryEmit(GameEvent.NodeSnapped(color, lastTouchedCell))
            _events.tryEmit(GameEvent.ColorCompleted(color, lastTouchedCell))
        }

        if (checkLevelComplete.isLevelComplete(updatedBoard)) {
            _uiState.value = _uiState.value.copy(isLevelComplete = true)
            _events.tryEmit(GameEvent.LevelCompleted)
        }
    }
}
