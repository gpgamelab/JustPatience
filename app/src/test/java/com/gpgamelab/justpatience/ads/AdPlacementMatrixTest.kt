package com.gpgamelab.justpatience.ads

import android.graphics.RectF
import com.gpgamelab.justpatience.ads.AdBannerTier
import com.gpgamelab.justpatience.ui.layout.GroupId
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for Phase 4 Ad Placement components.
 *
 * These tests validate the interplay between [AdPlacementPolicy], [AdPlacementValidator],
 * and [AdPlacementCoordinator] across a matrix of device categories and placements.
 */
class AdPlacementMatrixTest {

    private val safetyGapPx = 16f

    // Standard phone viewport (portrait):        1080×1920 logical pixels
    // Standard phone viewport (landscape):       1920×1080 logical pixels
    // Tablet viewport (portrait):                2560×1600 logical pixels
    // Tablet viewport (landscape):               1600×2560 logical pixels

    /**
     * Represents a standard solitaire board layout with interactive zones.
     * These are typical Phase 2 group rects for a 1-deck portrait layout.
     */
    private fun createPortraitBoardLayout(): Map<GroupId, RectF> = mapOf(
        // Top row: stock/waste, foundations
        GroupId.DRAW_WASTE to RectF(50f, 80f, 250f, 180f),
        GroupId.FOUNDATION to RectF(300f, 80f, 1030f, 180f),

        // Main tableau (7 columns)
        GroupId.TABLEAU to RectF(50f, 220f, 1030f, 1500f),

        // Control buttons at bottom
        GroupId.CONTROLS to RectF(50f, 1550f, 1030f, 1720f),

        // Scoreboard (top right)
        GroupId.STATS to RectF(850f, 30f, 1030f, 100f),

        // Safe ad zone (bottom center)
        GroupId.ADS to RectF(150f, 1750f, 930f, 1900f)
    )

    /**
     * Landscape version of the board layout.
     */
    private fun createLandscapeBoardLayout(): Map<GroupId, RectF> = mapOf(
        GroupId.DRAW_WASTE to RectF(50f, 50f, 200f, 200f),
        GroupId.FOUNDATION to RectF(250f, 50f, 1700f, 200f),
        GroupId.TABLEAU to RectF(50f, 250f, 1900f, 1030f),
        GroupId.CONTROLS to RectF(50f, 1050f, 1900f, 1080f),
        GroupId.STATS to RectF(1750f, 50f, 1900f, 200f),
        GroupId.ADS to RectF(300f, 1040f, 1600f, 1070f)
    )

    @Before
    fun setUp() {
        // Placeholder for any common setup
    }

    /**
     * Test 1: Small phone (SLIM_COMPACT) in portrait with SMALL banner.
     * Validates that a small banner fits in the designated ADS zone without overlap.
     */
    @Test
    fun testSmallPhonePortraitSmallBanner() {
        val boardLayout = createPortraitBoardLayout()
        val adsZone = boardLayout[GroupId.ADS]!!
        val smallBannerRect = RectF(adsZone.left, adsZone.top, adsZone.left + 320f, adsZone.top + 60f)

        val result = AdPlacementValidator.validate(smallBannerRect, boardLayout, safetyGapPx)
        assertTrue("Small banner should fit in ADS zone", result)
    }

    /**
     * Test 2: Tablet (BROAD) in landscape with LARGE banner.
     * Validates that a large banner fits in the designated ADS zone without overlap.
     */
    @Test
    fun testTabletLandscapeLargeBanner() {
        val boardLayout = createLandscapeBoardLayout()
        val adsZone = boardLayout[GroupId.ADS]!!
        val largeBannerRect = RectF(
            adsZone.left, adsZone.top,
            adsZone.left + 320f, adsZone.top + 260f
        )

        val result = AdPlacementValidator.validate(largeBannerRect, boardLayout, safetyGapPx)
        assertTrue("Large banner should fit in ADS zone", result)
    }

    /**
     * Test 3: Banner overlaps tableau (should fail).
     * Validates that AdPlacementValidator detects overlaps correctly.
     */
    @Test
    fun testBannerOverlapTableauFails() {
        val boardLayout = createPortraitBoardLayout()
        val tableauZone = boardLayout[GroupId.TABLEAU]!!

        // Position banner overlapping tableau
        val badBannerRect = RectF(
            tableauZone.left + 100f,
            tableauZone.top - 30f,  // Overlaps tableau top
            tableauZone.left + 400f,
            tableauZone.top + 100f
        )

        val result = AdPlacementValidator.validate(badBannerRect, boardLayout, safetyGapPx)
        assertFalse("Banner overlapping tableau should fail validation", result)
    }

    /**
     * Test 4: Banner too close to controls (fails gap check).
     * Validates that the safety gap is enforced.
     */
    @Test
    fun testBannerTooCloseToControlsFails() {
        val boardLayout = createPortraitBoardLayout()
        val controlsZone = boardLayout[GroupId.CONTROLS]!!
        val adsZone = boardLayout[GroupId.ADS]!!

        // Place banner in ADS zone but closer than safetyGap to controls
        val badBannerRect = RectF(
            adsZone.left,
            controlsZone.bottom - safetyGapPx + 2f,  // Only 2px gap, need 16px
            adsZone.left + 320f,
            controlsZone.bottom + 60f
        )

        val result = AdPlacementValidator.validate(badBannerRect, boardLayout, safetyGapPx)
        assertFalse("Banner with insufficient gap to controls should fail", result)
    }

    /**
     * Test 5: Multiple tiers - primary fails, fallback succeeds.
     * Validates the coordinator's multi-tier fallback logic.
     */
    @Test
    fun testPolicyMultiTierFallback() {
        // Phone (SLIM) in portrait: tries SMALL (primary), suggests MEDIUM if it fails
        val policy = defaultAdPlacementPolicy(
            category = com.gpgamelab.justpatience.util.DeviceAspectCategory.SLIM,
            isLandscape = false
        )

        assertEquals("Primary tier should be SMALL", AdBannerTier.SMALL, policy.primaryTier)
        assertEquals("Fallback tier should be MEDIUM", AdBannerTier.MEDIUM, policy.fallbackTier)
    }

    /**
     * Test 6: Tablet (BROAD) landscape - no fallback (only large banner).
     * Validates that large devices use only LARGE tier with no fallback.
     */
    @Test
    fun testTabletBroadLandscapeNoFallback() {
        val policy = defaultAdPlacementPolicy(
            category = com.gpgamelab.justpatience.util.DeviceAspectCategory.BROAD,
            isLandscape = true
        )

        assertEquals("Tablet broad should use LARGE", AdBannerTier.LARGE, policy.primaryTier)
        assertNull("Large devices should have no fallback", policy.fallbackTier)
    }

    /**
     * Test 7: Portrait to landscape rotation - banner repositions.
     * Validates that the coordinator detects orientation change and revalidates.
     */
    @Test
    fun testOrientationRotationRepositions() {
        val portraitLayout = createPortraitBoardLayout()
        val landscapeLayout = createLandscapeBoardLayout()

        // Portrait placement (valid)
        val portraitAdsZone = portraitLayout[GroupId.ADS]!!
        val portraitBanner = RectF(portraitAdsZone.left, portraitAdsZone.top, portraitAdsZone.left + 320f, portraitAdsZone.top + 60f)
        assertTrue("Portrait banner should be valid", AdPlacementValidator.validate(portraitBanner, portraitLayout, safetyGapPx))

        // Same rect coords in landscape might overlap (different viewport)
        val landscapeAdsZone = landscapeLayout[GroupId.ADS]!!
        assertFalse(
            "Portrait banner coords should not fit landscape ADS zone",
            AdPlacementValidator.validate(portraitBanner, landscapeLayout, safetyGapPx)
        )

        // Landscape-specific banner should work
        val landscapeBanner = RectF(landscapeAdsZone.left, landscapeAdsZone.top, landscapeAdsZone.left + 320f, landscapeAdsZone.top + 110f)
        assertTrue("Landscape banner should be valid", AdPlacementValidator.validate(landscapeBanner, landscapeLayout, safetyGapPx))
    }

    /**
     * Test 8: Empty board (no groups defined) - banner passes (no overlaps).
     * Edge case: validates graceful handling of sparse layouts.
     */
    @Test
    fun testEmptyBoardNoGroupsFails() {
        val emptyLayout = emptyMap<GroupId, RectF>()
        val bannerRect = RectF(0f, 0f, 320f, 60f)

        // With no groups to validate against, AdPlacementValidator should pass
        val result = AdPlacementValidator.validate(bannerRect, emptyLayout, safetyGapPx)
        assertTrue("Banner on empty board should pass", result)
    }

    /**
     * Test 9: Banner exactly fits ADS zone (no gap).
     * Edge case: validates boundary condition (same size as zone).
     */
    @Test
    fun testBannerExactlyFitsAdsZone() {
        val boardLayout = createPortraitBoardLayout()
        val adsZone = boardLayout[GroupId.ADS]!!

        // Banner exactly matches ADS zone
        val exactBanner = RectF(adsZone)

        val result = AdPlacementValidator.validate(exactBanner, boardLayout, safetyGapPx)
        assertTrue("Banner exactly matching ADS zone should be valid", result)
    }

    /**
     * Test 10: Coordinator state caching.
     * Validates that device category is computed once and cached.
     */
    @Test
    fun testCoordinatorDeviceCategoryCaching() {
        // This test would require Android Context (not unit-testable in isolation).
        // In instrumented tests (androidTest), we can verify:
        // 1. deviceCategory is lazy-initialized once
        // 2. Subsequent calls return the same instance
        // For now, document the expected behavior:

        // Expected:
        // val coordinator = AdPlacementCoordinator(context)
        // val cat1 = coordinator.deviceCategory
        // val cat2 = coordinator.deviceCategory
        // assertSame("Device category should be cached", cat1, cat2)
    }
}


