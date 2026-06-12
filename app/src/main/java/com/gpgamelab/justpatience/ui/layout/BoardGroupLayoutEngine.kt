package com.gpgamelab.justpatience.ui.layout

/**
 * Phase 2 bootstrap: computes reference-space group boxes for the six board zones.
 *
 * This engine intentionally keeps math integer-only and deterministic.
 * Mirroring is applied later via GroupBox.toScreenRectF(..., isMirrored = true).
 */
object BoardGroupLayoutEngine {

    fun computeGroupBoxes(config: LayoutConfig, cardSpec: CardSpec = defaultCardSpec(config.normalizedDeckCount)): Map<GroupId, GroupBox> {
        return if (config.orientation == BoardOrientation.PORTRAIT) {
            computePortrait(config, cardSpec)
        } else {
            computeLandscape(config, cardSpec)
        }
    }

    private fun computePortrait(config: LayoutConfig, cardSpec: CardSpec): Map<GroupId, GroupBox> {
        val modelW = config.orientation.modelWidth
        val modelH = config.orientation.modelHeight
        val deckCount = config.normalizedDeckCount

        val margin = 24
        val gap = 18
        val sectionGap = 24
        val drawWastePad = 12
        val foundationPad = 12

        val statsW = ((modelW - margin * 2 - gap) / 2.25).toInt()

        val drawWasteW = cardSpec.wasteFanWidthRef + drawWastePad * 2

        // Portrait cards are WIDTH-constrained (narrow phone screen), not height-constrained.
        // GameBoardView's portrait width-budget formula gives:
        //   cardW ≈ (modelW × 0.75 / columns) × 1.4
        //   cardH  = cardW × 1.7
        // For 1-deck (8 cols) at modelW=1152: cardH ≈ 133 × 1.7 ≈ 226 ref-px
        // For 2-deck (10 cols) at modelW=1152: cardH ≈ 108 × 1.7 ≈ 185 ref-px
        // Real phones have scaleY ≫ 1 (tall portrait screens), which gives the DRAW_WASTE
        // group extra vertical room.  For extra safety on unusual square/short devices,
        // use the larger of the spec-based or width-budget-based estimate.
        val portraitColumns = if (config.normalizedDeckCount == 2) 10 else 8
        val widthBudgetCardW = ((modelW * 0.75f - (portraitColumns + 1)) / (portraitColumns + 1)) * 1.4f
        val widthBudgetCardH = (widthBudgetCardW * 1.7f).toInt()
        val drawWasteH = max(
            (cardSpec.heightRef * 2) + gap + drawWastePad * 2,
            (widthBudgetCardH * 2) + gap + drawWastePad * 2
        )

        val foundationCols = 4
        val foundationRows = if (deckCount == 2) 2 else 2
        val foundationCellGap = 12
        val foundationW = (foundationCols * cardSpec.widthRef) + ((foundationCols - 1) * foundationCellGap) + foundationPad * 2
        val foundationH = (foundationRows * cardSpec.heightRef) + ((foundationRows - 1) * foundationCellGap) + foundationPad * 2

        val topRowH = max(drawWasteH, foundationH)
        val topY = margin

        val drawWasteLeft = margin
//        val foundationLeft = modelW - margin - foundationW - statsW
        val foundationLeft = drawWasteW + sectionGap

        val bottomStatsH = 96
        val bottomControlsH = 168
        val bottomAdsH = 136

        val adsTop = modelH - margin - bottomAdsH
        val controlsTop = adsTop - sectionGap - bottomControlsH
        val statsTop = controlsTop - sectionGap - bottomStatsH

        val tableauTop = topY + topRowH + sectionGap
        val tableauBottom = statsTop - sectionGap
        val tableauH = max(120, tableauBottom - tableauTop)

        val controlsW = modelW - margin * 2 - statsW - gap

        val adSafetyRef = 8
        val adsLeft = margin
        val adsW = modelW - margin * 2

        return linkedMapOf(
            GroupId.DRAW_WASTE to GroupBox(
                id = GroupId.DRAW_WASTE,
                left = drawWasteLeft,
                top = topY,
                width = drawWasteW,
                height = drawWasteH
            ),
            GroupId.FOUNDATION to GroupBox(
                id = GroupId.FOUNDATION,
                left = foundationLeft,
                top = topY,
                width = foundationW,
                height = foundationH
            ),
            GroupId.TABLEAU to GroupBox(
                id = GroupId.TABLEAU,
                left = margin,
                top = tableauTop,
                width = modelW - margin * 2,
                height = tableauH
            ),
            GroupId.STATS to GroupBox(
                id = GroupId.STATS,
                left = margin,
                top = statsTop,
                width = statsW,
                height = bottomStatsH
            ),
            GroupId.CONTROLS to GroupBox(
                id = GroupId.CONTROLS,
                left = margin + statsW + gap,
                top = controlsTop,
                width = controlsW,
                height = bottomControlsH
            ),
            GroupId.ADS to GroupBox(
                id = GroupId.ADS,
                left = adsLeft,
                top = adsTop + adSafetyRef,
                width = adsW,
                height = bottomAdsH - adSafetyRef
            )
        )
    }

    private fun computeLandscape(config: LayoutConfig, cardSpec: CardSpec): Map<GroupId, GroupBox> {
        val modelW = config.orientation.modelWidth
        val modelH = config.orientation.modelHeight
        val deckCount = config.normalizedDeckCount

        val margin = 20
        val gap = 16
        val sectionGap = 20
        val boxPad = 10

        val drawWasteW = cardSpec.wasteFanWidthRef + boxPad * 2

        // Landscape cards are sized from screen height, not from the reference card spec.
        // GameBoardView's landscape height-budget formula gives:
        //   cardH ≈ screenH × (0.95 / 3.60) × 0.72  ≈  screenH × 0.189
        // In reference-model terms (modelH = 1152):
        //   estimatedCardH ≈ 1152 × 0.2  ≈ 230 ref-px   (0.2 rounds 0.189 up for safety)
        // Use whichever is larger – the spec-based or the height-budget-based estimate –
        // so the DRAW_WASTE box never squeezes two stacked cards together even on devices
        // where scaleY < 1 (wide/CLASSIC landscape phones).
        val estimatedLandscapeCardH = (modelH * 0.2f).toInt()
        val drawWasteH = max(
            (cardSpec.heightRef * 2) + gap + boxPad * 2,
            (estimatedLandscapeCardH * 2) + gap + boxPad * 2
        )

        val foundationCols = 2
        val foundationRows = if (deckCount == 2) 4 else 4
        val foundationCellGap = 10
        val foundationW = (foundationCols * cardSpec.widthRef) + ((foundationCols - 1) * foundationCellGap) + boxPad * 2
        val foundationH = (foundationRows * cardSpec.heightRef) + ((foundationRows - 1) * foundationCellGap) + boxPad * 2

        val bottomStatsH = 84
        val bottomControlsH = 126
        val bottomAdsH = 110

        val adsTop = modelH - margin - bottomAdsH
        val controlsTop = adsTop - sectionGap - bottomControlsH
        val statsTop = controlsTop - sectionGap - bottomStatsH

        val contentTop = margin
        val contentBottom = statsTop - sectionGap
        val contentH = max(100, contentBottom - contentTop)

        val tableauLeft = margin + drawWasteW + gap
//        val tableauRight = modelW - margin - foundationW - gap
//        val tableauW = max(180, tableauRight - tableauLeft)
        val tableauW = cardSpec.widthRef * ((deckCount-1)*2+8)
        val tableauRight = tableauLeft + tableauW

        val adSafetyRef = 8

        return linkedMapOf(
            GroupId.DRAW_WASTE to GroupBox(
                id = GroupId.DRAW_WASTE,
                left = margin,
                top = contentTop,
                width = drawWasteW,
                height = minOf(drawWasteH, contentH)
            ),
            GroupId.TABLEAU to GroupBox(
                id = GroupId.TABLEAU,
                left = tableauLeft,
                top = contentTop,
                width = tableauW,
                height = contentH
            ),
            GroupId.FOUNDATION to GroupBox(
                id = GroupId.FOUNDATION,
//                left = modelW - margin - foundationW,
                left = tableauRight,
                top = contentTop,
                width = foundationW,
                height = minOf(foundationH, contentH)
            ),
            GroupId.STATS to GroupBox(
                id = GroupId.STATS,
                left = margin,
                top = statsTop,
                width = (modelW - margin * 2 - gap) / 2,
                height = bottomStatsH
            ),
            GroupId.CONTROLS to GroupBox(
                id = GroupId.CONTROLS,
                left = margin + (modelW - margin * 2 - gap) / 2 + gap,
                top = controlsTop,
                width = modelW - margin * 2 - ((modelW - margin * 2 - gap) / 2 + gap),
                height = bottomControlsH
            ),
            GroupId.ADS to GroupBox(
                id = GroupId.ADS,
                left = margin,
                top = adsTop + adSafetyRef,
                width = modelW - margin * 2,
                height = bottomAdsH - adSafetyRef
            )
        )
    }

    private fun max(a: Int, b: Int): Int = if (a >= b) a else b
}


