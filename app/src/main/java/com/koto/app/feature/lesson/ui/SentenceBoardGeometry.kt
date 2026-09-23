package com.koto.app.feature.lesson.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** Fixed destinations shared by layout and drag targeting, independent of animated tile positions. */
internal data class SentenceBoardGeometry(
    val columns: Int,
    val cellWidth: Int,
    val cellHeight: Int,
    val gap: Int,
    val sentenceTop: Int,
    val poolTop: Int,
    val tileCount: Int,
) {
    init {
        require(columns > 0)
        require(cellWidth > 0 && cellHeight > 0)
        require(gap >= 0)
        require(tileCount >= 0)
    }

    private val horizontalStride = cellWidth + gap
    private val verticalStride = cellHeight + gap
    private val reservedRows = (tileCount + columns - 1) / columns

    val sentenceBounds: Rect = Rect(
        left = 0f,
        top = sentenceTop.toFloat(),
        right = (columns * cellWidth + (columns - 1) * gap).toFloat(),
        bottom = (sentenceTop + reservedRows * cellHeight + (reservedRows - 1).coerceAtLeast(0) * gap).toFloat(),
    )

    fun sentenceSlot(index: Int): IntOffset = slot(index, sentenceTop)

    fun poolSlot(index: Int): IntOffset = slot(index, poolTop)

    /** The nearest logical selected slot; a short final row always resolves to an existing tile. */
    fun targetIndex(center: Offset, selectedCount: Int): Int {
        if (selectedCount <= 0) return 0
        val lastIndex = selectedCount - 1
        val lastRow = lastIndex / columns
        val column = nearestAxisSlot(center.x, cellWidth / 2f, horizontalStride, columns - 1)
        val row = nearestAxisSlot(center.y, sentenceTop + cellHeight / 2f, verticalStride, lastRow)
        return (row * columns + column).coerceAtMost(lastIndex)
    }

    private fun slot(index: Int, top: Int): IntOffset {
        require(index >= 0)
        return IntOffset(
            x = (index % columns) * horizontalStride,
            y = top + (index / columns) * verticalStride,
        )
    }

    private fun nearestAxisSlot(value: Float, firstCenter: Float, stride: Int, lastSlot: Int): Int {
        // A transient unspecified pointer coordinate must never make dragging crash.
        if (value.isNaN()) return 0
        return ((value - firstCenter) / stride).roundToInt().coerceIn(0, lastSlot)
    }
}
