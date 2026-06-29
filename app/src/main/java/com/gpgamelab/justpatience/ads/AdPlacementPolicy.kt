package com.gpgamelab.justpatience.ads

import com.gpgamelab.justpatience.util.DeviceAspectCategory

/**
 * Phase 4 – Ad Placement Policy
 * ──────────────────────────────────────────────────────────────────────────────
 *
 * Data model for device-category-aware banner ad placement.
 *
 * Design contract:
 * - [AdBannerTier] maps to concrete AdSize constants in AdManager.
 * - [AdPlacementPolicy] encodes primary + fallback tier for a given device/orientation.
 * - [defaultAdPlacementPolicy] returns the canonical policy; call this instead of
 *   constructing AdPlacementPolicy directly.
 * - [AdPlacementResult] is the sealed outcome of a validation attempt by
 *   [AdPlacementCoordinator].
 */

// ──────────────────────────────────────────────────────────────────────────────
// Banner tier
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Banner ad size tier.  Maps 1-to-1 with AdManager's size selections.
 *
 * | Tier   | AdSize          | Width × Height (dp) | Fits screen width   |
 * |--------|-----------------|---------------------|---------------------|
 * | SMALL  | BANNER          | 320 × 50            | SLIM_COMPACT+       |
 * | MEDIUM | LARGE_BANNER    | 320 × 100           | SLIM+               |
 * | LARGE  | MEDIUM_RECTANGLE| 300 × 250           | CLASSIC+ (portrait) |
 */
enum class AdBannerTier {
    /** 320×50 dp banner – safe for all device categories including SLIM_COMPACT. */
    SMALL,

    /** 320×100 dp large banner – safe for SLIM, CLASSIC, BROAD, SQUARE. */
    MEDIUM,

    /**
     * 300×250 dp medium rectangle – preferred on BROAD/SQUARE and tablets.
     * Avoid on SLIM_COMPACT and SLIM (occupies too much vertical space).
     */
    LARGE;

    companion object {
        /**
         * Returns true if [tier] is safe for ultra-narrow (SLIM_COMPACT) screens
         * where vertical real estate is scarce.
         */
        fun fitsNarrowScreen(tier: AdBannerTier): Boolean = tier == SMALL
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Safety gap
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Minimum physical gap (in dp) between the ad container and any interactive game element.
 *
 * The 3 dp minimum follows AdMob's accidental-click prevention guidelines.
 * [GENEROUS] is used on SLIM_COMPACT devices where a misplaced tap is more likely.
 */
data class AdSafetyGap(val dpValue: Float = 3f) {
    companion object {
        /** Mandatory minimum: 3 dp.  Used for all standard device categories. */
        val DEFAULT = AdSafetyGap(3f)

        /**
         * Generous buffer: 6 dp.  Recommended for ultra-narrow (SLIM_COMPACT) devices
         * where small screens make accidental ad interactions more common.
         */
        val GENEROUS = AdSafetyGap(6f)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Placement policy
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Resolved placement policy for a single banner ad request.
 *
 * Combines the device category and orientation context with tier priorities and
 * the required safety gap.  This is the input to [AdPlacementCoordinator].
 *
 * @param category       Device aspect category (SLIM_COMPACT, SLIM, CLASSIC, BROAD, SQUARE).
 * @param isLandscape    Whether the device is currently in landscape orientation.
 * @param primaryTier    First-choice banner tier.
 * @param fallbackTier   Second-choice tier if primary placement fails validation.
 *                       null means there is no safe fallback and the ad must be suppressed.
 * @param safetyGap      Required gap between ad container and interactive zones.
 */
data class AdPlacementPolicy(
    val category:     DeviceAspectCategory,
    val isLandscape:  Boolean,
    val primaryTier:  AdBannerTier,
    val fallbackTier: AdBannerTier?,
    val safetyGap:    AdSafetyGap = AdSafetyGap.DEFAULT
)

// ──────────────────────────────────────────────────────────────────────────────
// Placement result
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Outcome of an ad placement validation attempt by [AdPlacementCoordinator].
 *
 * Usage:
 * ```kotlin
 * when (val result = coordinator.validateForCurrentLayout(containerRect, groupRects, safetyPx)) {
 *     is AdPlacementResult.Valid      -> loadBannerAd(result.tier)
 *     is AdPlacementResult.NoPlacement -> suppressBannerAd()
 * }
 * ```
 */
sealed class AdPlacementResult {
    /** Ad container is safely placed; [tier] is the validated banner tier to load. */
    data class Valid(val tier: AdBannerTier) : AdPlacementResult()

    /**
     * No safe placement was found.
     * [reason] is a human-readable description for logs/debugging.
     * The ad should be suppressed for this layout cycle.
     */
    data class NoPlacement(val reason: String) : AdPlacementResult()
}

// ──────────────────────────────────────────────────────────────────────────────
// Default policy factory
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Returns the canonical [AdPlacementPolicy] for the given [category] and [isLandscape].
 *
 * Tier selection heuristics (independent of orientation):
 *
 * | Category    | Primary | Fallback | Rationale                                   |
 * |-------------|---------|----------|---------------------------------------------|
 * | SLIM_COMPACT| SMALL   | MEDIUM   | Ultra-narrow devices still prefer SMALL      |
 * | SLIM        | SMALL   | MEDIUM   | SMALL preferred; MEDIUM is the fallback      |
 * | CLASSIC     | MEDIUM  | LARGE    | Standard phone; use medium first             |
 * | BROAD       | LARGE   | null     | Wide/tablet layouts use the large banner     |
 * | SQUARE      | MEDIUM  | LARGE    | Near-square layouts prefer medium first      |
 *
 * SLIM_COMPACT uses [AdSafetyGap.GENEROUS] because accidental taps are more common
 * on ultra-narrow screens.
 */
fun defaultAdPlacementPolicy(
    category: DeviceAspectCategory,
    isLandscape: Boolean
): AdPlacementPolicy = when (category) {
    DeviceAspectCategory.SLIM_COMPACT -> AdPlacementPolicy(
        category     = category,
        isLandscape  = isLandscape,
        primaryTier  = AdBannerTier.SMALL,
        fallbackTier = null
    )
    DeviceAspectCategory.SLIM -> AdPlacementPolicy(
        category     = category,
        isLandscape  = isLandscape,
        primaryTier  = AdBannerTier.SMALL,
        fallbackTier = null
    )
    DeviceAspectCategory.CLASSIC -> AdPlacementPolicy(
        category     = category,
        isLandscape  = isLandscape,
        primaryTier  = AdBannerTier.SMALL,
        fallbackTier = null
    )
    DeviceAspectCategory.BROAD -> AdPlacementPolicy(
        category     = category,
        isLandscape  = isLandscape,
        primaryTier  = AdBannerTier.SMALL,
        fallbackTier = null
    )
    DeviceAspectCategory.SQUARE -> AdPlacementPolicy(
        category     = category,
        isLandscape  = isLandscape,
        primaryTier  = AdBannerTier.SMALL,
        fallbackTier = null
    )
}


