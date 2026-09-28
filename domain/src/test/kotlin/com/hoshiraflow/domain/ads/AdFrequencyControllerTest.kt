package com.hoshiraflow.domain.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdFrequencyControllerTest {

    @Test
    fun `no muestra ad antes de alcanzar el umbral de esfuerzo`() {
        val controller = AdFrequencyController()

        assertFalse(controller.onLevelCompleted(nowMs = 200_000, boardWidth = 5, boardHeight = 5))
        assertFalse(controller.onLevelCompleted(nowMs = 200_001, boardWidth = 5, boardHeight = 5))
    }

    @Test
    fun `muestra ad al alcanzar umbral de esfuerzo y cooldown`() {
        val controller = AdFrequencyController()

        // Acumular esfuerzo en un tiempo t > 180_000ms
        repeat(9) {
            controller.onLevelCompleted(nowMs = 200_000, boardWidth = 5, boardHeight = 5) // +1 por cada uno = 9
        }
        assertTrue(controller.onLevelCompleted(nowMs = 200_000, boardWidth = 5, boardHeight = 5)) // +1 = 10 -> trigger!
    }

    @Test
    fun `no muestra ad al reiniciar nivel`() {
        val controller = AdFrequencyController()
        assertFalse(controller.onLevelRestarted(nowMs = 1000))
    }
}
