package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameViewModel.kt (REEMPLAZA el archivo)
 * Cambio: agrega loadDailyChallenge() usando GetDailyChallengeSeedUseCase +
 * el mismo GenerateProceduralLevelUseCase del Modo Infinito. Al completar el
 * reto del día se llama a dailyChallengeRepository.markCompleted(epochDay)
 * (separado de progressRepository, que es solo para el Pack de niveles).
 * restartLevel() ahora reconoce los tres modos (campaña / infinito / diario).
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    fun loadDailyChallenge() {
        val daily = getDailyChallengeSeed()
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

    /** Recarga el mismo nivel/tablero, sea cual sea el modo activo (campaña, infinito o diario). */
    fun restartLevel() {
        currentInfinite?.let { (index, seed) -> loadInfiniteLevel(index, seed); return }
        if (currentDailyEpochDay != null) { loadDailyChallenge(); return }
        currentLevelId?.let { loadLevel(it) }
    }

    fun onNodeTouched(color: PuzzleColor, cell: Cell) {
        val board = _uiState.value.board ?: return
        val updatedBoard = board.withPath(color, listOf(cell))
        _uiState.value = _uiState.value.copy(board = updatedBoard, activeColor = color)
    }

    /**
     * Cuando el dedo se mueve rápido, Compose puede reportar la nueva posición
     * varias celdas más allá de la última registrada (salta intermedios). Si
     * solo validamos el salto directo, ValidateMoveUseCase lo rechaza por "no
     * adyacente" - pero la línea visual (que sigue la posición cruda del dedo
     * en GameScreen) igual se estira hasta ahí, dando el efecto de diagonal
     * que corta la cuadrícula. Arreglo: caminar celda por celda (escalera
     * ortogonal) entre la última celda confirmada y la nueva, validando cada
     * paso individualmente - así nunca se "salta" una celda intermedia.
     */
    fun onDrag(targetCell: Cell) {
        val color = _uiState.value.activeColor ?: return
        val lastCell = _uiState.value.board?.paths?.get(color)?.lastOrNull() ?: return
        if (lastCell == targetCell) return

        for (step in stepCellsTo(lastCell, targetCell)) {
            val board = _uiState.value.board ?: return
            when (val result = validateMove(board, color, step)) {
                is ValidateMoveUseCase.Result.Extend -> applyPath(board, color, result.newPath, step)
                is ValidateMoveUseCase.Result.Retreat -> applyPath(board, color, result.newPath, step)
                ValidateMoveUseCase.Result.Invalid -> {
                    _events.tryEmit(GameEvent.InvalidMove)
                    return // se detiene en el primer paso inválido, no sigue de largo
                }
            }
        }
    }

    /** Secuencia de celdas ortogonales (escalera: primero fila, luego columna) entre [from] y [to], sin incluir [from]. */
    private fun stepCellsTo(from: Cell, to: Cell): List<Cell> {
        val steps = mutableListOf<Cell>()
        var row = from.row
        var col = from.col
        while (row != to.row || col != to.col) {
            when {
                row != to.row -> row += if (to.row > row) 1 else -1
                else -> col += if (to.col > col) 1 else -1
            }
            steps.add(Cell(row, col))
        }
        return steps
    }

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
}