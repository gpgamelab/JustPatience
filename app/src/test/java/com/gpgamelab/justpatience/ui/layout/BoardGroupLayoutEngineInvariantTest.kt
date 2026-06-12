package com.gpgamelab.justpatience.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardGroupLayoutEngineInvariantTest {

    /**
     * The DRAW_WASTE group box must always hold two stacked cards (waste on top, stock below)
     * with at least a minimal gap between them.
     *
     * Verified at the reference-model level using the default card spec for each deck count.
     */
    @Test
    fun drawWasteBoxFitsTwoCardsAtReferenceLevel() {
        val minGapRef = 8 // minimum gap between waste and stock in reference pixels
        listOf(1, 2).forEach { deckCount ->
            BoardOrientation.entries.forEach { orientation ->
                val config = LayoutConfig(orientation, deckCount, isMirrored = false)
                val cardSpec = defaultCardSpec(deckCount)
                val boxes = BoardGroupLayoutEngine.computeGroupBoxes(config, cardSpec)
                val drawWaste = boxes.getValue(GroupId.DRAW_WASTE)
                val minRequired = cardSpec.heightRef * 2 + minGapRef
                assertTrue(
                    "$orientation/$deckCount DRAW_WASTE height ${drawWaste.height} < minRequired $minRequired",
                    drawWaste.height >= minRequired
                )
            }
        }
    }

    /**
     * In landscape, GameBoardView computes card heights from screen height using a
     * height-budget formula that can produce cards ~30% taller than the reference card spec
     * on wide phones (scaleY < 1, e.g. 2400×1080 → scaleY ≈ 0.94).
     *
     * This test confirms that the DRAW_WASTE landscape box is tall enough to hold two of
     * these "real-world" cards, using the same conservative estimate baked into the engine
     * (modelH × 0.2 per card, matching GameBoardView's (0.95/3.60×0.72) formula).
     *
     * Failing here means `computeLandscape` needs a larger `drawWasteH`.
     */
    @Test
    fun drawWasteBoxSufficientForLandscapeRealWorldCardSizes() {
        val minGapPx = 8
        listOf(1, 2).forEach { deckCount ->
            val config = LayoutConfig(BoardOrientation.LANDSCAPE, deckCount, isMirrored = false)
            val cardSpec = defaultCardSpec(deckCount)
            val boxes = BoardGroupLayoutEngine.computeGroupBoxes(config, cardSpec)
            val drawWaste = boxes.getValue(GroupId.DRAW_WASTE)

            val modelH = BoardOrientation.LANDSCAPE.modelHeight // 1152
            // Estimated card height from GameBoardView's landscape height-budget formula:
            //   cardH ≈ viewH × 0.95 / 3.60 × 0.72  ≈  viewH × 0.189
            // At modelH = 1152:  1152 × 0.189 ≈ 218 px.  Use 0.2 (conservative round-up).
            val estimatedCardH = (modelH * 0.2f).toInt()
            val minRequired = estimatedCardH * 2 + minGapPx
            assertTrue(
                "landscape/$deckCount DRAW_WASTE height ${drawWaste.height} < minRequired $minRequired " +
                    "(estimatedCardH=$estimatedCardH based on modelH=$modelH × 0.2)",
                drawWaste.height >= minRequired
            )
        }
    }

    /**
     * In portrait, GameBoardView card heights are WIDTH-constrained, not height-constrained.
     * The binding constraint is the narrow phone width, giving smaller cards than the height
     * budget alone would suggest.  Real portrait phones also have scaleY > 1 (tall screens)
     * which gives the DRAW_WASTE group extra vertical room.
     *
     * This test replicates GameBoardView's portrait width-budget formula at reference-model
     * scale and verifies the DRAW_WASTE box fits two such cards with a minimum gap.
     *
     * Failing here means `computePortrait` needs a larger `drawWasteH`.
     */
    @Test
    fun drawWasteBoxSufficientForPortraitWidthBudgetCardSizes() {
        val minGapPx = 8
        listOf(1, 2).forEach { deckCount ->
            val config = LayoutConfig(BoardOrientation.PORTRAIT, deckCount, isMirrored = false)
            val cardSpec = defaultCardSpec(deckCount)
            val boxes = BoardGroupLayoutEngine.computeGroupBoxes(config, cardSpec)
            val drawWaste = boxes.getValue(GroupId.DRAW_WASTE)

            val modelW = BoardOrientation.PORTRAIT.modelWidth  // 1152
            val columns = if (deckCount == 2) 10 else 8
            // GameBoardView.calculateWidthLimitedCardWidth (portrait) at reference width:
            //   usableWidth = modelW × 0.75   boardFraction
            //   cardW = ((usableWidth - (columns+1)) / (columns+1)) × 1.4 × 1.0  (portrait multiplier)
            //   cardH = cardW × 1.7
            val widthBudgetCardW = ((modelW * 0.75f - (columns + 1)) / (columns + 1)) * 1.4f
            val widthBudgetCardH = (widthBudgetCardW * 1.7f).toInt()
            val minRequired = widthBudgetCardH * 2 + minGapPx
            assertTrue(
                "portrait/$deckCount DRAW_WASTE height ${drawWaste.height} < minRequired $minRequired " +
                    "(widthBudgetCardH=$widthBudgetCardH at modelW=$modelW, $columns cols)",
                drawWaste.height >= minRequired
            )
        }
    }

    @Test
    fun groupBoxesStayInsideModelAndKeepVerticalOrder() {
        val cases = listOf(
            LayoutConfig(BoardOrientation.PORTRAIT, deckCount = 1, isMirrored = false),
            LayoutConfig(BoardOrientation.PORTRAIT, deckCount = 2, isMirrored = true),
            LayoutConfig(BoardOrientation.LANDSCAPE, deckCount = 1, isMirrored = false),
            LayoutConfig(BoardOrientation.LANDSCAPE, deckCount = 2, isMirrored = true)
        )

        cases.forEach { config ->
            val boxes = BoardGroupLayoutEngine.computeGroupBoxes(config)
            assertEquals(GroupId.entries.toSet(), boxes.keys.toSet())

            val modelW = config.orientation.modelWidth
            val modelH = config.orientation.modelHeight
            boxes.values.forEach { box ->
                assertTrue("${config.orientation}/${config.deckCount} ${box.id} left", box.left >= 0)
                assertTrue("${config.orientation}/${config.deckCount} ${box.id} top", box.top >= 0)
                assertTrue("${config.orientation}/${config.deckCount} ${box.id} right", box.right <= modelW)
                assertTrue("${config.orientation}/${config.deckCount} ${box.id} bottom", box.bottom <= modelH)
            }

            val tableau = boxes.getValue(GroupId.TABLEAU)
            val stats = boxes.getValue(GroupId.STATS)
            val controls = boxes.getValue(GroupId.CONTROLS)
            val ads = boxes.getValue(GroupId.ADS)
            assertTrue("tableau before stats", tableau.top < stats.top)
            assertTrue("stats before controls", stats.top < controls.top)
            assertTrue("controls before ads", controls.top < ads.top)

            val recomputed = BoardGroupLayoutEngine.computeGroupBoxes(config)
            assertEquals(boxes, recomputed)
        }
    }
}




