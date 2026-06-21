package com.gpgamelab.justpatience.ui.layout

import android.util.Log
import kotlin.math.roundToInt

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

        // OLD val statsW = ((modelW - margin * 2 - gap) / 2.25).toInt()
        val statsW = ((modelW) / 6.1f).toInt()

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
        // OLD val bottomControlsH = 168
        val bottomControlsH = 110
        // OLD val bottomAdsH = 136
        val bottomAdsH = 110

        // OLD val adsTop = modelH - margin - bottomAdsH
        val adsTop = modelH - bottomAdsH
        // OLD val controlsTop = adsTop - sectionGap - bottomControlsH
        val controlsTop = adsTop - bottomControlsH
        // OLD val statsTop = controlsTop - sectionGap - bottomStatsH
        val statsTop = margin //+ 40*margin

        // OLD val tableauTop = topY + topRowH + sectionGap
        val tableauTop = topRowH
        // OLD val tableauBottom = statsTop - sectionGap
        val tableauBottom = tableauTop + 742
        // OLD val tableauH = max(120, tableauBottom - tableauTop)
        val tableauH = 742
        // OLD val controlsW = modelW - margin * 2 - statsW - gap
        val controlsW = modelW - margin * 2 - gap

        val adSafetyRef = 8
        // OLD val adsLeft = margin
        val adsLeft = 0
        // OLD val adsW = modelW - margin * 2
        val adsW = modelW

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
                top = topY, // - (cardSpec.heightRef * .3f).toInt(), // OLD topY,
                width = foundationW,
                height = foundationH
            ),
            GroupId.TABLEAU to GroupBox(
                id = GroupId.TABLEAU,
                left = margin,
                top = tableauTop - (cardSpec.heightRef * 0.45f).toInt(), // - (cardSpec.heightRef * 1.05f).toInt(), // OLD tableauTop,
                width = modelW - margin * 2,
                height = tableauH
            ),
            GroupId.STATS to GroupBox(
                id = GroupId.STATS,
                left = modelW - margin - statsW, // OLD margin,
                top = statsTop,
                width = statsW,
                height = bottomStatsH
            ),
            GroupId.CONTROLS to GroupBox(
                id = GroupId.CONTROLS,
                left = margin, // -200, // OLD margin + statsW + gap,
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
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "modelW=${modelW} modelH=${modelH} deckCount=${deckCount}"
        )

        val margin = 20
        val gap = 16
        val sectionGap = 20
        val boxPad = 10
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "margin=${margin} gap=${gap} sectionGap=${sectionGap} boxPad=${boxPad}"
        )
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "cardSpec.wasteFanWidthRef=${cardSpec.wasteFanWidthRef}"
        )

        val drawWasteW = cardSpec.wasteFanWidthRef + boxPad * 2
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "drawWasteW=${drawWasteW}"
        )

        // Landscape cards are sized from screen height, not from the reference card spec.
        // GameBoardView's landscape height-budget formula gives:
        //   cardH ≈ screenH × (0.95 / 3.60) × 0.72  ≈  screenH × 0.189
        // In reference-model terms (modelH = 1152):
        //   estimatedCardH ≈ 1152 × 0.2  ≈ 230 ref-px   (0.2 rounds 0.189 up for safety)
        // Use whichever is larger – the spec-based or the height-budget-based estimate –
        // so the DRAW_WASTE box never squeezes two stacked cards together even on devices
        // where scaleY < 1 (wide/CLASSIC landscape phones).
        val estimatedLandscapeCardH = (modelH * 0.2f).toInt()
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "estimatedLandscapeCardH=${estimatedLandscapeCardH}"
        )
        val drawWasteH = max(
            (cardSpec.heightRef * 2) + gap + boxPad * 2,
            (estimatedLandscapeCardH * 2) + gap + boxPad * 2
        )
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "drawWasteH=${drawWasteH}"
        )

        val foundationCols = 2
        val foundationRows = 4
        val foundationCellGap = 10
        val foundationW = (foundationCols * cardSpec.widthRef) + ((foundationCols - 1) * foundationCellGap) + boxPad * 2
        val foundationH = (foundationRows * cardSpec.heightRef) + ((foundationRows - 1) * foundationCellGap) + boxPad * 2
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "foundationW=${foundationW}"
        )
        Log.d( "BoardGroupLayoutEngine ZYZZX",
            "foundationH=${foundationH}"
        )

        val bottomStatsH = 84
        val bottomControlsH = 126
        val bottomAdsH = 180

        val adsTop = modelH - bottomAdsH
        val controlsTop = adsTop - bottomControlsH
//        val statsTop = controlsTop - sectionGap - bottomStatsH
        val statsTop = margin //+ 40*margin
        val statsW = ((modelW) / 8.5f).toInt()

        val contentTop = margin

        val tableauH = 742
        val tableauLeft = margin + drawWasteW * 5 / 6 // + gap
//        val tableauRight = modelW - margin - foundationW - gap
//        val tableauW = max(180, tableauRight - tableauLeft)
        val tableauW = cardSpec.widthRef * ((deckCount-1)*2+8) - cardSpec.widthRef*1/3
        val tableauRight = tableauLeft + tableauW

        val adSafetyRef = 8

        return linkedMapOf(
            GroupId.DRAW_WASTE to GroupBox(
                id = GroupId.DRAW_WASTE,
                left = margin,
                top = contentTop,
                width = drawWasteW,
                height = drawWasteH            ),
            GroupId.TABLEAU to GroupBox(
                id = GroupId.TABLEAU,
                left = tableauLeft,
                top = contentTop,
                width = tableauW,
                height = tableauH
            ),
            GroupId.FOUNDATION to GroupBox(
                id = GroupId.FOUNDATION,
                left = tableauRight,
                top = contentTop,
                width = foundationW,
                height = foundationH
            ),
            GroupId.STATS to GroupBox(
                id = GroupId.STATS,
                left = modelW - margin - statsW,
                top = statsTop,
                width = (modelW - margin * 2 - gap) / 2,
                height = bottomStatsH
            ),
            GroupId.CONTROLS to GroupBox(
                id = GroupId.CONTROLS,
                left = margin,
                top = controlsTop,
                width = modelW/4,
                height = bottomControlsH
            ),
            GroupId.ADS to GroupBox(
                id = GroupId.ADS,
                left = modelW - 565, //margin,
                top = adsTop + adSafetyRef,
                width = 550, //modelW,
                height = bottomAdsH
            )
        )
    }

    private fun max(a: Int, b: Int): Int = if (a >= b) a else b
}


