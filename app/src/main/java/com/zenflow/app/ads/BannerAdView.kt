package com.zenflow.app.ads

/**
 * Ruta destino: app/src/main/java/com/zenflow/app/ads/BannerAdView.kt (REEMPLAZA el archivo anterior)
 * Test ID oficial de banner: ca-app-pub-3940256099942544/6300978111
 *
 * Cambio clave: pasa de AdSize.BANNER fijo (320x50) a un Adaptive Banner
 * anclado, que Google recomienda desde 2022. Se calcula el ancho disponible
 * en tiempo real y el SDK devuelve la altura óptima (usualmente 50-90dp
 * según el dispositivo) -> mejor eCPM y proporción visual correcta en
 * tablets/pantallas grandes, sin cambiar nada del layout que ya tienes:
 *   Column {
 *       Box(Modifier.weight(1f)) { GameScreen(...) }
 *       BannerAdView(modifier = Modifier.padding(top = 16.dp))
 *   }
 */
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
private const val TAG = "ZenFlowBannerAd"

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // Adaptive Banner: se calcula UNA vez por ancho de pantalla (no en cada
    // frame) - remember(screenWidthDp) evita recrear el AdSize en cada
    // recomposición, solo cuando cambia la orientación/ventana.
    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    var adHeightDp by remember(adSize) { mutableStateOf<Dp?>(null) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .let { base -> adHeightDp?.let { h -> base.height(h) } ?: base },
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                adUnitId = TEST_BANNER_ID
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        adHeightDp = adSize.getHeightInPixels(ctx).let { px ->
                            (px / ctx.resources.displayMetrics.density).dp
                        }
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Banner falló al cargar: ${error.message} (código ${error.code})")
                        // No reintenta agresivamente: evita drenar cuota/red.
                        // El siguiente recomposition/pantalla intentará de nuevo.
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}