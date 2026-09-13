package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectViewModel.kt
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenflow.data.level.LevelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LevelSelectViewModel(
    private val levelRepository: LevelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    init {
        loadLevels()
    }

    private fun loadLevels() {
        viewModelScope.launch {
            runCatching { levelRepository.getTotalLevels() }
                .onSuccess { total -> _uiState.value = LevelSelectUiState(isLoading = false, totalLevels = total) }
                .onFailure { e -> _uiState.value = LevelSelectUiState(isLoading = false, errorMessage = e.message) }
        }
    }
}
