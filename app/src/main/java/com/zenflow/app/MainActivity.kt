package com.zenflow.app

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/MainActivity.kt (REEMPLAZA el archivo anterior)
 *
 * Navegación simple con estado (sin Navigation-Compose todavía, para no meter
 * otra dependencia antes de tiempo). Cuando el juego crezca a más pantallas
 * (settings, tienda, etc.) migrar esto a NavHost sin tocar GameScreen/LevelSelectScreen.
 */
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zenflow.app.game.GameScreen
import com.zenflow.app.levelselect.LevelSelectScreen
import com.zenflow.data.level.LevelDataSource
import com.zenflow.data.level.LevelRepositoryImpl

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val levelRepository = LevelRepositoryImpl(LevelDataSource(applicationContext))

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var selectedLevelId by remember { mutableStateOf<Int?>(null) }

                    if (selectedLevelId != null) {
                        BackHandler { selectedLevelId = null }
                        GameScreen(
                            levelId = selectedLevelId!!,
                            levelRepository = levelRepository
                        )
                    } else {
                        LevelSelectScreen(
                            levelRepository = levelRepository,
                            onLevelSelected = { levelId -> selectedLevelId = levelId }
                        )
                    }
                }
            }
        }
    }
}