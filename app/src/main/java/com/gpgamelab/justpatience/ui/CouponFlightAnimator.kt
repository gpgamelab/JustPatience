package com.gpgamelab.justpatience.ui

import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import kotlin.math.hypot

/**
 * Manages coupon flight animation from the ticket icon to a target button.
 * The coupon image follows a discrete 25 ms size schedule across the 1500 ms flight.
 * The opening 10 display points and closing 10 display points follow exact requested
 * size tables, with a 4x hold in the middle and a 1x hold at the end.
 *
 * Motion and visual state are sampled every 25 ms.
 * Movement follows a parabolic arc similar to auto-play card animations.
 */
class CouponFlightAnimator {

    private var isAnimating = false
    private var animationStartTimeMs: Long = 0
    private var startRect: RectF = RectF()
    private var endRect: RectF = RectF()
    private var landscapeFlight = false
    private var landscapeMinArcPx = 0f
    private var controlPoint: PointF? = null

    companion object {
        const val ANIMATION_DURATION_MS = 1500L
        const val TOTAL_RUNTIME_MS = ANIMATION_DURATION_MS
        internal const val FRAME_STEP_MS = 25L
        internal const val MAX_SIZE_MULTIPLIER = 4f
        private const val OPENING_SAMPLE_COUNT = 10
        private const val CLOSING_SAMPLE_START = 48
        private const val END_HOLD_SAMPLE_START = 57
        private const val TOTAL_SAMPLE_COUNT = (ANIMATION_DURATION_MS / FRAME_STEP_MS).toInt()
        private const val ONE_THIRD_STEP = 1f / 3f
        private const val LANDSCAPE_ARC_BOOST = 2.8f
        private const val PORTRAIT_ARC_MAX_FACTOR = 0.82f
        private const val LANDSCAPE_ARC_MAX_FACTOR = 8.0f
    }

    /**
     * Schedules a coupon animation from source to destination.
     * @param sourceRect Starting position and size of the coupon (from ticket icon)
     * @param targetRect Ending position and size of the coupon (at target button)
     */
    fun scheduleCouponAnimation(
        sourceRect: RectF,
        targetRect: RectF,
        isLandscape: Boolean = false,
        landscapeMinArcPx: Float = 0f,
        controlPoint: PointF? = null
    ) {
        startRect = RectF(sourceRect)
        endRect = RectF(targetRect)
        landscapeFlight = isLandscape
        this.landscapeMinArcPx = landscapeMinArcPx.coerceAtLeast(0f)
        this.controlPoint = controlPoint?.let { PointF(it.x, it.y) }
        animationStartTimeMs = SystemClock.elapsedRealtime()
        isAnimating = true
    }

    /**
     * Returns true if animation is currently running.
     */
    fun isAnimating(): Boolean = isAnimating

    /**
     * Draw the animated coupon on the provided canvas.
     * Call this from the GameBoardView.onDraw() method.
     */
    fun drawAnimatedCoupon(canvas: Canvas, couponDrawable: android.graphics.drawable.Drawable?) {
        if (!isAnimating || couponDrawable == null) return

        val elapsedMs = SystemClock.elapsedRealtime() - animationStartTimeMs
        val progress = sampledProgressForElapsed(elapsedMs)
        val sizeMultiplier = sizeMultiplierForElapsed(elapsedMs)

        val eased = easeInOutCubic(progress)

        val pathCenterX = controlPoint?.let { cp ->
            quadraticBezier(startRect.centerX(), cp.x, endRect.centerX(), eased)
        } ?: lerp(startRect.centerX(), endRect.centerX(), eased)

        val pathCenterY = controlPoint?.let { cp ->
            quadraticBezier(startRect.centerY(), cp.y, endRect.centerY(), eased)
        } ?: lerp(startRect.centerY(), endRect.centerY(), eased)

        val baseW = lerp(startRect.width(), endRect.width(), eased)
        val baseH = lerp(startRect.height(), endRect.height(), eased)
        val baseRect = RectF(
            pathCenterX - baseW * 0.5f,
            pathCenterY - baseH * 0.5f,
            pathCenterX + baseW * 0.5f,
            pathCenterY + baseH * 0.5f
        )

        val dx = endRect.centerX() - startRect.centerX()
        val dy = endRect.centerY() - startRect.centerY()
        val distance = hypot(dx, dy)

        // Calculate parabolic arc (same logic as card animation in GameBoardView)
        val cardW = startRect.width().coerceAtLeast(1f)
        val cardH = startRect.height().coerceAtLeast(1f)
        val arcHeight = calculateArcHeight(distance, cardW, cardH)
        val arcLift = -4f * arcHeight * eased * (1f - eased)

        // Apply arc lift
        val animatedRect = RectF(
            baseRect.left,
            baseRect.top + arcLift,
            baseRect.right,
            baseRect.bottom + arcLift
        )

        // Apply size scaling around the center.
        // IMPORTANT:
        // 1) size is always relative to the original start size (no cumulative growth), and
        // 2) aspect ratio comes from the drawable itself so width/height scale uniformly.
        val centerX = animatedRect.centerX()
        val centerY = animatedRect.centerY()
        val baseSize = baseCouponDrawSize(couponDrawable)
        val scaledWidth = baseSize.width * sizeMultiplier
        val scaledHeight = baseSize.height * sizeMultiplier

        val scaledRect = RectF(
            centerX - scaledWidth / 2f,
            centerY - scaledHeight / 2f,
            centerX + scaledWidth / 2f,
            centerY + scaledHeight / 2f
        )

        // Draw the coupon in its native orientation.
        couponDrawable.setBounds(
            scaledRect.left.toInt(),
            scaledRect.top.toInt(),
            scaledRect.right.toInt(),
            scaledRect.bottom.toInt()
        )
        couponDrawable.draw(canvas)

        if (elapsedMs >= TOTAL_RUNTIME_MS) {
            isAnimating = false
        }

        // Continue invalidating if still animating
        // Note: Caller must handle postInvalidateOnAnimation()
    }

    internal fun sampledProgressForElapsed(elapsedMs: Long): Float {
        return (sampleIndexForElapsed(elapsedMs).toFloat() / TOTAL_SAMPLE_COUNT.toFloat()).coerceIn(0f, 1f)
    }

    internal fun sampleIndexForElapsed(elapsedMs: Long): Int {
        val clampedElapsed = elapsedMs.coerceIn(0L, ANIMATION_DURATION_MS)
        return ((clampedElapsed / FRAME_STEP_MS) * FRAME_STEP_MS / FRAME_STEP_MS)
            .toInt()
            .coerceIn(0, TOTAL_SAMPLE_COUNT)
    }

    internal fun sizeMultiplierForElapsed(elapsedMs: Long): Float {
        return sizeMultiplierForSampleIndex(sampleIndexForElapsed(elapsedMs))
    }

    internal fun sizeMultiplierForSampleIndex(sampleIndex: Int): Float {
        val clampedIndex = sampleIndex.coerceIn(0, TOTAL_SAMPLE_COUNT)
        return when {
            clampedIndex == 0 -> 1f

            clampedIndex in 1 until OPENING_SAMPLE_COUNT -> {
                val segmentIndex = (clampedIndex - 1) / 3
                when ((clampedIndex - 1) % 3) {
                    0 -> 1f + ONE_THIRD_STEP
                    1 -> 1f + (2f * ONE_THIRD_STEP)
                    else -> 2f + segmentIndex
                }
            }

            clampedIndex < CLOSING_SAMPLE_START -> MAX_SIZE_MULTIPLIER

            clampedIndex < END_HOLD_SAMPLE_START -> {
                val segmentIndex = (clampedIndex - CLOSING_SAMPLE_START) / 3
                when ((clampedIndex - CLOSING_SAMPLE_START) % 3) {
                    0 -> 1f + (2f * ONE_THIRD_STEP)
                    1 -> 1f + ONE_THIRD_STEP
                    else -> 3f - segmentIndex
                }
            }

            else -> 1f
        }
    }

    internal fun calculateArcHeight(distance: Float, cardW: Float, cardH: Float): Float {
        val safeCardW = cardW.coerceAtLeast(1f)
        val safeCardH = cardH.coerceAtLeast(1f)
        val baselineArcHeight = (distance * 0.14f).coerceIn(safeCardH * 0.10f, safeCardH * 0.42f)
        val shortestMoveDistance = (safeCardW * 1.20f).coerceAtLeast(1f)
        val longMoveRange = (safeCardW * 4.5f).coerceAtLeast(1f)
        val longMoveProgress = ((distance - shortestMoveDistance) / longMoveRange).coerceIn(0f, 1f)
        val dramaticMultiplier = 1f + (longMoveProgress * longMoveProgress) * 1.45f
        val arcBoost = if (landscapeFlight) LANDSCAPE_ARC_BOOST else 1f
        val maxArcFactor = if (landscapeFlight) LANDSCAPE_ARC_MAX_FACTOR else PORTRAIT_ARC_MAX_FACTOR

        val boostedHeight = baselineArcHeight * dramaticMultiplier * arcBoost
        val maxArc = safeCardH * maxArcFactor
        val requestedMinArc = if (landscapeFlight) landscapeMinArcPx.coerceAtLeast(safeCardH * 0.10f) else safeCardH * 0.10f
        val minArc = requestedMinArc.coerceAtMost(maxArc)
        return boostedHeight.coerceIn(minArc, maxArc)
    }

    internal fun configureFlightForTest(isLandscape: Boolean, landscapeMinArcPx: Float) {
        landscapeFlight = isLandscape
        this.landscapeMinArcPx = landscapeMinArcPx.coerceAtLeast(0f)
    }

    private data class CouponBaseSize(val width: Float, val height: Float)

    private fun baseCouponDrawSize(couponDrawable: android.graphics.drawable.Drawable): CouponBaseSize {
        val startW = startRect.width().coerceAtLeast(1f)
        val startH = startRect.height().coerceAtLeast(1f)
        val intrinsicW = couponDrawable.intrinsicWidth.toFloat()
        val intrinsicH = couponDrawable.intrinsicHeight.toFloat()

        if (intrinsicW <= 0f || intrinsicH <= 0f) {
            return CouponBaseSize(startW, startH)
        }

        // Fit drawable into start rect while preserving native aspect ratio.
        val fitScale = minOf(startW / intrinsicW, startH / intrinsicH)
        return CouponBaseSize(
            width = (intrinsicW * fitScale).coerceAtLeast(1f),
            height = (intrinsicH * fitScale).coerceAtLeast(1f)
        )
    }

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

    private fun quadraticBezier(start: Float, control: Float, end: Float, t: Float): Float {
        val oneMinusT = 1f - t
        return (oneMinusT * oneMinusT * start) + (2f * oneMinusT * t * control) + (t * t * end)
    }

    private fun easeInOutCubic(t: Float): Float {
        return if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - ((-2f * t + 2f) * (-2f * t + 2f) * (-2f * t + 2f)) / 2f
        }
    }
}
















