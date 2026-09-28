package com.hoshiraflow.domain.usecase

import com.hoshiraflow.domain.util.CubeEdgeMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateCubeLevelUseCaseTest {
    private val gen = GenerateCubeLevelUseCase()

    @Test
    fun `los segmentos cubren todas las celdas y son caminos contiguos`() {
        for (blocks in 1..2) for (n in 3..6) for (index in 1..15) {
            val segments = gen.generateSegments(seed = 12345L, index = index, n = n, blocks = blocks)
            val all = segments.flatten()
            val msg = "n=$n blocks=$blocks index=$index"

            assertEquals(msg, 3 * n * n * blocks, all.size)
            assertEquals(msg, all.size, all.toSet().size)
            segments.forEach { seg ->
                assertTrue(msg, seg.size >= 2)
                seg.zipWithNext().forEach { (a, b) ->
                    assertTrue(msg, a.isAdjacentTo(b) || CubeEdgeMap.areAdjacent(a, b, n, blocks))
                }
            }
        }
    }
}
