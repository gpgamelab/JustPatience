package com.gpgamelab.justpatience.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object UiScaleUtil {

    // ─── Baseline reference device (Lenovo Tab P12 Pro, API 31 medium tablet) ───
    // All XML dp/sp values were designed at this size. Increase to shrink UI globally;
    // decrease to enlarge UI globally.
    const val BASELINE_WIDTH_DP  = 1280f
    const val BASELINE_HEIGHT_DP = 800f

    // ─── Extreme-aspect (phone vs tablet) axis correction ───────────────────────
    // Devices whose (largest dim / smallest dim) exceeds this threshold are treated
    // as "tall phone" shaped.  The rule is RECIPROCAL:
    //   • Shorter axis: multiplied by EXTREME_ASPECT_COMPRESSION (0.8 → narrower/shorter)
    //   • Longer  axis: divided   by EXTREME_ASPECT_COMPRESSION (÷0.8 = ×1.25 → more room)
    //   • Text:         multiplied by EXTREME_ASPECT_COMPRESSION (smaller on phones)
    // Phones ≈ 2.17+, tablets ≈ <1.67.  2.0 is the demarcation point.
    // Set EXTREME_ASPECT_COMPRESSION = 1.0f to disable the rule entirely.
    const val EXTREME_ASPECT_RATIO_THRESHOLD = 2.0f
    const val EXTREME_ASPECT_COMPRESSION     = 0.8f

    data class ScaleFactors(
        val horizontal: Float,
        val vertical: Float,
        val text: Float,
        val aspectRatio: Float,
        val isExtremeAspect: Boolean,
        /** Multiplier applied to the horizontal axis (< 1 compresses, > 1 expands). */
        val horizontalFactor: Float,
        /** Multiplier applied to the vertical   axis (< 1 compresses, > 1 expands). */
        val verticalFactor: Float,
        val textCompression: Float
    )

    fun calculateBaselineScaleFactors(context: Context): ScaleFactors {
        val config = context.resources.configuration
        val metrics = context.resources.displayMetrics
        val widthDp = config.screenWidthDp.takeIf { it > 0 }?.toFloat() ?: (metrics.widthPixels / metrics.density)
        val heightDp = config.screenHeightDp.takeIf { it > 0 }?.toFloat() ?: (metrics.heightPixels / metrics.density)
        return calculateBaselineScaleFactors(widthDp, heightDp)
    }

    fun calculateBaselineScaleFactors(widthDp: Float, heightDp: Float): ScaleFactors {
        val safeWidthDp = widthDp.coerceAtLeast(1f)
        val safeHeightDp = heightDp.coerceAtLeast(1f)
        val baseHorizontal = (safeWidthDp / BASELINE_WIDTH_DP).coerceIn(0.70f, 1.70f)
        val baseVertical = (safeHeightDp / BASELINE_HEIGHT_DP).coerceIn(0.70f, 1.70f)
        val baseText = sqrt((safeWidthDp / BASELINE_WIDTH_DP) * (safeHeightDp / BASELINE_HEIGHT_DP)).coerceIn(0.80f, 1.40f)

        val smallerDp = min(safeWidthDp, safeHeightDp)
        val largerDp = max(safeWidthDp, safeHeightDp)
        val aspectRatio = largerDp / smallerDp
        val isExtremeAspect = aspectRatio > EXTREME_ASPECT_RATIO_THRESHOLD

        // Reciprocal rule: short axis compressed, long axis expanded.
        val expansion = if (isExtremeAspect) 1f / EXTREME_ASPECT_COMPRESSION else 1f
        val isPortraitLike = safeWidthDp <= safeHeightDp
        val horizontalFactor = when {
            !isExtremeAspect  -> 1f
            isPortraitLike    -> EXTREME_ASPECT_COMPRESSION  // width is short axis → compress
            else              -> expansion                   // width is long  axis → expand
        }
        val verticalFactor = when {
            !isExtremeAspect  -> 1f
            !isPortraitLike   -> EXTREME_ASPECT_COMPRESSION  // height is short axis → compress
            else              -> expansion                   // height is long  axis → expand
        }
        val textCompression = if (isExtremeAspect) EXTREME_ASPECT_COMPRESSION else 1f

        return ScaleFactors(
            horizontal = baseHorizontal * horizontalFactor,
            vertical = baseVertical * verticalFactor,
            text = baseText * textCompression,
            aspectRatio = aspectRatio,
            isExtremeAspect = isExtremeAspect,
            horizontalFactor = horizontalFactor,
            verticalFactor = verticalFactor,
            textCompression = textCompression
        )
    }

    fun applyBaselineScale(root: View, context: Context) {
        val factors = calculateBaselineScaleFactors(context)
        applyScaleRecursive(root, factors)
    }

    /**
     * Apply only the *baseline* vertical scale (screen height vs. baseline height) to the
     * given view and its descendants, deliberately stripping the extreme-aspect-ratio
     * multiplier.  This is the right call for top/bottom chrome bars (info panel, button
     * row) where the full aspect correction would wildly over- or under-scale:
     *
     *   • Portrait phone: raw vertical ≈ 1.14  →  buttons get ~14 % taller.  ✓
     *   • Landscape phone: raw vertical ≈ 0.70  →  buttons get ~30 % shorter,
     *       reclaiming vertical space for the board.  ✓
     *   • Tablet: raw vertical ≈ 1.0  →  no change.  ✓
     *
     * Only heights, vertical paddings, vertical margins and minimumHeight are
     * touched.  Widths, horizontal paddings/margins and text sizes are left alone.
     * The same [R.id.ui_scale_applied_tag] guard prevents double-application.
     */
    fun applyScreenVerticalScale(root: View, context: Context) {
        val factors = calculateBaselineScaleFactors(context)
        // Strip the extreme-aspect multiplier so the factor reflects only the
        // screen-height vs. baseline-height ratio.
        val screenVertical = if (factors.verticalFactor != 0f)
            (factors.vertical / factors.verticalFactor).coerceIn(0.70f, 1.50f)
        else
            factors.vertical.coerceIn(0.70f, 1.50f)
        applyVerticalScaleRecursive(root, screenVertical)
    }

    private fun applyVerticalScaleRecursive(view: View, factor: Float) {
        if (view.getTag(com.gpgamelab.justpatience.R.id.ui_scale_applied_tag) == true) return

        val lp = view.layoutParams
        if (lp != null) {
            if (lp.height > 0) {
                lp.height = (lp.height * factor).toInt().coerceAtLeast(1)
            }
            if (lp is ViewGroup.MarginLayoutParams) {
                lp.topMargin    = (lp.topMargin    * factor).toInt()
                lp.bottomMargin = (lp.bottomMargin * factor).toInt()
            }
            view.layoutParams = lp
        }

        view.setPadding(
            view.paddingLeft,
            (view.paddingTop    * factor).toInt(),
            view.paddingRight,
            (view.paddingBottom * factor).toInt()
        )

        view.minimumHeight = (view.minimumHeight * factor).toInt()
        view.translationY  *= factor

        if (view is ViewGroup) {
            view.children.forEach { applyVerticalScaleRecursive(it, factor) }
        }

        view.setTag(com.gpgamelab.justpatience.R.id.ui_scale_applied_tag, true)
    }

    private fun applyScaleRecursive(view: View, factors: ScaleFactors) {
        if (view.getTag(com.gpgamelab.justpatience.R.id.ui_scale_applied_tag) == true) {
            return
        }

        val lp = view.layoutParams
        if (lp != null) {
            if (lp.width > 0) {
                lp.width = (lp.width * factors.horizontal).toInt().coerceAtLeast(1)
            }
            if (lp.height > 0) {
                lp.height = (lp.height * factors.vertical).toInt().coerceAtLeast(1)
            }

            if (lp is ViewGroup.MarginLayoutParams) {
                lp.leftMargin = (lp.leftMargin * factors.horizontal).toInt()
                lp.rightMargin = (lp.rightMargin * factors.horizontal).toInt()
                lp.topMargin = (lp.topMargin * factors.vertical).toInt()
                lp.bottomMargin = (lp.bottomMargin * factors.vertical).toInt()
                lp.marginStart = (lp.marginStart * factors.horizontal).toInt()
                lp.marginEnd = (lp.marginEnd * factors.horizontal).toInt()
            }

            view.layoutParams = lp
        }

        view.setPadding(
            (view.paddingLeft * factors.horizontal).toInt(),
            (view.paddingTop * factors.vertical).toInt(),
            (view.paddingRight * factors.horizontal).toInt(),
            (view.paddingBottom * factors.vertical).toInt()
        )

        view.minimumWidth = (view.minimumWidth * factors.horizontal).toInt()
        view.minimumHeight = (view.minimumHeight * factors.vertical).toInt()
        view.translationX *= factors.horizontal
        view.translationY *= factors.vertical

        if (view is TextView) {
            view.textSize = view.textSize * factors.text
        }

        if (view is ViewGroup) {
            view.children.forEach { child ->
                applyScaleRecursive(child, factors)
            }
        }

        view.setTag(com.gpgamelab.justpatience.R.id.ui_scale_applied_tag, true)
    }
}

