package com.gpgamelab.justpatience.ui.layout

import android.graphics.RectF

/**
 * Phase 3 kickoff: centralizes Phase2 top-row card-rect derivation so GameBoardView
 * can progressively retire duplicated geometry math.
 */
object TopRowRectResolver {

    fun resolveWasteRect(
        drawWasteRect: RectF,
        cardW: Float,
        cardH: Float,
        isMirrored: Boolean
    ): RectF {
        val x = if (isMirrored) drawWasteRect.right - cardW else drawWasteRect.left
        val y = drawWasteRect.top
        return RectF(x, y, x + cardW, y + cardH)
    }

    fun resolveStockRect(
        wasteRect: RectF,
        cardW: Float,
        cardH: Float,
        stackGap: Float
    ): RectF {
        val x = wasteRect.left
        val y = wasteRect.bottom + stackGap
        return RectF(x, y, x + cardW, y + cardH)
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
        return if (isLandscape) {
            val row = if (foundationCount > 4) index % 4 else index
            val col = if (foundationCount > 4) index / 4 else 0
            val totalRows = 4
            val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
            val centeredOffsetY = ((foundationRect.height() - totalHeight) / 2f).coerceAtLeast(0f)
            val startY = foundationRect.top + centeredOffsetY
            val y = startY + row * (cardH + gap)

            val closestColumnX = if (isMirrored) foundationRect.right - cardW else foundationRect.left
            val x = if (isMirrored) {
                closestColumnX - col * (cardW + gap)
            } else {
                closestColumnX + col * (cardW + gap)
            }
            RectF(x, y, x + cardW, y + cardH)
        } else {
            val row = if (foundationCount > 4) index / 4 else 1
            val col = if (foundationCount > 4) index % 4 else index
            val totalCols = 4
            val totalRows = 2
            val totalWidth = (totalCols * cardW) + ((totalCols - 1) * gap)
            val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
            val centeredOffsetX = ((foundationRect.width() - totalWidth) / 2f).coerceAtLeast(0f)
            val centeredOffsetY = ((foundationRect.height() - totalHeight) / 2f).coerceAtLeast(0f)
            val startX = foundationRect.left + centeredOffsetX
            val startY = foundationRect.top + centeredOffsetY
            val x = startX + col * (cardW + gap)
            val y = startY + row * (cardH + gap)
            RectF(x, y, x + cardW, y + cardH)
        }
    }
}

