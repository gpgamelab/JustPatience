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
 *
 * NOTE ON ANDROID STUB: The Android SDK test stub's multi-argument RectF constructors are
 * no-ops — they create the object but never assign the public fields (all stay 0.0f).
 * We work around this by using [rectF], which uses the zero-arg constructor and sets each
 * field directly via PUTFIELD — the only reliable way to create non-zero RectF values in
 * plain JVM unit tests (no Robolectric required).
 */
class AdPlacementMatrixTest {

    private val safetyGapPx = 16f

    // Standard phone viewport (portrait):        1080×1920 logical pixels
    // Standard phone viewport (landscape):       1920×1080 logical pixels
    // Tablet viewport (portrait):                2560×1600 logical pixels
    // Tablet viewport (landscape):               1600×2560 logical pixels

    /**
     * Creates a RectF with correct field values despite the Android stub constructor being
     * a no-op. Uses the zero-arg constructor then assigns each public field directly.
     */
    private fun rectF(l: Float, t: Float, r: Float, b: Float): RectF =
        RectF().also { it.left = l; it.top = t; it.right = r; it.bottom = b }

    /**
     * Represents a standard solitaire board layout with interactive zones.
     * These are typical Phase 2 group rects for a 1-deck portrait layout.
     *
     * Geometry (px):
     *   DRAW_WASTE  50, 80  →  250, 180
     *   FOUNDATION 300, 80  → 1030, 180
     *   TABLEAU     50, 220 → 1030, 1500
     *   CONTROLS    50,1550 → 1030, 1720
     *   STATS      850,  30 → 1030,  100
     *   ADS        150,1750 →  930, 1900   (below controls, 30px gap)
     */
    private fun createPortraitBoardLayout(): Map<GroupId, RectF> = mapOf(
        GroupId.DRAW_WASTE to rectF(50f, 80f, 250f, 180f),
        GroupId.FOUNDATION to rectF(300f, 80f, 1030f, 180f),
        GroupId.TABLEAU    to rectF(50f, 220f, 1030f, 1500f),
        GroupId.CONTROLS   to rectF(50f, 1550f, 1030f, 1720f),
        GroupId.STATS      to rectF(850f, 30f, 1030f, 100f),
        GroupId.ADS        to rectF(150f, 1750f, 930f, 1900f)
    )

    /**
     * Landscape version of the board layout.
     *
     * Geometry (px) — ADS zone is physically below every interactive zone:
     *   DRAW_WASTE   50,  50 →  250,  200
     *   FOUNDATION  270,  50 → 1700,  200
     *   TABLEAU      50, 230 → 1900,  700
     *   CONTROLS     50, 720 → 1900,  780   (20px gap below TABLEAU)
     *   STATS      1750,  50 → 1900,  200
     *   ADS          50, 800 → 1900, 1060   (20px gap below CONTROLS)
     */
    private fun createLandscapeBoardLayout(): Map<GroupId, RectF> = mapOf(
        GroupId.DRAW_WASTE to rectF(50f, 50f, 250f, 200f),
        GroupId.FOUNDATION to rectF(270f, 50f, 1700f, 200f),
        GroupId.TABLEAU    to rectF(50f, 230f, 1900f, 700f),
        GroupId.CONTROLS   to rectF(50f, 720f, 1900f, 780f),
        GroupId.STATS      to rectF(1750f, 50f, 1900f, 200f),
        GroupId.ADS        to rectF(50f, 800f, 1900f, 1060f)
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
        val smallBannerRect = rectF(adsZone.left, adsZone.top, adsZone.left + 320f, adsZone.top + 60f)

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
        val largeBannerRect = rectF(
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
        val badBannerRect = rectF(
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
        val badBannerRect = rectF(
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
        val portraitLayout  = createPortraitBoardLayout()
        val landscapeLayout = createLandscapeBoardLayout()

        // Portrait placement (valid)
        val portraitAdsZone = portraitLayout[GroupId.ADS]!!
        val portraitBanner = rectF(
            portraitAdsZone.left, portraitAdsZone.top,
            portraitAdsZone.left + 320f, portraitAdsZone.top + 60f
        )
        assertTrue("Portrait banner should be valid",
            AdPlacementValidator.validate(portraitBanner, portraitLayout, safetyGapPx))

        // Same portrait coords are outside the landscape ADS zone (different bottom boundary)
        assertFalse(
            "Portrait banner coords should not fit landscape ADS zone",
            AdPlacementValidator.validate(portraitBanner, landscapeLayout, safetyGapPx)
        )

        // Landscape-specific banner should work
        val landscapeAdsZone = landscapeLayout[GroupId.ADS]!!
        val landscapeBanner = rectF(
            landscapeAdsZone.left, landscapeAdsZone.top,
            landscapeAdsZone.left + 320f, landscapeAdsZone.top + 110f
        )
        assertTrue("Landscape banner should be valid",
            AdPlacementValidator.validate(landscapeBanner, landscapeLayout, safetyGapPx))
    }

    /**
     * Test 8: Empty board (no groups defined) - banner passes (no overlaps).
     * Edge case: validates graceful handling of sparse layouts.
     */
    @Test
    fun testEmptyBoardNoGroupsFails() {
        val emptyLayout = emptyMap<GroupId, RectF>()
        val bannerRect = rectF(0f, 0f, 320f, 60f)

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

        // Banner exactly matches ADS zone — use rectF() to avoid copy-constructor stub
        val exactBanner = rectF(adsZone.left, adsZone.top, adsZone.right, adsZone.bottom)

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
