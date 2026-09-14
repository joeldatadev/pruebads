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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.zenflow.app.levelselect.LevelSelectionScreen
import com.zenflow.app.mainmenu.MainMenuScreen
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.zenflow.app.ads.BannerAdView
import com.zenflow.app.ads.InterstitialAdManager
import com.zenflow.app.game.GameScreen
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
    object MainMenu : Screen()
    object LevelSelection : Screen()
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Background Base (Slate Dark)
                ) {
                    var screen by remember { mutableStateOf<Screen>(Screen.MainMenu) }

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
                                    BackHandler { screen = Screen.LevelSelection }
                                    GameScreen(
                                        levelId = current.levelId,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        onNextLevel = { proceedOrShowAd { screen = Screen.Campaign(current.levelId + 1) } },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.Infinite -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        infiniteSeed = current.seed,
                                        onNextLevel = {
                                            proceedOrShowAd {
                                                screen = Screen.Infinite(current.index + 1, System.currentTimeMillis())
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.Daily -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = 0,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        dailyEpochDay = LocalDate.now().toEpochDay(),
                                        onNextLevel = { screen = Screen.MainMenu },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.Settings -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    SettingsScreen(
                                        settingsRepository = settingsRepository,
                                        onBack = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.LevelSelection -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    LevelSelectionScreen(
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        onLevelSelected = { levelId -> screen = Screen.Campaign(levelId) },
                                        onBack = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.MainMenu -> MainMenuScreen(
                                    levelRepository = levelRepository,
                                    progressRepository = progressRepository,
                                    dailyChallengeRepository = dailyChallengeRepository,
                                    onCampaignSelected = { screen = Screen.LevelSelection },
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
