package com.zenflow.app.ads

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/ads/InterstitialAdManager.kt
 *
 * IMPORTANTE: usa el Ad Unit ID de PRUEBA de Google. NO lo cambies por el real
 * hasta que la app esté lista para publicar - mostrar ads reales durante
 * desarrollo puede banear tu cuenta de AdMob.
 * Test ID oficial: ca-app-pub-3940256099942544/1033173712
 */
import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

class InterstitialAdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null

    init {
        loadAd()
    }

    private fun loadAd() {
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    /** Muestra el ad si ya está cargado; si no, no hace nada (evita bloquear al usuario). */
    fun showIfAvailable(activity: Activity, onDismissed: () -> Unit = {}) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadAd() // precarga el siguiente de inmediato
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadAd()
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
