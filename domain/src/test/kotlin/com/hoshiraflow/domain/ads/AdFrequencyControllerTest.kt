package com.hoshiraflow.domain.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFrequencyControllerTest {

    @Test
    fun `no muestra ad antes de alcanzar el umbral de niveles`() {
        val controller = AdFrequencyController(levelsBetweenAds = 3, cooldownMs = 0)

        assertFalse(controller.onLevelCompleted(nowMs = 1000))
        assertFalse(controller.onLevelCompleted(nowMs = 2000))
    }

    @Test
    fun `muestra ad exactamente al llegar al umbral de niveles`() {
        val controller = AdFrequencyController(levelsBetweenAds = 3, cooldownMs = 0)

        controller.onLevelCompleted(nowMs = 1000)
        controller.onLevelCompleted(nowMs = 2000)
        assertTrue(controller.onLevelCompleted(nowMs = 3000))
    }

    @Test
    fun `muestra ad al llegar al umbral de reinicios`() {
        val controller = AdFrequencyController(restartsBetweenAds = 2, cooldownMs = 0)

        assertFalse(controller.onLevelRestarted(nowMs = 1000))
        assertTrue(controller.onLevelRestarted(nowMs = 2000))
    }

    @Test
    fun `respeta el cooldown - no muestra dos ads seguidos aunque se cumpla el umbral`() {
        val controller = AdFrequencyController(levelsBetweenAds = 1, cooldownMs = 45_000L)

        assertTrue(controller.onLevelCompleted(nowMs = 0)) // primer ad, sin cooldown previo
        // Inmediatamente después completa otro nivel - debería bloquear por cooldown
        assertFalse(controller.onLevelCompleted(nowMs = 5_000))
    }

    @Test
    fun `permite un nuevo ad una vez pasado el cooldown`() {
        val controller = AdFrequencyController(levelsBetweenAds = 1, cooldownMs = 45_000L)

        assertTrue(controller.onLevelCompleted(nowMs = 0))
        assertFalse(controller.onLevelCompleted(nowMs = 10_000)) // dentro del cooldown
        assertTrue(controller.onLevelCompleted(nowMs = 46_000))  // ya pasaron los 45s
    }

    @Test
    fun `los contadores se resetean despues de mostrar un ad`() {
        val controller = AdFrequencyController(levelsBetweenAds = 2, cooldownMs = 0)

        controller.onLevelCompleted(nowMs = 0)
        assertTrue(controller.onLevelCompleted(nowMs = 1000)) // dispara con 2 niveles

        // Debería necesitar 2 niveles MÁS desde cero, no solo 1
        assertFalse(controller.onLevelCompleted(nowMs = 2000))
        assertTrue(controller.onLevelCompleted(nowMs = 3000))
    }
}
