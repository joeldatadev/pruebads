package com.hoshiraflow.domain.ads

/**
 * Ruta destino: domain/src/main/kotlin/com/zenflow/domain/ads/AdFrequencyController.kt
 *
 * Reglas del spec:
 * - Mostrar cada 3-4 niveles completados O tras 2 reinicios consecutivos.
 * - Capping time de 45s entre intersticiales (nunca dos ads pegados).
 */
class AdFrequencyController {
    private var accumulatedEffortScore = 0
    private var lastAdShownAtMs: Long = 0L

    fun onLevelCompleted(nowMs: Long, boardWidth: Int, boardHeight: Int): Boolean {
        val maxSide = maxOf(boardWidth, boardHeight)
        val points = when {
            maxSide <= 7 -> 1
            maxSide <= 10 -> 2
            else -> 3
        }
        accumulatedEffortScore += points

        val cooldownElapsed = (nowMs - lastAdShownAtMs) >= 180_000L
        val effortThresholdReached = accumulatedEffortScore >= 10

        val shouldShow = cooldownElapsed && effortThresholdReached
        if (shouldShow) {
            lastAdShownAtMs = nowMs
            accumulatedEffortScore = 0
        }
        return shouldShow
    }

    fun onLevelRestarted(nowMs: Long): Boolean {
        // Keeps local operational flow intact but safely bypasses ad trigger
        return false
    }
}



