package com.hoshiraflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameViewModelFactory.kt (REEMPLAZA el archivo)
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hoshiraflow.domain.repository.DailyChallengeRepository
import com.hoshiraflow.domain.repository.LevelRepository
import com.hoshiraflow.domain.repository.ProgressRepository

class GameViewModelFactory(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(levelRepository, progressRepository, dailyChallengeRepository) as T
    }
}



