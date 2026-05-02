package com.gpgamelab.justpatience.util

import android.content.Context

/**
 * Reusable helper for scaling feature-specific values from a portrait baseline device.
 *
 * Callers provide the portrait baseline resolution once. The utility automatically swaps
 * the baseline axes when the current device is in landscape so the same average-ratio math
 * works in both orientations.
 */
object BaselineResolutionScaleUtil {

    private const val MIN_AVERAGE_RATIO = 0.01f

    data class ResolutionRatioProfile(
        val currentWidthPx: Int,
        val currentHeightPx: Int,
        val baselineWidthPx: Int,
        val baselineHeightPx: Int,
        val widthRatio: Float,
        val heightRatio: Float,
        val averageRatio: Float,
        val isLandscape: Boolean
    )

    fun calculateAverageRatio(
        context: Context,
        baselinePortraitWidthPx: Int,
        baselinePortraitHeightPx: Int
    ): ResolutionRatioProfile {
        val metrics = context.resources.displayMetrics
        return calculateAverageRatio(
            currentWidthPx = metrics.widthPixels,
            currentHeightPx = metrics.heightPixels,
            baselinePortraitWidthPx = baselinePortraitWidthPx,
            baselinePortraitHeightPx = baselinePortraitHeightPx
        )
    }

    fun calculateAverageRatio(
        currentWidthPx: Int,
        currentHeightPx: Int,
        baselinePortraitWidthPx: Int,
        baselinePortraitHeightPx: Int
    ): ResolutionRatioProfile {
        val safeCurrentWidthPx = currentWidthPx.coerceAtLeast(1)
        val safeCurrentHeightPx = currentHeightPx.coerceAtLeast(1)
        val safeBaselinePortraitWidthPx = baselinePortraitWidthPx.coerceAtLeast(1)
        val safeBaselinePortraitHeightPx = baselinePortraitHeightPx.coerceAtLeast(1)
        val isLandscape = safeCurrentWidthPx > safeCurrentHeightPx

        val baselineWidthPx = if (isLandscape) {
            safeBaselinePortraitHeightPx
        } else {
            safeBaselinePortraitWidthPx
        }
        val baselineHeightPx = if (isLandscape) {
            safeBaselinePortraitWidthPx
        } else {
            safeBaselinePortraitHeightPx
        }

        val widthRatio = safeCurrentWidthPx.toFloat() / baselineWidthPx.toFloat()
        val heightRatio = safeCurrentHeightPx.toFloat() / baselineHeightPx.toFloat()
        val averageRatio = ((widthRatio + heightRatio) / 2f).coerceAtLeast(MIN_AVERAGE_RATIO)

        return ResolutionRatioProfile(
            currentWidthPx = safeCurrentWidthPx,
            currentHeightPx = safeCurrentHeightPx,
            baselineWidthPx = baselineWidthPx,
            baselineHeightPx = baselineHeightPx,
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            averageRatio = averageRatio,
            isLandscape = isLandscape
        )
    }

    fun scaleFromBaseline(baselineValue: Float, averageRatio: Float): Float {
        return baselineValue * averageRatio.coerceAtLeast(MIN_AVERAGE_RATIO)
    }

    fun inverseScaleFromBaseline(baselineValue: Float, averageRatio: Float): Float {
        return baselineValue / averageRatio.coerceAtLeast(MIN_AVERAGE_RATIO)
    }
}

