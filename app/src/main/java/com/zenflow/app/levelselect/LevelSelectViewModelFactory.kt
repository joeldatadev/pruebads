package com.zenflow.app.levelselect

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/levelselect/LevelSelectViewModelFactory.kt (REEMPLAZA el archivo anterior)
 */
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zenflow.domain.repository.LevelRepository
import com.zenflow.domain.repository.ProgressRepository

class LevelSelectViewModelFactory(
    private val levelRepository: LevelRepository,
    private val progressRepository: ProgressRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LevelSelectViewModel(levelRepository, progressRepository) as T
    }
}
