package com.zenflow.app

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/MainActivity.kt (REEMPLAZA el archivo)
 * Cambio: Screen sellado con 3 estados (LevelSelect / Campaign / Infinite).
 * El intersticial y el banner funcionan igual en ambos modos de juego.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.zenflow.app.ads.BannerAdView
import com.zenflow.app.ads.InterstitialAdManager
import com.zenflow.app.game.GameScreen
import com.zenflow.app.levelselect.LevelSelectScreen
import com.zenflow.data.level.LevelDataSource
import com.zenflow.data.level.LevelRepositoryImpl
import com.zenflow.data.progress.ProgressDataStore
import com.zenflow.data.progress.ProgressRepositoryImpl
import com.zenflow.domain.ads.AdFrequencyController

private sealed class Screen {
    object LevelSelect : Screen()
    data class Campaign(val levelId: Int) : Screen()
    data class Infinite(val index: Int, val seed: Long) : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this)

        val levelRepository = LevelRepositoryImpl(LevelDataSource(applicationContext))
        val progressRepository = ProgressRepositoryImpl(ProgressDataStore(applicationContext))
        val interstitialAdManager = InterstitialAdManager(applicationContext)
        val adFrequencyController = AdFrequencyController()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.LevelSelect) }

                    fun proceedOrShowAd(navigate: () -> Unit) {
                        val shouldShowAd = adFrequencyController.onLevelCompleted(System.currentTimeMillis())
                        if (shouldShowAd) {
                            interstitialAdManager.showIfAvailable(activity = this@MainActivity, onDismissed = navigate)
                        } else {
                            navigate()
                        }
                    }

                    fun onRestarted() {
                        val shouldShowAd = adFrequencyController.onLevelRestarted(System.currentTimeMillis())
                        if (shouldShowAd) interstitialAdManager.showIfAvailable(activity = this@MainActivity)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (val current = screen) {
                                is Screen.Campaign -> {
                                    BackHandler { screen = Screen.LevelSelect }
                                    GameScreen(
                                        levelId = current.levelId,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        onNextLevel = { proceedOrShowAd { screen = Screen.Campaign(current.levelId + 1) } },
                                        onBackToLevelSelect = { screen = Screen.LevelSelect },
                                        onLevelRestarted = { onRestarted() }
                                    )
                                }
                                is Screen.Infinite -> {
                                    BackHandler { screen = Screen.LevelSelect }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        infiniteSeed = current.seed,
                                        onNextLevel = {
                                            proceedOrShowAd {
                                                screen = Screen.Infinite(current.index + 1, System.currentTimeMillis())
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelect },
                                        onLevelRestarted = { onRestarted() }
                                    )
                                }
                                Screen.LevelSelect -> LevelSelectScreen(
                                    levelRepository = levelRepository,
                                    progressRepository = progressRepository,
                                    onLevelSelected = { levelId -> screen = Screen.Campaign(levelId) },
                                    onInfiniteModeSelected = {
                                        screen = Screen.Infinite(index = 1, seed = System.currentTimeMillis())
                                    }
                                )
                            }
                        }

                        BannerAdView(modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}