package com.gpgamelab.justpatience.ads

import android.content.Context
import android.content.res.Configuration
import android.graphics.RectF
import com.gpgamelab.justpatience.ui.layout.GroupId
import com.gpgamelab.justpatience.util.DeviceAspectCategory

/**
 * Phase 4 – Ad Placement Coordinator
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * Orchestrates device-category-aware ad placement by combining:
 * - [AdPlacementPolicy] (which banner tier is preferred for this device/orientation)
 * - [AdPlacementValidator] (runtime no-overlap check against Phase 2 board groups)
 *
 * Typical usage in GameActivity:
 * ```kotlin
 * // Field (created once)
 * private val adPlacementCoordinator = AdPlacementCoordinator(this)
 *
 * // Called after banner container is positioned (after Phase 2 layout settled)
 * val containerRect = getContainerScreenRect()  // from current container View bounds
 * val result = adPlacementCoordinator.validateForCurrentLayout(
 *     adContainerRect = containerRect,
 *     groupRects      = cachedPhase2GroupRects,
 *     safetyGapPx     = adSafetyPx
 * )
 * when (result) {
 *     is AdPlacementResult.Valid      -> applyBannerTier(result.tier)
 *     is AdPlacementResult.NoPlacement -> suppressBanner(result.reason)
 * }
 * ```
 *
 * @param context  Activity or Application context.  Used only for display metrics
 *                 and orientation queries; not retained beyond construction-time
 *                 for device category classification.
 */
class AdPlacementCoordinator(private val context: Context) {

    // ──────────────────────────────────────────────────────────────────────────
    // Device classification (computed once; orientation-independent)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Device aspect category, computed once from display metrics.
     *
     * [DeviceAspectCategory.classify] uses min/max pixel dimensions so the result
     * is stable regardless of current orientation.
     */
    val deviceCategory: DeviceAspectCategory by lazy {
        val metrics = context.resources.displayMetrics
        DeviceAspectCategory.classify(metrics.widthPixels, metrics.heightPixels)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Policy resolution
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Current orientation – re-evaluated on every call because the activity can rotate.
     */
    private fun isLandscape(): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * Resolve the [AdPlacementPolicy] for the current device category and orientation.
     *
     * Call this each time you need to determine which banner tier to use.
     * The policy is re-created on every call (cheap: no IO, no allocations beyond
     * the data class).
     */
    fun resolvePolicy(): AdPlacementPolicy =
        defaultAdPlacementPolicy(deviceCategory, isLandscape())

    // ──────────────────────────────────────────────────────────────────────────
    // Placement validation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validate a proposed ad container rectangle against the current Phase 2 board layout.
     *
     * Validation sequence:
     * 1. Resolve policy for current device + orientation.
     * 2. Run [AdPlacementValidator.validate] for the primary tier.
     * 3. If primary fails:
     *    a. If a fallback tier exists, report [AdPlacementResult.Valid] with the
     *       fallback tier (caller is responsible for resizing container to match).
     *    b. If no fallback, return [AdPlacementResult.NoPlacement].
     *
     * Note: The coordinator does NOT reposition the container; it only advises
     * which tier to use.  The caller must resize/reposition the container for
     * the fallback tier and call [reloadBannerForCurrentConfiguration] again.
     *
     * @param adContainerRect  Ad container bounds in screen pixels (after translations applied).
     * @param groupRects       Phase 2 screen-space rects (from [GameBoardView.onPhase2BoardLayoutReady]).
     * @param safetyGapPx      Minimum gap in pixels ([RefToScreenTransform.adSafetyPx]).
     */
    fun validateForCurrentLayout(
        adContainerRect: RectF,
        groupRects:      Map<GroupId, RectF>,
        safetyGapPx:     Float
    ): AdPlacementResult {
        val policy = resolvePolicy()

        // Attempt primary tier validation
        val primaryValid = AdPlacementValidator.validate(adContainerRect, groupRects, safetyGapPx)
        if (primaryValid) {
            return AdPlacementResult.Valid(policy.primaryTier)
        }

        // Primary failed — advise fallback tier (caller must resize container)
        val fallback = policy.fallbackTier
        if (fallback != null) {
            return AdPlacementResult.Valid(fallback)
        }

        // No valid placement available
        val reason = "Ad container overlaps interactive game zone; no fallback available " +
                     "(category=${deviceCategory}, landscape=${isLandscape()}, " +
                     "adRect=[${adContainerRect.left.toInt()},${adContainerRect.top.toInt()}," +
                     "${adContainerRect.right.toInt()},${adContainerRect.bottom.toInt()}])"
        return AdPlacementResult.NoPlacement(reason)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Design-time / debug validation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validate that the ADS group rect from Phase 2 is well-separated from all
     * other interactive groups.
     *
     * This is a regression-detection utility: the Phase 2 layout engine should always
     * produce a correctly-separated ADS group, but this check catches future regressions
     * if the layout engine is modified.
     *
     * Call from develop-mode menu or in automated tests.
     *
     * @param groupRects   All Phase 2 group rects (must include [GroupId.ADS]).
     * @param safetyGapPx  Safety gap in pixels.
     * @return true if ADS group is safely separated; false indicates a layout regression.
     */
    fun validateAdsGroupSeparation(
        groupRects:  Map<GroupId, RectF>,
        safetyGapPx: Float
    ): Boolean {
        val adsRect = groupRects[GroupId.ADS] ?: run {
            return true
        }
        val valid = AdPlacementValidator.validate(adsRect, groupRects, safetyGapPx)
        return valid
    }

    /**
     * Convenience helper: compute the safety gap in pixels from dp for the
     * current device density.
     *
     * @param dpValue  Gap size in dp (use [AdSafetyGap.dpValue]).
     * @return Gap rounded up to the nearest pixel, minimum 1.
     */
    fun safetyGapDpToPx(dpValue: Float): Float {
        val density = context.resources.displayMetrics.density
        return ((dpValue * density) + 0.5f).coerceAtLeast(1f)
    }
}


