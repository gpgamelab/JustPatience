package com.gpgamelab.justpatience.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class TopRowRectResolverTest {

    private fun assertRect(
        rect: FloatRect,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        epsilon: Float = 0.001f
    ) {
        assertEquals(left, rect.left, epsilon)
        assertEquals(top, rect.top, epsilon)
        assertEquals(right, rect.right, epsilon)
        assertEquals(bottom, rect.bottom, epsilon)
    }

    @Test
    fun resolveWasteRect_classicAndMirrored_anchorToExpectedSide() {
        val drawWaste = FloatRect(10f, 20f, 210f, 420f)
        val cardW = 80f
        val cardH = 120f

        val classic = TopRowRectMath.resolveWasteRect(drawWaste, cardW, cardH, isMirrored = false)
        assertRect(classic, 10f, 20f, 90f, 140f)

        val mirrored = TopRowRectMath.resolveWasteRect(drawWaste, cardW, cardH, isMirrored = true)
        assertRect(mirrored, 130f, 20f, 210f, 140f)
    }

    @Test
    fun resolveStockRect_placesCardDirectlyBelowWasteWithGap() {
        val waste = FloatRect(50f, 30f, 130f, 150f)
        val stock = TopRowRectMath.resolveStockRect(
            wasteRect = waste,
            cardW = 80f,
            cardH = 120f,
            stackGap = 6f
        )
        assertRect(stock, 50f, 156f, 130f, 276f)
    }

    @Test
    fun resolveFoundationRect_landscapeOneDeck_usesColumnClosestToTableau() {
        val foundationGroup = FloatRect(400f, 40f, 580f, 600f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        // Classic landscape: closest column is left edge of group.
        val classic = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 4,
            index = 0,
            isLandscape = true,
            isMirrored = false
        )
        assertEquals(foundationGroup.left, classic.left, 0.001f)

        // Mirrored landscape: closest column is right edge minus card width.
        val mirrored = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 4,
            index = 0,
            isLandscape = true,
            isMirrored = true
        )
        assertEquals(foundationGroup.right - cardW, mirrored.left, 0.001f)
    }

    @Test
    fun resolveFoundationRect_portraitOneDeck_usesBottomRow() {
        val foundationGroup = FloatRect(100f, 30f, 500f, 330f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        val rect = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 4,
            index = 0,
            isLandscape = false,
            isMirrored = false
        )

        val totalRows = 2
        val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
        val centeredOffsetY = ((foundationGroup.height - totalHeight) / 2f).coerceAtLeast(0f)
        val expectedTop = foundationGroup.top + centeredOffsetY + (cardH + gap)
        assertEquals(expectedTop, rect.top, 0.001f)
    }

    @Test
    fun resolveFoundationRect_landscapeTwoDeck_secondColumnMovesAwayFromTableau() {
        val foundationGroup = FloatRect(400f, 40f, 620f, 600f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        val classicCol0 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 0,
            isLandscape = true,
            isMirrored = false
        )
        val classicCol1 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 4,
            isLandscape = true,
            isMirrored = false
        )
        assertEquals(cardW + gap, classicCol1.left - classicCol0.left, 0.001f)

        val mirroredCol0 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 0,
            isLandscape = true,
            isMirrored = true
        )
        val mirroredCol1 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 4,
            isLandscape = true,
            isMirrored = true
        )
        assertEquals(cardW + gap, mirroredCol0.left - mirroredCol1.left, 0.001f)
    }

    @Test
    fun resolveFoundationRect_portraitTwoDeck_rowMappingIsTopThenBottom() {
        val foundationGroup = FloatRect(100f, 30f, 500f, 360f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        val topRowIndex0 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 0,
            isLandscape = false,
            isMirrored = false
        )
        val bottomRowIndex4 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 4,
            isLandscape = false,
            isMirrored = false
        )

        assertEquals(cardH + gap, bottomRowIndex4.top - topRowIndex0.top, 0.001f)
        assertEquals(topRowIndex0.left, bottomRowIndex4.left, 0.001f)
    }

    @Test
    fun resolveFoundationRect_slotMathKeeps1DeckAnd2DeckMappingsStable() {
        val foundationGroup = FloatRect(100f, 30f, 500f, 360f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        val oneDeckLandscape = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 4,
            index = 3,
            isLandscape = true,
            isMirrored = false
        )
        val twoDeckLandscape = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 7,
            isLandscape = true,
            isMirrored = false
        )
        assertEquals(oneDeckLandscape.top, twoDeckLandscape.top, 0.001f)

        val oneDeckPortrait = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 4,
            index = 3,
            isLandscape = false,
            isMirrored = false
        )
        val twoDeckPortrait = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 7,
            isLandscape = false,
            isMirrored = false
        )
        assertEquals(oneDeckPortrait.top, twoDeckPortrait.top, 0.001f)
        assertEquals(oneDeckPortrait.left, twoDeckPortrait.left, 0.001f)
    }

    @Test
    fun resolveFoundationRect_portraitTwoDeck_mirroredReversesHorizontalOrder() {
        val foundationGroup = FloatRect(100f, 30f, 500f, 360f)
        val cardW = 70f
        val cardH = 100f
        val gap = 8f

        val classicIndex0 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 0,
            isLandscape = false,
            isMirrored = false
        )
        val mirroredIndex0 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 0,
            isLandscape = false,
            isMirrored = true
        )
        assertEquals(classicIndex0.left, mirroredIndex0.left, 0.001f)
        assertEquals(classicIndex0.top, mirroredIndex0.top, 0.001f)

        val classicIndex7 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 7,
            isLandscape = false,
            isMirrored = false
        )
        val mirroredIndex7 = TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationGroup,
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = 8,
            index = 7,
            isLandscape = false,
            isMirrored = true
        )
        assertEquals(classicIndex7.left, mirroredIndex7.left, 0.001f)
        assertEquals(classicIndex7.top, mirroredIndex7.top, 0.001f)
    }
}










