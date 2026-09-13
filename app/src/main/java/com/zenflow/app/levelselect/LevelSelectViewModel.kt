package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectViewModel.kt (REEMPLAZA el archivo anterior)
 * Cambio: agrega dailyChallengeRepository para mostrar en el menú si el
 * Reto del día ya se completó hoy (y la racha), no solo para poder entrar a jugarlo.
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.domain.repository.DailyChallengeRepository
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LevelSelectViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
        observeProgress()
        observeDailyChallenge()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            runCatching { levelRepository.getTotalLevels() }
                .onSuccess { total -> _uiState.value = _uiState.value.copy(isLoading = false, totalLevels = total) }
                .onFailure { e -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message) }
        }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            progressRepository.observeCompletedLevels().collect { completed ->
                _uiState.value = _uiState.value.copy(completedLevels = completed)
            }
        }
    }

    private fun observeDailyChallenge() {
        val todayEpochDay = LocalDate.now().toEpochDay()
        viewModelScope.launch {
            dailyChallengeRepository.observeState().collect { state ->
                _uiState.value = _uiState.value.copy(
                    dailyChallengeState = state,
                    isDailyChallengeCompletedToday = state.isCompletedOn(todayEpochDay)
                )
            }
        }
    }
}
