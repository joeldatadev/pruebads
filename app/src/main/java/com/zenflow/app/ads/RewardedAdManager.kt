package com.zenflow.app.ads

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/ads/RewardedAdManager.kt
 * Test ID oficial de rewarded: ca-app-pub-3940256099942544/5224354917
 */
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

enum class RewardType { HINT, AUTO_FILL_COLOR, EXTRA_LIFE }

class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null

    init {
        loadAd()
    }

    private fun loadAd() {
        RewardedAd.load(
            context,
            TEST_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    fun isReady(): Boolean = rewardedAd != null

    /** onRewardEarned solo se llama si el usuario vio el ad completo. */
    fun showForReward(activity: Activity, rewardType: RewardType, onRewardEarned: (RewardType) -> Unit, onDismissed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadAd()
                onDismissed()
            }
        }

        ad.show(activity) { _ -> onRewardEarned(rewardType) }
    }
}
