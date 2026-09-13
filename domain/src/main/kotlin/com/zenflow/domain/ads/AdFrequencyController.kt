package com.zenflow.domain.ads

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/ads/AdFrequencyController.kt
 *
 * Reglas del spec:
 * - Mostrar cada 3-4 niveles completados O tras 2 reinicios consecutivos.
 * - Capping time de 45s entre intersticiales (nunca dos ads pegados).
 */
class AdFrequencyController(
    private val levelsBetweenAds: Int = 3,
    private val restartsBetweenAds: Int = 2,
    private val cooldownMs: Long = 45_000L
) {
    private var levelsCompletedSinceLastAd = 0
    private var restartsSinceLastAd = 0
    private var lastAdShownAtMs: Long = 0L

    fun onLevelCompleted(nowMs: Long): Boolean {
        levelsCompletedSinceLastAd++
        return maybeShow(nowMs)
    }

    fun onLevelRestarted(nowMs: Long): Boolean {
        restartsSinceLastAd++
        return maybeShow(nowMs)
    }

    private fun maybeShow(nowMs: Long): Boolean {
        val cooldownElapsed = nowMs - lastAdShownAtMs >= cooldownMs
        val triggeredByLevels = levelsCompletedSinceLastAd >= levelsBetweenAds
        val triggeredByRestarts = restartsSinceLastAd >= restartsBetweenAds

        val shouldShow = cooldownElapsed && (triggeredByLevels || triggeredByRestarts)
        if (shouldShow) {
            lastAdShownAtMs = nowMs
            levelsCompletedSinceLastAd = 0
            restartsSinceLastAd = 0
        }
        return shouldShow
    }
}
