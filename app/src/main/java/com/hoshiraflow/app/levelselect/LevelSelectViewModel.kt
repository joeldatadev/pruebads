package com.hoshiraflow.app.levelselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LevelSelectViewModel(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LevelSelectUiState())
    val uiState: StateFlow<LevelSelectUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadLevels()
        observeProgress()
        observeDailyChallenge()
        startCountdown()
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
        viewModelScope.launch {
            dailyChallengeRepository.observeState().collect { state ->
                val currentChallengeEpoch = dailyChallengeRepository.getCurrentChallengeEpochDay()
                _uiState.value = _uiState.value.copy(
                    dailyChallengeState = state,
                    isDailyChallengeCompletedToday = state.isCompletedOn(currentChallengeEpoch)
                )
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val nextReset = dailyChallengeRepository.getNextResetTimeMillis()
                val diff = nextReset - System.currentTimeMillis()
                
                if (diff <= 0) {
                    // Force refresh when countdown reaches zero
                    val state = _uiState.value.dailyChallengeState
                    val currentChallengeEpoch = dailyChallengeRepository.getCurrentChallengeEpochDay()
                    _uiState.value = _uiState.value.copy(
                        isDailyChallengeCompletedToday = state.isCompletedOn(currentChallengeEpoch)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    dailyChallengeCountdown = formatCountdown(diff.coerceAtLeast(0))
                )
                delay(1000)
            }
        }
    }

    private fun formatCountdown(millis: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(millis)
        val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}



