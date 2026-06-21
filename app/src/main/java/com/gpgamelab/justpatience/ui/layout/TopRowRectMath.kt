package com.gpgamelab.justpatience.ui.layout

/**
 * Pure math helpers for top-row card rects.
 *
 * Kept Android-free so JVM unit tests can validate geometry without depending on
 * android.graphics.RectF behavior in local unit test runtime.
 */
data class FloatRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

object TopRowRectMath {

    private data class FoundationSlot(val row: Int, val col: Int)

    private fun resolveFoundationSlot(
        foundationCount: Int,
        index: Int,
        isLandscape: Boolean
    ): FoundationSlot {
        return if (isLandscape) {
            FoundationSlot(
                row = if (foundationCount > 4) index % 4 else index,
                col = if (foundationCount > 4) index / 4 else 0
            )
        } else {
            FoundationSlot(
                row = if (foundationCount > 4) index / 4 else 1,
                col = if (foundationCount > 4) index % 4 else index
            )
        }
    }

    fun resolveWasteRect(
        drawWasteRect: FloatRect,
        cardW: Float,
        cardH: Float,
        isMirrored: Boolean
    ): FloatRect {
        val x = if (isMirrored) drawWasteRect.right - cardW else drawWasteRect.left
        val y = drawWasteRect.top
        return FloatRect(x, y, x + cardW, y + cardH)
    }

    fun resolveStockRect(
        wasteRect: FloatRect,
        cardW: Float,
        cardH: Float,
        stackGap: Float
    ): FloatRect {
        val x = wasteRect.left
        val y = wasteRect.bottom + stackGap
        return FloatRect(x, y, x + cardW, y + cardH)
    }

    fun resolveFoundationRect(
        foundationRect: FloatRect,
        cardW: Float,
        cardH: Float,
        gap: Float,
        foundationCount: Int,
        index: Int,
        isLandscape: Boolean,
        isMirrored: Boolean
    ): FloatRect {
        return if (isLandscape) {
            val slot = resolveFoundationSlot(foundationCount, index, isLandscape = true)
            val totalRows = 4
            val totalHeight = (totalRows * cardH) + ((totalRows - 1) * gap)
            val centeredOffsetY = ((foundationRect.height - totalHeight) / 2f).coerceAtLeast(0f)
            val startY = foundationRect.top + centeredOffsetY
            val y = startY + slot.row * (cardH + gap)

            val closestColumnX = if (isMirrored) foundationRect.right - cardW else foundationRect.left
            val x = if (isMirrored) {
                closestColumnX - slot.col * (cardW + gap)
            } else {
                closestColumnX + slot.col * (cardW + gap)
            }
            FloatRect(x, y, x + cardW, y + cardH)
        } else {
            val slot = resolveFoundationSlot(foundationCount, index, isLandscape = false)
            val startY = foundationRect.top
            val startX = foundationRect.left
            val x = startX + slot.col * (cardW + gap)
            val y = startY + slot.row * (cardH + gap)
            FloatRect(x, y, x + cardW, y + cardH)
        }
    }
}


