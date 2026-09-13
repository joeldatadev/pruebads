package com.zenflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.zenflow.app.settings.SettingsScreen
import com.zenflow.data.daily.DailyChallengeDataStore
import com.zenflow.data.daily.DailyChallengeRepositoryImpl
import com.zenflow.data.level.LevelDataSource
import com.zenflow.data.level.LevelRepositoryImpl
import com.zenflow.data.progress.ProgressDataStore
import com.zenflow.data.progress.ProgressRepositoryImpl
import com.zenflow.data.settings.SettingsDataStore
import com.zenflow.data.settings.SettingsRepositoryImpl
import com.zenflow.domain.ads.AdFrequencyController
import java.time.LocalDate

private sealed class Screen {
    object LevelSelect : Screen()
    data class Campaign(val levelId: Int) : Screen()
    data class Infinite(val index: Int, val seed: Long) : Screen()
    object Daily : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this)

        val levelRepository = LevelRepositoryImpl(LevelDataSource(applicationContext))
        val progressRepository = ProgressRepositoryImpl(ProgressDataStore(applicationContext))
        val dailyChallengeRepository = DailyChallengeRepositoryImpl(DailyChallengeDataStore(applicationContext))
        val settingsRepository = SettingsRepositoryImpl(SettingsDataStore(applicationContext))
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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (val current = screen) {
                                is Screen.Campaign -> {
                                    BackHandler { screen = Screen.LevelSelect }
                                    GameScreen(
                                        levelId = current.levelId,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
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
                                        dailyChallengeRepository = dailyChallengeRepository,
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
                                Screen.Daily -> {
                                    BackHandler { screen = Screen.LevelSelect }
                                    GameScreen(
                                        levelId = 0,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        dailyEpochDay = LocalDate.now().toEpochDay(),
                                        onNextLevel = { screen = Screen.LevelSelect },
                                        onBackToLevelSelect = { screen = Screen.LevelSelect },
                                        onLevelRestarted = { onRestarted() }
                                    )
                                }
                                Screen.Settings -> {
                                    BackHandler { screen = Screen.LevelSelect }
                                    SettingsScreen(
                                        settingsRepository = settingsRepository,
                                        onBack = { screen = Screen.LevelSelect }
                                    )
                                }
                                Screen.LevelSelect -> LevelSelectScreen(
                                    levelRepository = levelRepository,
                                    progressRepository = progressRepository,
                                    dailyChallengeRepository = dailyChallengeRepository,
                                    onLevelSelected = { levelId -> screen = Screen.Campaign(levelId) },
                                    onInfiniteModeSelected = {
                                        screen = Screen.Infinite(index = 1, seed = System.currentTimeMillis())
                                    },
                                    onDailyChallengeSelected = { screen = Screen.Daily },
                                    onSettingsSelected = { screen = Screen.Settings }
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