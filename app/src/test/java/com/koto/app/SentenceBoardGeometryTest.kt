package com.koto.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import com.koto.app.feature.lesson.ui.SentenceBoardGeometry
import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceBoardGeometryTest {
    private val geometry = SentenceBoardGeometry(
        columns = 3,
        cellWidth = 80,
        cellHeight = 48,
        gap = 8,
        sentenceTop = 20,
        poolTop = 180,
        tileCount = 5,
    )

    @Test fun logicalSlotsWrapAtTheSameColumnsInBothAreas() {
        assertEquals(IntOffset(0, 20), geometry.sentenceSlot(0))
        assertEquals(IntOffset(176, 20), geometry.sentenceSlot(2))
        assertEquals(IntOffset(0, 76), geometry.sentenceSlot(3))
        assertEquals(IntOffset(88, 76), geometry.sentenceSlot(4))
        assertEquals(IntOffset(0, 180), geometry.poolSlot(0))
        assertEquals(IntOffset(88, 236), geometry.poolSlot(4))
    }

    @Test fun draggingAcrossRowsReachesFrontMiddleAndEnd() {
        assertEquals(0, geometry.targetIndex(Offset(40f, 44f), selectedCount = 5))
        assertEquals(2, geometry.targetIndex(Offset(216f, 44f), selectedCount = 5))
        assertEquals(3, geometry.targetIndex(Offset(40f, 100f), selectedCount = 5))
        assertEquals(4, geometry.targetIndex(Offset(128f, 100f), selectedCount = 5))
    }

    @Test fun aShortLastRowAndOutsideCoordinatesResolveToExistingSlots() {
        assertEquals(4, geometry.targetIndex(Offset(216f, 100f), selectedCount = 5))
        assertEquals(0, geometry.targetIndex(Offset(-500f, -500f), selectedCount = 5))
        assertEquals(2, geometry.targetIndex(Offset(500f, -500f), selectedCount = 5))
        assertEquals(3, geometry.targetIndex(Offset(-500f, 500f), selectedCount = 5))
        assertEquals(4, geometry.targetIndex(Offset(500f, 500f), selectedCount = 5))
        assertEquals(1, geometry.targetIndex(Offset(216f, 500f), selectedCount = 2))
    }

    @Test fun crossingTheGapChangesTargetAtTheMidpointBetweenSlotCenters() {
        assertEquals(0, geometry.targetIndex(Offset(83.9f, 44f), selectedCount = 5))
        assertEquals(1, geometry.targetIndex(Offset(84f, 44f), selectedCount = 5))
        assertEquals(1, geometry.targetIndex(Offset(128f, 71.9f), selectedCount = 5))
        assertEquals(4, geometry.targetIndex(Offset(128f, 72f), selectedCount = 5))
    }

    @Test fun repeatedDragLocationsDoNotChangeWhenTilesWouldAnimate() {
        val pointer = Offset(40f, 100f)
        repeat(100) {
            assertEquals(3, geometry.targetIndex(pointer, selectedCount = 5))
        }
    }

    @Test fun sentenceBoundsReserveAllRowsRegardlessOfSelection() {
        assertEquals(Rect(0f, 20f, 256f, 124f), geometry.sentenceBounds)
        assertEquals(Rect(0f, 20f, 256f, 68f), geometry.copy(tileCount = 3).sentenceBounds)
        assertEquals(Rect(0f, 20f, 256f, 20f), geometry.copy(tileCount = 0).sentenceBounds)
    }

    @Test fun emptyAndSingleTileSelectionsHaveSafeTargets() {
        assertEquals(0, geometry.targetIndex(Offset(500f, 500f), selectedCount = 0))
        assertEquals(0, geometry.targetIndex(Offset(500f, 500f), selectedCount = 1))
        assertEquals(0, geometry.targetIndex(Offset.Unspecified, selectedCount = 5))
        assertEquals(4, geometry.targetIndex(Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), selectedCount = 5))
    }
}
