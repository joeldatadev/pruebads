package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/game/GameViewModelFactory.kt (REEMPLAZA el archivo)
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository

class GameViewModelFactory(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(levelRepository, progressRepository) as T
    }
}
