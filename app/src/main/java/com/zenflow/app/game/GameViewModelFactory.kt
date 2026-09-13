package com.zenflow.app.game

/**
 * Ruta destino: app/src/main/kotlin/com/zenflow/app/game/GameViewModelFactory.kt
 *
 * Nota: esto es temporal mientras no metas Hilt/Koin. Cuando agregues DI,
 * este archivo se borra y GameViewModel se anota con @HiltViewModel;
 * el resto del código no cambia.
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenflow.domain.repository.LevelRepository

class GameViewModelFactory(
    private val levelRepository: LevelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return GameViewModel(levelRepository) as T
    }
}
