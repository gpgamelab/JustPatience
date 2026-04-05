package com.gpgamelab.justpatience.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CouponFlightAnimatorTest {

    private val animator = CouponFlightAnimator()

    @Test
    fun `opening display points match exact requested size table`() {
        assertEquals(1f, animator.sizeMultiplierForElapsed(0L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(25L), 0.0001f)
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(50L), 0.0001f)
        assertEquals(2f, animator.sizeMultiplierForElapsed(75L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(100L), 0.0001f)
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(125L), 0.0001f)
        assertEquals(3f, animator.sizeMultiplierForElapsed(150L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(175L), 0.0001f)
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(200L), 0.0001f)
        assertEquals(4f, animator.sizeMultiplierForElapsed(225L), 0.0001f)
    }

    @Test
    fun `size remains at 4x through the middle hold window`() {
        assertEquals(4f, animator.sizeMultiplierForElapsed(225L), 0.0001f)
        assertEquals(4f, animator.sizeMultiplierForElapsed(250L), 0.0001f)
        assertEquals(4f, animator.sizeMultiplierForElapsed(600L), 0.0001f)
        assertEquals(4f, animator.sizeMultiplierForElapsed(1175L), 0.0001f)
    }

    @Test
    fun `closing display points match exact requested size table`() {
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(1200L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(1225L), 0.0001f)
        assertEquals(3f, animator.sizeMultiplierForElapsed(1250L), 0.0001f)
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(1275L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(1300L), 0.0001f)
        assertEquals(2f, animator.sizeMultiplierForElapsed(1325L), 0.0001f)
        assertEquals(5f / 3f, animator.sizeMultiplierForElapsed(1350L), 0.0001f)
        assertEquals(4f / 3f, animator.sizeMultiplierForElapsed(1375L), 0.0001f)
        assertEquals(1f, animator.sizeMultiplierForElapsed(1400L), 0.0001f)
    }

    @Test
    fun `size remains at 1x through final hold window`() {
        assertEquals(1f, animator.sizeMultiplierForElapsed(1400L), 0.0001f)
        assertEquals(1f, animator.sizeMultiplierForElapsed(1425L), 0.0001f)
        assertEquals(1f, animator.sizeMultiplierForElapsed(1475L), 0.0001f)
        assertEquals(1f, animator.sizeMultiplierForElapsed(1500L), 0.0001f)
    }

    @Test
    fun `progress snaps to 25 millisecond steps`() {
        assertEquals(0f, animator.sampledProgressForElapsed(0L), 0.0001f)
        assertEquals(0f, animator.sampledProgressForElapsed(24L), 0.0001f)
        assertEquals(25f / 1500f, animator.sampledProgressForElapsed(25L), 0.0001f)
        assertEquals(25f / 1500f, animator.sampledProgressForElapsed(49L), 0.0001f)
        assertEquals(50f / 1500f, animator.sampledProgressForElapsed(50L), 0.0001f)
        assertEquals(1475f / 1500f, animator.sampledProgressForElapsed(1499L), 0.0001f)
        assertEquals(1f, animator.sampledProgressForElapsed(1500L), 0.0001f)
    }

    @Test
    fun `sample index snaps to each 25 millisecond display point`() {
        assertEquals(0, animator.sampleIndexForElapsed(0L))
        assertEquals(0, animator.sampleIndexForElapsed(24L))
        assertEquals(1, animator.sampleIndexForElapsed(25L))
        assertEquals(9, animator.sampleIndexForElapsed(249L))
        assertEquals(10, animator.sampleIndexForElapsed(250L))
        assertEquals(47, animator.sampleIndexForElapsed(1175L))
        assertEquals(48, animator.sampleIndexForElapsed(1200L))
        assertEquals(56, animator.sampleIndexForElapsed(1400L))
        assertEquals(60, animator.sampleIndexForElapsed(1500L))
    }

    @Test
    fun `total runtime matches flight duration without midpoint pause`() {
        assertEquals(1_500L, CouponFlightAnimator.TOTAL_RUNTIME_MS)
    }

    @Test
    fun `landscape arc uses configured minimum for short flights`() {
        animator.configureFlightForTest(isLandscape = true, landscapeMinArcPx = 120f)

        val arcHeight = animator.calculateArcHeight(distance = 10f, cardW = 80f, cardH = 120f)
        assertEquals(120f, arcHeight, 0.0001f)
    }

    @Test
    fun `portrait arc ignores landscape minimum`() {
        animator.configureFlightForTest(isLandscape = false, landscapeMinArcPx = 120f)

        val arcHeight = animator.calculateArcHeight(distance = 10f, cardW = 80f, cardH = 120f)
        // Portrait keeps the original smaller range for short flights.
        assertEquals(12f, arcHeight, 0.0001f)
    }
}






