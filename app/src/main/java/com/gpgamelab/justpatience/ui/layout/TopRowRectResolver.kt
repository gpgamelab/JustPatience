package com.gpgamelab.justpatience.ui.layout

import android.graphics.RectF

/**
 * Phase 3 kickoff: centralizes Phase2 top-row card-rect derivation so GameBoardView
 * can progressively retire duplicated geometry math.
 */
object TopRowRectResolver {

    private fun RectF.toFloatRect(): FloatRect = FloatRect(
        left = left,
        top = top,
        right = right,
        bottom = bottom
    )

    private fun FloatRect.toRectF(): RectF = RectF(left, top, right, bottom)

    fun resolveWasteRect(
        drawWasteRect: RectF,
        cardW: Float,
        cardH: Float,
        isMirrored: Boolean
    ): RectF {
        return TopRowRectMath.resolveWasteRect(
            drawWasteRect = drawWasteRect.toFloatRect(),
            cardW = cardW,
            cardH = cardH,
            isMirrored = isMirrored
        ).toRectF()
    }

    fun resolveStockRect(
        wasteRect: RectF,
        cardW: Float,
        cardH: Float,
        stackGap: Float
    ): RectF {
        return TopRowRectMath.resolveStockRect(
            wasteRect = wasteRect.toFloatRect(),
            cardW = cardW,
            cardH = cardH,
            stackGap = stackGap
        ).toRectF()
    }

    fun resolveFoundationRect(
        foundationRect: RectF,
        cardW: Float,
        cardH: Float,
        gap: Float,
        foundationCount: Int,
        index: Int,
        isLandscape: Boolean,
        isMirrored: Boolean
    ): RectF {
        return TopRowRectMath.resolveFoundationRect(
            foundationRect = foundationRect.toFloatRect(),
            cardW = cardW,
            cardH = cardH,
            gap = gap,
            foundationCount = foundationCount,
            index = index,
            isLandscape = isLandscape,
            isMirrored = isMirrored
        ).toRectF()
    }
}


