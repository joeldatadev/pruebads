package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameViewModel.kt (REEMPLAZA el archivo completo)
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.domain.model.Board
import com.zenflow.domain.model.Cell
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
import java.time.LocalDate

class GameViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val validateMove: ValidateMoveUseCase = ValidateMoveUseCase(),
    private val checkLevelComplete: CheckLevelCompleteUseCase = CheckLevelCompleteUseCase(),
    private val generateProceduralLevel: GenerateProceduralLevelUseCase = GenerateProceduralLevelUseCase(),
    private val getDailyChallengeSeed: GetDailyChallengeSeedUseCase = GetDailyChallengeSeedUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private var currentLevelId: Int? = null              // solo modo campaña
    private var currentInfinite: Pair<Int, Long>? = null  // (index, seed) solo modo infinito
    private var currentDailyEpochDay: Long? = null        // solo modo Reto Diario
    private var startTimeMs: Long = 0L

    fun loadLevel(levelId: Int) {
        currentLevelId = levelId
        currentInfinite = null
        currentDailyEpochDay = null
        startTimeMs = System.currentTimeMillis()
        _uiState.value = GameUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { levelRepository.getLevel(levelId) }
                .onSuccess { board -> _uiState.value = GameUiState(isLoading = false, board = board) }
                .onFailure { e -> _uiState.value = GameUiState(isLoading = false, errorMessage = e.message) }
        }
    }

    fun loadInfiniteLevel(index: Int, seed: Long) {
        currentLevelId = null
        currentInfinite = index to seed
        currentDailyEpochDay = null
        startTimeMs = System.currentTimeMillis()
        _uiState.value = GameUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { generateProceduralLevel(seed = seed, index = index) }
                .onSuccess { board -> _uiState.value = GameUiState(isLoading = false, board = board) }
                .onFailure { e -> _uiState.value = GameUiState(isLoading = false, errorMessage = e.message) }
        }
    }

    /** Reto Diario: mismo tablero para todos los jugadores en la misma fecha (ver GetDailyChallengeSeedUseCase). */
    fun loadDailyChallenge(epochDay: Long? = null) {
        val daily = if (epochDay != null) {
            getDailyChallengeSeed(LocalDate.ofEpochDay(epochDay))
        } else {
            getDailyChallengeSeed()
        }

        currentLevelId = null
        currentInfinite = null
        currentDailyEpochDay = daily.epochDay
        startTimeMs = System.currentTimeMillis()
        _uiState.value = GameUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { generateProceduralLevel(seed = daily.seed, index = daily.index) }
                .onSuccess { board -> _uiState.value = GameUiState(isLoading = false, board = board) }
                .onFailure { e -> _uiState.value = GameUiState(isLoading = false, errorMessage = e.message) }
        }
    }

    fun restartLevel() {
        currentInfinite?.let { (index, seed) -> loadInfiniteLevel(index, seed); return }
        currentDailyEpochDay?.let { epochDay -> loadDailyChallenge(epochDay); return }
        currentLevelId?.let { loadLevel(it) }
    }
    fun clearColorPath(color: PuzzleColor) {
        val board = _uiState.value.board ?: return
        val node = board.nodes.firstOrNull { it.color == color } ?: return
        val updatedBoard = board.withPath(color, listOf(node.cell))
        _uiState.value = _uiState.value.copy(
            board = updatedBoard,
            connectedColors = _uiState.value.connectedColors - color
        )
    }

    fun onNodeTouched(color: PuzzleColor, cell: Cell) {
        val board = _uiState.value.board ?: return
        val existingPath = board.paths[color].orEmpty()
        val indexInPath = existingPath.indexOf(cell)

        val updatedBoard = if (indexInPath != -1) {
            // La celda ya es parte del camino de este color (nodo o intermedia,
            // incluye el caso donde el gesto se canceló al salir del tablero):
            // recorta el camino hasta ahí en vez de reiniciarlo a 1 celda.
            board.withPath(color, existingPath.subList(0, indexInPath + 1))
        } else {
            board.withPath(color, listOf(cell))
        }
        _uiState.value = _uiState.value.copy(board = updatedBoard, activeColor = color)
    }

    fun onDragBatch(cells: List<Cell>) {
        val board = _uiState.value.board ?: return
        val color = _uiState.value.activeColor ?: return
        var currentBoard = board
        var lastTouched: Cell? = null

        for (cell in cells) {
            when (val result = validateMove(currentBoard, color, cell)) {
                is ValidateMoveUseCase.Result.Extend -> {
                    currentBoard = currentBoard.withPath(color, result.newPath)
                    lastTouched = cell
                }
                is ValidateMoveUseCase.Result.Retreat -> {
                    currentBoard = currentBoard.withPath(color, result.newPath)
                    lastTouched = cell
                }
                ValidateMoveUseCase.Result.Invalid -> { /* ignora, sigue con la próxima */ }
            }
        }
        if (lastTouched != null) applyPath(currentBoard, color, currentBoard.paths[color].orEmpty(), lastTouched)
    }

    private fun applyPath(board: Board, color: PuzzleColor, path: List<Cell>, lastCell: Cell) {
        val updatedBoard = board.withPath(color, path)
        val wasConnected = color in _uiState.value.connectedColors
        val isNowConnected = checkLevelComplete.isColorConnected(updatedBoard, color)
        val updatedConnected = if (isNowConnected) {
            _uiState.value.connectedColors + color
        } else {
            _uiState.value.connectedColors - color
        }

        _uiState.value = _uiState.value.copy(board = updatedBoard, connectedColors = updatedConnected)

        if (isNowConnected && !wasConnected) {
            _events.tryEmit(GameEvent.NodeSnapped(color, lastCell))
            _events.tryEmit(GameEvent.ColorCompleted(color, lastCell))
        }

        if (checkLevelComplete.isLevelComplete(updatedBoard)) {
            val elapsed = System.currentTimeMillis() - startTimeMs
            _uiState.value = _uiState.value.copy(isLevelComplete = true, elapsedMs = elapsed)
            _events.tryEmit(GameEvent.LevelCompleted)

            currentLevelId?.let { levelId ->
                viewModelScope.launch { progressRepository.markLevelCompleted(levelId) }
            }
            currentDailyEpochDay?.let { epochDay ->
                viewModelScope.launch { dailyChallengeRepository.markCompleted(epochDay) }
            }
        }
    }

    fun onDragEnd() {
        _uiState.value = _uiState.value.copy(activeColor = null)
    }
}