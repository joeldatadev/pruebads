package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectViewModel.kt (REEMPLAZA el archivo anterior)
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LevelSelectViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
        observeProgress()
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
}
