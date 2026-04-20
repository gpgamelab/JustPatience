package com.gpgamelab.justpatience.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineResolutionScaleUtilTest {

    @Test
    fun mediumTabletPortrait_isExactBaseline() {
        val profile = BaselineResolutionScaleUtil.calculateAverageRatio(
            currentWidthPx = 1600,
            currentHeightPx = 2560,
            baselinePortraitWidthPx = 1600,
            baselinePortraitHeightPx = 2560
        )

        assertFalse(profile.isLandscape)
        assertEquals(1.0f, profile.widthRatio, 0.0001f)
        assertEquals(1.0f, profile.heightRatio, 0.0001f)
        assertEquals(1.0f, profile.averageRatio, 0.0001f)
        assertEquals(4.0f, BaselineResolutionScaleUtil.scaleFromBaseline(4.0f, profile.averageRatio), 0.0001f)
        assertEquals(-33.0f, BaselineResolutionScaleUtil.inverseScaleFromBaseline(-33.0f, profile.averageRatio), 0.0001f)
    }

    @Test
    fun smallPhonePortrait_matchesExpectedAverageRatioAndDerivedValues() {
        val profile = BaselineResolutionScaleUtil.calculateAverageRatio(
            currentWidthPx = 720,
            currentHeightPx = 1280,
            baselinePortraitWidthPx = 1600,
            baselinePortraitHeightPx = 2560
        )

        assertFalse(profile.isLandscape)
        assertEquals(0.45f, profile.widthRatio, 0.0001f)
        assertEquals(0.50f, profile.heightRatio, 0.0001f)
        assertEquals(0.475f, profile.averageRatio, 0.0001f)
        assertEquals(1.9f, BaselineResolutionScaleUtil.scaleFromBaseline(4.0f, profile.averageRatio), 0.0001f)
        assertEquals(-69.47368f, BaselineResolutionScaleUtil.inverseScaleFromBaseline(-33.0f, profile.averageRatio), 0.0001f)
    }

    @Test
    fun smallPhoneLandscape_swapsBaselineAxesButKeepsSameAverageRatio() {
        val profile = BaselineResolutionScaleUtil.calculateAverageRatio(
            currentWidthPx = 1280,
            currentHeightPx = 720,
            baselinePortraitWidthPx = 1600,
            baselinePortraitHeightPx = 2560
        )

        assertTrue(profile.isLandscape)
        assertEquals(2560, profile.baselineWidthPx)
        assertEquals(1600, profile.baselineHeightPx)
        assertEquals(0.50f, profile.widthRatio, 0.0001f)
        assertEquals(0.45f, profile.heightRatio, 0.0001f)
        assertEquals(0.475f, profile.averageRatio, 0.0001f)
        assertEquals(1.9f, BaselineResolutionScaleUtil.scaleFromBaseline(4.0f, profile.averageRatio), 0.0001f)
        assertEquals(-69.47368f, BaselineResolutionScaleUtil.inverseScaleFromBaseline(-33.0f, profile.averageRatio), 0.0001f)
    }
}

