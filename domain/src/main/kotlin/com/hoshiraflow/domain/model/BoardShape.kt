package com.hoshiraflow.domain.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class BoardShape {
    abstract val width: Int
    abstract val height: Int

    abstract fun getCellTypes(): Map<Cell, CellType>

    data class SQUARE(val size: Int) : BoardShape() {
        override val width: Int = size
        override val height: Int = size
        override fun getCellTypes(): Map<Cell, CellType> = emptyMap()
    }

    data class RECTANGLE(override val width: Int, override val height: Int) : BoardShape() {
        override fun getCellTypes(): Map<Cell, CellType> = emptyMap()
    }

    data class HOURGLASS(override val width: Int, override val height: Int) : BoardShape() {
        override fun getCellTypes(): Map<Cell, CellType> {
            val map = mutableMapOf<Cell, CellType>()
            val midYStart = height / 3
            val midYEnd = height - 1 - (height / 3)
            val centerWidth = (width / 3).coerceAtLeast(1)
            val centerXStart = (width - centerWidth) / 2
            val centerXEnd = centerXStart + centerWidth - 1

            for (r in 0 until height) {
                for (c in 0 until width) {
                    if (r in midYStart..midYEnd) {
                        if (c < centerXStart || c > centerXEnd) {
                            map[Cell(r, c)] = CellType.Void
                        }
                    }
                }
            }
            return map
        }
    }
}
