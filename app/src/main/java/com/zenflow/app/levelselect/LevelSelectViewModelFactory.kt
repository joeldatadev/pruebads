package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectViewModelFactory.kt
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenflow.domain.repository.LevelRepository

class LevelSelectViewModelFactory(
    private val levelRepository: LevelRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LevelSelectViewModel(levelRepository) as T
    }
}
