package com.zenflow.app.ads

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/ads/BannerAdView.kt
 * Test ID oficial de banner: ca-app-pub-3940256099942544/6300978111
 *
 * Úsalo así, con padding EXTRA respecto al área jugable (spec pide mínimo 16dp
 * de margen para evitar clics accidentales):
 *   Column {
 *       Box(Modifier.weight(1f)) { GameScreen(...) }
 *       BannerAdView(modifier = Modifier.padding(top = 16.dp))
 *   }
 */
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
