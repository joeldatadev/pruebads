package com.hoshiraflow.app

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
import com.hoshiraflow.app.levelselect.LevelSelectionScreen
import com.hoshiraflow.app.mainmenu.MainMenuScreen
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.hoshiraflow.app.ads.BannerAdView
import com.hoshiraflow.app.ads.InterstitialAdManager
import com.hoshiraflow.app.game.GameScreen
import com.hoshiraflow.app.settings.SettingsScreen
import com.hoshiraflow.data.daily.DailyChallengeDataStore
import com.hoshiraflow.data.daily.DailyChallengeRepositoryImpl
import com.hoshiraflow.data.level.LevelDataSource
import com.hoshiraflow.data.level.LevelRepositoryImpl
import com.hoshiraflow.data.progress.ProgressDataStore
import com.hoshiraflow.data.progress.ProgressRepositoryImpl
import com.hoshiraflow.data.settings.SettingsDataStore
import com.hoshiraflow.data.settings.SettingsRepositoryImpl
import com.hoshiraflow.domain.ads.AdFrequencyController
import java.time.LocalDate
import com.hoshiraflow.app.daily.DailyChallengeWorker
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts

private sealed class Screen {
    object MainMenu : Screen()
    object LevelSelection : Screen()
    data class Campaign(val levelId: Int) : Screen()
    data class Infinite(val index: Int, val seed: Long, val shape: com.hoshiraflow.domain.model.BoardShape? = null) : Screen()
    data class Portal(val index: Int, val seed: Long, val shape: com.hoshiraflow.domain.model.BoardShape? = null) : Screen()
    data class Switch(val index: Int, val seed: Long, val shape: com.hoshiraflow.domain.model.BoardShape? = null) : Screen()
    data class Master(val index: Int, val seed: Long, val shape: com.hoshiraflow.domain.model.BoardShape? = null) : Screen()
    data class IsometricCampaign(val levelId: Int) : Screen()
    data class IsometricInfinite(val index: Int, val seed: Long) : Screen()
    object SimpleCube : Screen()
    object EmptyCube : Screen()
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

        DailyChallengeWorker.enqueue(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Background Base (Slate Dark)
                ) {
                    var screen by remember { mutableStateOf<Screen>(Screen.MainMenu) }

                    fun proceedOrShowAd(boardWidth: Int, boardHeight: Int, navigate: () -> Unit) {
                        val shouldShowAd = adFrequencyController.onLevelCompleted(System.currentTimeMillis(), boardWidth, boardHeight)
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
                                        onNextLevel = {
                                            val w = 5
                                            val h = 5
                                            proceedOrShowAd(w, h) { screen = Screen.Campaign(current.levelId + 1) }
                                        },
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
                                        infiniteShape = current.shape,
                                        onNextLevel = {
                                            val w = current.shape?.width ?: 7
                                            val h = current.shape?.height ?: 7
                                            proceedOrShowAd(w, h) {
                                                screen = Screen.Infinite(current.index + 1, System.currentTimeMillis(), current.shape)
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.Portal -> {
                                    BackHandler { screen = Screen.LevelSelection }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        portalSeed = current.seed,
                                        portalShape = current.shape,
                                        onNextLevel = {
                                            val w = current.shape?.width ?: 7
                                            val h = current.shape?.height ?: 7
                                            proceedOrShowAd(w, h) {
                                                screen = Screen.Portal(current.index + 1, System.currentTimeMillis(), current.shape)
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.Switch -> {
                                    BackHandler { screen = Screen.LevelSelection }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        switchSeed = current.seed,
                                        switchShape = current.shape,
                                        onNextLevel = {
                                            val w = current.shape?.width ?: 7
                                            val h = current.shape?.height ?: 7
                                            proceedOrShowAd(w, h) {
                                                screen = Screen.Switch(current.index + 1, System.currentTimeMillis(), current.shape)
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.Master -> {
                                    BackHandler { screen = Screen.LevelSelection }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        masterSeed = current.seed,
                                        masterShape = current.shape,
                                        onNextLevel = {
                                            val w = current.shape?.width ?: 7
                                            val h = current.shape?.height ?: 7
                                            proceedOrShowAd(w, h) {
                                                screen = Screen.Master(current.index + 1, System.currentTimeMillis(), current.shape)
                                            }
                                        },
                                        onBackToLevelSelect = { screen = Screen.LevelSelection },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.IsometricCampaign -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = current.levelId,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        isIsometricCampaign = true,
                                        onNextLevel = { screen = Screen.IsometricCampaign(current.levelId + 1) },
                                        onBackToLevelSelect = { screen = Screen.MainMenu },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                is Screen.IsometricInfinite -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = current.index,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        infiniteSeed = current.seed,
                                        isIsometricInfinite = true,
                                        onNextLevel = { screen = Screen.IsometricInfinite(current.index + 1, System.currentTimeMillis()) },
                                        onBackToLevelSelect = { screen = Screen.MainMenu },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.SimpleCube -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = 1,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        isSimpleCube = true,
                                        onNextLevel = { screen = Screen.MainMenu },
                                        onBackToLevelSelect = { screen = Screen.MainMenu },
                                        onLevelRestarted = { onRestarted() },
                                        onGoToMainMenu = { screen = Screen.MainMenu }
                                    )
                                }
                                Screen.EmptyCube -> {
                                    BackHandler { screen = Screen.MainMenu }
                                    GameScreen(
                                        levelId = 1,
                                        levelRepository = levelRepository,
                                        progressRepository = progressRepository,
                                        dailyChallengeRepository = dailyChallengeRepository,
                                        settingsRepository = settingsRepository,
                                        isEmptyCube = true,
                                        onNextLevel = { screen = Screen.MainMenu },
                                        onBackToLevelSelect = { screen = Screen.MainMenu },
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
                                        onNextLevel = { 
                                            proceedOrShowAd(7, 7) { screen = Screen.MainMenu }
                                        },
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
                                    onInfiniteModeSelected = { shape ->
                                        screen = Screen.Infinite(index = 1, seed = System.currentTimeMillis(), shape = shape)
                                    },
                                    onDailyChallengeSelected = { screen = Screen.Daily },
                                    onSettingsSelected = { screen = Screen.Settings },
                                    onPortalModeSelected = { shape ->
                                        screen = Screen.Portal(index = 1, seed = System.currentTimeMillis(), shape = shape)
                                    },
                                    onSwitchModeSelected = { shape ->
                                        screen = Screen.Switch(index = 1, seed = System.currentTimeMillis(), shape = shape)
                                    },
                                    onMasterModeSelected = { shape ->
                                        screen = Screen.Master(index = 1, seed = System.currentTimeMillis(), shape = shape)
                                    },

                                    onSimpleCubeSelected = { screen = Screen.SimpleCube },
                                    onEmptyCubeSelected = { screen = Screen.EmptyCube }
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



