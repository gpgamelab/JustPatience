package com.gpgamelab.justpatience.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.gpgamelab.justpatience.R
import java.util.Locale

class DevelopMenuDialogFragment : DialogFragment() {

    data class ExpandState(
        val starburstExpanded: Boolean = false,
        val popupExpanded: Boolean = false,
        val cardPilesExpanded: Boolean = false,
        val adsRewardsExpanded: Boolean = false,
        val shuffleExpanded: Boolean = false,
        val aspectRatioExpanded: Boolean = false
    )

    interface Host {
        fun testerStarburstPivotOffsetX(): Int
        fun testerStarburstPivotOffsetY(): Int
        fun onTesterAdjustStarburstPivotOffsetX(delta: Int)
        fun onTesterAdjustStarburstPivotOffsetY(delta: Int)
        fun onTesterSetStarburstPivotOffsetX(value: Int)
        fun onTesterSetStarburstPivotOffsetY(value: Int)
        fun testerStarburstPositionX(): Int
        fun testerStarburstPositionY(): Int
        fun onTesterAdjustStarburstPositionX(delta: Int)
        fun onTesterAdjustStarburstPositionY(delta: Int)
        fun onTesterSetStarburstPositionX(value: Int)
        fun onTesterSetStarburstPositionY(value: Int)
        fun testerStarburstScale(): Float
        fun onTesterAdjustStarburstScale(delta: Float)
        fun onTesterSetStarburstScale(value: Float)
        fun testerStarburstRotationDurationMs(): Int
        fun onTesterAdjustStarburstRotationDurationMs(delta: Int)
        fun onTesterSetStarburstRotationDurationMs(value: Int)
        fun testerIsStarburstRotationEnabled(): Boolean
        fun onTesterSetStarburstRotationEnabled(enabled: Boolean)
        fun onTesterApplyAutoStarburstLayout()

        fun devGemImageHeightDp(): Float
        fun devGemOffsetXDp(): Float
        fun devGemOffsetYDp(): Float
        fun devTicketImageHeightDp(): Float
        fun devTicketOffsetXDp(): Float
        fun devTicketOffsetYDp(): Float
        fun devWandImageHeightDp(): Float
        fun devWandOffsetXDp(): Float
        fun devWandOffsetYDp(): Float
        fun devRewardTextSizeSp(): Float
        fun devGemNumberOffsetXDp(): Float
        fun devGemNumberOffsetYDp(): Float
        fun devTicketNumberOffsetXDp(): Float
        fun devTicketNumberOffsetYDp(): Float
        fun devWandNumberOffsetXDp(): Float
        fun devWandNumberOffsetYDp(): Float
        fun devButtonRowOffsetXDp(): Float
        fun devButtonRowOffsetYDp(): Float
        fun devPopupButton0ScaleX(): Float
        fun devPopupButton0ScaleY(): Float
        fun devPopupButton0Scale(): Float
        fun devPopupButton1ScaleX(): Float
        fun devPopupButton1ScaleY(): Float
        fun devPopupButton1Scale(): Float
        fun devPopupButton2ScaleX(): Float
        fun devPopupButton2ScaleY(): Float
        fun devPopupButton2Scale(): Float
        fun devPopupDescriptionTextSizeSp(): Float
        fun devPopupDescriptionOffsetXDp(): Float
        fun devPopupDescriptionOffsetYDp(): Float
        fun devVictoryTextSizeSp(): Float
        fun devVictoryOffsetXDp(): Float
        fun devVictoryOffsetYDp(): Float

        fun onDevSetGemImageHeight(value: Float)
        fun onDevSetGemOffsetX(value: Float)
        fun onDevSetGemOffsetY(value: Float)
        fun onDevSetTicketImageHeight(value: Float)
        fun onDevSetTicketOffsetX(value: Float)
        fun onDevSetTicketOffsetY(value: Float)
        fun onDevSetWandImageHeight(value: Float)
        fun onDevSetWandOffsetX(value: Float)
        fun onDevSetWandOffsetY(value: Float)
        fun onDevSetRewardTextSize(value: Float)
        fun onDevSetGemNumberOffsetX(value: Float)
        fun onDevSetGemNumberOffsetY(value: Float)
        fun onDevSetTicketNumberOffsetX(value: Float)
        fun onDevSetTicketNumberOffsetY(value: Float)
        fun onDevSetWandNumberOffsetX(value: Float)
        fun onDevSetWandNumberOffsetY(value: Float)
        fun onDevSetButtonRowOffsetX(value: Float)
        fun onDevSetButtonRowOffsetY(value: Float)
        fun onDevSetPopupButton0ScaleX(value: Float)
        fun onDevSetPopupButton0ScaleY(value: Float)
        fun onDevSetPopupButton0Scale(value: Float)
        fun onDevSetPopupButton1ScaleX(value: Float)
        fun onDevSetPopupButton1ScaleY(value: Float)
        fun onDevSetPopupButton1Scale(value: Float)
        fun onDevSetPopupButton2ScaleX(value: Float)
        fun onDevSetPopupButton2ScaleY(value: Float)
        fun onDevSetPopupButton2Scale(value: Float)
        fun onDevSetPopupDescriptionTextSize(value: Float)
        fun onDevSetPopupDescriptionOffsetX(value: Float)
        fun onDevSetPopupDescriptionOffsetY(value: Float)
        fun onDevSetVictoryTextSize(value: Float)
        fun onDevSetVictoryOffsetX(value: Float)
        fun onDevSetVictoryOffsetY(value: Float)
        fun onDevApplyAutoWinPopupRatios()

        fun devLockedPileAdOffsetXPortraitPx(): Float
        fun devLockedPileAdOffsetYPortraitPx(): Float
        fun devLockedPileAdScaleXPortrait(): Float
        fun devLockedPileAdScaleYPortrait(): Float
        fun devLockedPileAdOffsetXLandscapePx(): Float
        fun devLockedPileAdOffsetYLandscapePx(): Float
        fun devLockedPileAdScaleXLandscape(): Float
        fun devLockedPileAdScaleYLandscape(): Float
        fun onDevSetLockedPileAdOffsetXPortraitPx(value: Float)
        fun onDevSetLockedPileAdOffsetYPortraitPx(value: Float)
        fun onDevSetLockedPileAdScaleXPortrait(value: Float)
        fun onDevSetLockedPileAdScaleYPortrait(value: Float)
        fun onDevSetLockedPileAdOffsetXLandscapePx(value: Float)
        fun onDevSetLockedPileAdOffsetYLandscapePx(value: Float)
        fun onDevSetLockedPileAdScaleXLandscape(value: Float)
        fun onDevSetLockedPileAdScaleYLandscape(value: Float)

        fun devLandscapePileOverallOffsetXDp(): Float
        fun devLandscapePileOverallOffsetYDp(): Float
        fun devLandscapePileFoundationOffsetXDp(): Float
        fun devLandscapePileFoundationOffsetYDp(): Float
        fun devLandscapePileDrawWasteOffsetXDp(): Float
        fun devLandscapePileDrawWasteOffsetYDp(): Float
        fun devLandscapePileStockOffsetXDp(): Float
        fun devLandscapePileStockOffsetYDp(): Float
        fun devLandscapePileWasteOffsetXDp(): Float
        fun devLandscapePileWasteOffsetYDp(): Float
        fun devLandscapePileTableauOffsetXDp(): Float
        fun devLandscapePileTableauOffsetYDp(): Float
        fun onDevSetLandscapePileOverallOffsetX(value: Float)
        fun onDevSetLandscapePileOverallOffsetY(value: Float)
        fun onDevSetLandscapePileFoundationOffsetX(value: Float)
        fun onDevSetLandscapePileFoundationOffsetY(value: Float)
        fun onDevSetLandscapePileDrawWasteOffsetX(value: Float)
        fun onDevSetLandscapePileDrawWasteOffsetY(value: Float)
        fun onDevSetLandscapePileStockOffsetX(value: Float)
        fun onDevSetLandscapePileStockOffsetY(value: Float)
        fun onDevSetLandscapePileWasteOffsetX(value: Float)
        fun onDevSetLandscapePileWasteOffsetY(value: Float)
        fun onDevSetLandscapePileTableauOffsetX(value: Float)
        fun onDevSetLandscapePileTableauOffsetY(value: Float)

        fun devAspectPortraitSlimXDp(): Float
        fun devAspectPortraitClassicXDp(): Float
        fun devAspectPortraitBroadXDp(): Float
        fun devAspectPortraitSquareXDp(): Float
        fun devPortraitPileOverallOffsetXDp(): Float
        fun devPortraitPileOverallOffsetYDp(): Float
        fun devPortraitPileFoundationOffsetXDp(): Float
        fun devPortraitPileFoundationOffsetYDp(): Float
        fun devPortraitPileDrawWasteOffsetXDp(): Float
        fun devPortraitPileDrawWasteOffsetYDp(): Float
        fun devPortraitPileStockOffsetXDp(): Float
        fun devPortraitPileStockOffsetYDp(): Float
        fun devPortraitPileWasteOffsetXDp(): Float
        fun devPortraitPileWasteOffsetYDp(): Float
        fun devPortraitPileTableauOffsetXDp(): Float
        fun devPortraitPileTableauOffsetYDp(): Float
        fun onDevSetPortraitPileOverallOffsetX(value: Float)
        fun onDevSetPortraitPileOverallOffsetY(value: Float)
        fun onDevSetPortraitPileFoundationOffsetX(value: Float)
        fun onDevSetPortraitPileFoundationOffsetY(value: Float)
        fun onDevSetPortraitPileDrawWasteOffsetX(value: Float)
        fun onDevSetPortraitPileDrawWasteOffsetY(value: Float)
        fun onDevSetPortraitPileStockOffsetX(value: Float)
        fun onDevSetPortraitPileStockOffsetY(value: Float)
        fun onDevSetPortraitPileWasteOffsetX(value: Float)
        fun onDevSetPortraitPileWasteOffsetY(value: Float)
        fun onDevSetPortraitPileTableauOffsetX(value: Float)
        fun onDevSetPortraitPileTableauOffsetY(value: Float)

        fun devAspectLandscapeSlimXDp(): Float
        fun devAspectLandscapeClassicXDp(): Float
        fun devAspectLandscapeBroadXDp(): Float
        fun devAspectLandscapeSquareXDp(): Float
        fun devLandscapeBannerSmallWidthDp(): Float
        fun devLandscapeBannerSmallHeightDp(): Float
        fun devLandscapeBannerMediumWidthDp(): Float
        fun devLandscapeBannerMediumHeightDp(): Float
        fun devLandscapeBannerLargeWidthDp(): Float
        fun devLandscapeBannerLargeHeightDp(): Float
        fun devSmallDeviceLandscapeBannerOffsetXDp(): Float
        fun devSmallDeviceLandscapeBannerOffsetYDp(): Float
        fun devMediumDeviceLandscapeBannerOffsetXDp(): Float
        fun devMediumDeviceLandscapeBannerOffsetYDp(): Float
        fun devLargeDeviceLandscapeBannerOffsetXDp(): Float
        fun devLargeDeviceLandscapeBannerOffsetYDp(): Float
        fun devScoreboardOffsetXDp(): Float
        fun devScoreboardOffsetYDp(): Float
        fun devGemRewardOffsetXDp(): Float
        fun devGemRewardOffsetYDp(): Float
        fun devTicketRewardOffsetXDp(): Float
        fun devTicketRewardOffsetYDp(): Float
        fun devUndoControlScale(): Float
        fun devUndoControlOffsetXDp(): Float
        fun devUndoControlOffsetYDp(): Float
        fun devRedoControlScale(): Float
        fun devRedoControlOffsetXDp(): Float
        fun devRedoControlOffsetYDp(): Float
        fun devHintControlScale(): Float
        fun devHintControlOffsetXDp(): Float
        fun devHintControlOffsetYDp(): Float
        fun devMagicWandControlScale(): Float
        fun devMagicWandControlOffsetXDp(): Float
        fun devMagicWandControlOffsetYDp(): Float
        fun devPlayControlScale(): Float
        fun devPlayControlOffsetXDp(): Float
        fun devPlayControlOffsetYDp(): Float
        fun devAutoControlScale(): Float
        fun devAutoControlOffsetXDp(): Float
        fun devAutoControlOffsetYDp(): Float
        fun devGemRewardScale(): Float
        fun devGemRewardCounterOffsetXDp(): Float
        fun devGemRewardCounterOffsetYDp(): Float
        fun devGemRewardCounterScale(): Float
        fun devTicketRewardScale(): Float
        fun devTicketRewardCounterOffsetXDp(): Float
        fun devTicketRewardCounterOffsetYDp(): Float
        fun devTicketRewardCounterScale(): Float
        fun devLandscapeBannerAdBoxChoiceLabel(): String
        fun onDevSetLandscapeBannerSmallWidthDp(value: Float)
        fun onDevSetLandscapeBannerSmallHeightDp(value: Float)
        fun onDevSetLandscapeBannerMediumWidthDp(value: Float)
        fun onDevSetLandscapeBannerMediumHeightDp(value: Float)
        fun onDevSetLandscapeBannerLargeWidthDp(value: Float)
        fun onDevSetLandscapeBannerLargeHeightDp(value: Float)
        fun onDevSetSmallDeviceLandscapeBannerOffsetX(value: Float)
        fun onDevSetSmallDeviceLandscapeBannerOffsetY(value: Float)
        fun onDevSetMediumDeviceLandscapeBannerOffsetX(value: Float)
        fun onDevSetMediumDeviceLandscapeBannerOffsetY(value: Float)
        fun onDevSetLargeDeviceLandscapeBannerOffsetX(value: Float)
        fun onDevSetLargeDeviceLandscapeBannerOffsetY(value: Float)
        fun onDevSetScoreboardOffsetX(value: Float)
        fun onDevSetScoreboardOffsetY(value: Float)
        fun onDevSetGemRewardOffsetX(value: Float)
        fun onDevSetGemRewardOffsetY(value: Float)
        fun onDevSetTicketRewardOffsetX(value: Float)
        fun onDevSetTicketRewardOffsetY(value: Float)
        fun onDevSetUndoControlScale(value: Float)
        fun onDevSetUndoControlOffsetX(value: Float)
        fun onDevSetUndoControlOffsetY(value: Float)
        fun onDevSetRedoControlScale(value: Float)
        fun onDevSetRedoControlOffsetX(value: Float)
        fun onDevSetRedoControlOffsetY(value: Float)
        fun onDevSetHintControlScale(value: Float)
        fun onDevSetHintControlOffsetX(value: Float)
        fun onDevSetHintControlOffsetY(value: Float)
        fun onDevSetMagicWandControlScale(value: Float)
        fun onDevSetMagicWandControlOffsetX(value: Float)
        fun onDevSetMagicWandControlOffsetY(value: Float)
        fun onDevSetPlayControlScale(value: Float)
        fun onDevSetPlayControlOffsetX(value: Float)
        fun onDevSetPlayControlOffsetY(value: Float)
        fun onDevSetAutoControlScale(value: Float)
        fun onDevSetAutoControlOffsetX(value: Float)
        fun onDevSetAutoControlOffsetY(value: Float)
        fun onDevSetGemRewardScale(value: Float)
        fun onDevSetGemRewardCounterOffsetX(value: Float)
        fun onDevSetGemRewardCounterOffsetY(value: Float)
        fun onDevSetGemRewardCounterScale(value: Float)
        fun onDevSetTicketRewardScale(value: Float)
        fun onDevSetTicketRewardCounterOffsetX(value: Float)
        fun onDevSetTicketRewardCounterOffsetY(value: Float)
        fun onDevSetTicketRewardCounterScale(value: Float)
        fun onDevCycleBannerBoxChoice()

        // Shuffle/deal timing
        fun devShuffleSecondClipDelayMs(): Float
        fun devShuffleTailDelayMs(): Float
        fun devDealCardIntervalMs(): Float
        fun onDevSetShuffleSecondClipDelayMs(value: Float)
        fun onDevSetShuffleTailDelayMs(value: Float)
        fun onDevSetDealCardIntervalMs(value: Float)

        // Aspect-ratio category pile trims
        fun devAspectPortraitSlimYDp(): Float
        fun devAspectPortraitClassicYDp(): Float
        fun devAspectPortraitBroadYDp(): Float
        fun devAspectPortraitSquareYDp(): Float
        fun devAspectLandscapeSlimYDp(): Float
        fun devAspectLandscapeClassicYDp(): Float
        fun devAspectLandscapeBroadYDp(): Float
        fun devAspectLandscapeSquareYDp(): Float
        fun onDevSetAspectPortraitSlimX(value: Float)
        fun onDevSetAspectPortraitClassicX(value: Float)
        fun onDevSetAspectPortraitBroadX(value: Float)
        fun onDevSetAspectPortraitSquareX(value: Float)
        fun onDevSetAspectPortraitSlimY(value: Float)
        fun onDevSetAspectPortraitClassicY(value: Float)
        fun onDevSetAspectPortraitBroadY(value: Float)
        fun onDevSetAspectPortraitSquareY(value: Float)
        fun onDevSetAspectLandscapeSlimX(value: Float)
        fun onDevSetAspectLandscapeClassicX(value: Float)
        fun onDevSetAspectLandscapeBroadX(value: Float)
        fun onDevSetAspectLandscapeSquareX(value: Float)
        fun onDevSetAspectLandscapeSlimY(value: Float)
        fun onDevSetAspectLandscapeClassicY(value: Float)
        fun onDevSetAspectLandscapeBroadY(value: Float)
        fun onDevSetAspectLandscapeSquareY(value: Float)
        fun devCurrentAspectCategoryLabel(): String

        fun onDevExpandStateChanged(state: ExpandState)
    }

    private var btnStarburstPivotXValue: MaterialButton? = null
    private var btnStarburstPivotYValue: MaterialButton? = null
    private var btnStarburstPositionXValue: MaterialButton? = null
    private var btnStarburstPositionYValue: MaterialButton? = null
    private var btnStarburstScaleValue: MaterialButton? = null
    private var btnStarburstRotationSpeedValue: MaterialButton? = null
    private var tvStarburstRotationStatus: TextView? = null
    private var btnStarburstRotationToggle: MaterialButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_develop_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = activity as? Host ?: return

        btnStarburstPivotXValue = view.findViewById(R.id.btn_starburst_pivot_x_value)
        btnStarburstPivotYValue = view.findViewById(R.id.btn_starburst_pivot_y_value)
        btnStarburstPositionXValue = view.findViewById(R.id.btn_starburst_position_x_value)
        btnStarburstPositionYValue = view.findViewById(R.id.btn_starburst_position_y_value)
        btnStarburstScaleValue = view.findViewById(R.id.btn_starburst_scale_value)
        btnStarburstRotationSpeedValue = view.findViewById(R.id.btn_starburst_rotation_speed_value)
        tvStarburstRotationStatus = view.findViewById(R.id.tv_starburst_rotation_status)
        btnStarburstRotationToggle = view.findViewById(R.id.btn_starburst_rotation_toggle)

        val starburstHeader = view.findViewById<View>(R.id.layout_develop_starburst_header)
        val starburstArrow = view.findViewById<TextView>(R.id.tv_develop_starburst_arrow)
        val starburstContent = view.findViewById<LinearLayout>(R.id.layout_develop_starburst_content)
        var expandState = readExpandStateFromArgs()
        var starburstExpanded = expandState.starburstExpanded
        setSectionExpanded(starburstContent, starburstArrow, starburstExpanded)
        starburstHeader.setOnClickListener {
            starburstExpanded = !starburstExpanded
            setSectionExpanded(starburstContent, starburstArrow, starburstExpanded)
            expandState = expandState.copy(starburstExpanded = starburstExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val popupHeader = view.findViewById<View>(R.id.layout_develop_popup_header)
        val popupArrow = view.findViewById<TextView>(R.id.tv_develop_popup_arrow)
        val popupContent = view.findViewById<LinearLayout>(R.id.layout_develop_popup_content)
        var popupExpanded = expandState.popupExpanded
        setSectionExpanded(popupContent, popupArrow, popupExpanded)
        popupHeader.setOnClickListener {
            popupExpanded = !popupExpanded
            setSectionExpanded(popupContent, popupArrow, popupExpanded)
            expandState = expandState.copy(popupExpanded = popupExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val shuffleHeader = view.findViewById<View>(R.id.layout_develop_shuffle_header)
        val shuffleArrow = view.findViewById<TextView>(R.id.tv_develop_shuffle_arrow)
        val shuffleContent = view.findViewById<LinearLayout>(R.id.layout_develop_shuffle_content)
        var shuffleExpanded = expandState.shuffleExpanded
        setSectionExpanded(shuffleContent, shuffleArrow, shuffleExpanded)
        shuffleHeader.setOnClickListener {
            shuffleExpanded = !shuffleExpanded
            setSectionExpanded(shuffleContent, shuffleArrow, shuffleExpanded)
            expandState = expandState.copy(shuffleExpanded = shuffleExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val cardPilesHeader = view.findViewById<View>(R.id.layout_develop_card_piles_header)
        val cardPilesArrow = view.findViewById<TextView>(R.id.tv_develop_card_piles_arrow)
        val cardPilesContent = view.findViewById<LinearLayout>(R.id.layout_develop_card_piles_content)
        var cardPilesExpanded = expandState.cardPilesExpanded
        setSectionExpanded(cardPilesContent, cardPilesArrow, cardPilesExpanded)
        cardPilesHeader.setOnClickListener {
            cardPilesExpanded = !cardPilesExpanded
            setSectionExpanded(cardPilesContent, cardPilesArrow, cardPilesExpanded)
            expandState = expandState.copy(cardPilesExpanded = cardPilesExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val adsRewardsHeader = view.findViewById<View>(R.id.layout_develop_ads_rewards_header)
        val adsRewardsArrow = view.findViewById<TextView>(R.id.tv_develop_ads_rewards_arrow)
        val adsRewardsContent = view.findViewById<LinearLayout>(R.id.layout_develop_ads_rewards_content)
        var adsRewardsExpanded = expandState.adsRewardsExpanded
        setSectionExpanded(adsRewardsContent, adsRewardsArrow, adsRewardsExpanded)
        adsRewardsHeader.setOnClickListener {
            adsRewardsExpanded = !adsRewardsExpanded
            setSectionExpanded(adsRewardsContent, adsRewardsArrow, adsRewardsExpanded)
            expandState = expandState.copy(adsRewardsExpanded = adsRewardsExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val aspectRatioHeader = view.findViewById<View>(R.id.layout_develop_aspect_ratio_header)
        val aspectRatioArrow = view.findViewById<TextView>(R.id.tv_develop_aspect_ratio_arrow)
        val aspectRatioContent = view.findViewById<LinearLayout>(R.id.layout_develop_aspect_ratio_content)
        var aspectRatioExpanded = expandState.aspectRatioExpanded
        setSectionExpanded(aspectRatioContent, aspectRatioArrow, aspectRatioExpanded)
        aspectRatioHeader.setOnClickListener {
            aspectRatioExpanded = !aspectRatioExpanded
            setSectionExpanded(aspectRatioContent, aspectRatioArrow, aspectRatioExpanded)
            expandState = expandState.copy(aspectRatioExpanded = aspectRatioExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        bindStarburstControls(view, host)
        bindPopupControls(view, host)
        bindBottomControlAdjustments(view, host)
        bindRewardHudAdjustments(view, host)
        bindLandscapeBannerBoxChoice(view, host)
        bindAspectRatioControls(view, host)
        refreshAllDisplays(view, host)

        view.findViewById<MaterialButton>(R.id.btn_develop_close).setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { win ->
            val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val widthFraction = if (isLandscape) 0.60f else 0.92f
            val widthPx = (resources.displayMetrics.widthPixels * widthFraction).toInt()
            win.setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    private fun bindStarburstControls(view: View, host: Host) {
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_minus5).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetX(-5); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_minus1).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetX(-1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_plus1).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetX(1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_x_plus5).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetX(5); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_minus5).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetY(-5); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_minus1).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetY(-1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_plus1).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetY(1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_pivot_y_plus5).setOnClickListener { host.onTesterAdjustStarburstPivotOffsetY(5); refreshStarburstDisplays(host) }

        btnStarburstPivotXValue?.setOnClickListener {
            showSetSignedValueDialog(getString(R.string.tester_menu_starburst_pivot_x), host.testerStarburstPivotOffsetX()) {
                host.onTesterSetStarburstPivotOffsetX(it)
                refreshStarburstDisplays(host)
            }
        }
        btnStarburstPivotYValue?.setOnClickListener {
            showSetSignedValueDialog(getString(R.string.tester_menu_starburst_pivot_y), host.testerStarburstPivotOffsetY()) {
                host.onTesterSetStarburstPivotOffsetY(it)
                refreshStarburstDisplays(host)
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_minus10).setOnClickListener { host.onTesterAdjustStarburstPositionX(-10); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_minus1).setOnClickListener { host.onTesterAdjustStarburstPositionX(-1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_plus1).setOnClickListener { host.onTesterAdjustStarburstPositionX(1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_x_plus10).setOnClickListener { host.onTesterAdjustStarburstPositionX(10); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_minus10).setOnClickListener { host.onTesterAdjustStarburstPositionY(-10); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_minus1).setOnClickListener { host.onTesterAdjustStarburstPositionY(-1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_plus1).setOnClickListener { host.onTesterAdjustStarburstPositionY(1); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_position_y_plus10).setOnClickListener { host.onTesterAdjustStarburstPositionY(10); refreshStarburstDisplays(host) }

        btnStarburstPositionXValue?.setOnClickListener {
            showSetSignedValueDialog(getString(R.string.tester_menu_starburst_position_x), host.testerStarburstPositionX()) {
                host.onTesterSetStarburstPositionX(it)
                refreshStarburstDisplays(host)
            }
        }
        btnStarburstPositionYValue?.setOnClickListener {
            showSetSignedValueDialog(getString(R.string.tester_menu_starburst_position_y), host.testerStarburstPositionY()) {
                host.onTesterSetStarburstPositionY(it)
                refreshStarburstDisplays(host)
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_starburst_scale_minus_quarter).setOnClickListener { host.onTesterAdjustStarburstScale(-0.25f); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_scale_minus_small).setOnClickListener { host.onTesterAdjustStarburstScale(-0.05f); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_scale_plus_small).setOnClickListener { host.onTesterAdjustStarburstScale(0.05f); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_scale_plus_quarter).setOnClickListener { host.onTesterAdjustStarburstScale(0.25f); refreshStarburstDisplays(host) }
        btnStarburstScaleValue?.setOnClickListener {
            showSetDecimalValueDialog(getString(R.string.tester_menu_starburst_scale), host.testerStarburstScale()) {
                host.onTesterSetStarburstScale(it)
                refreshStarburstDisplays(host)
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_minus100).setOnClickListener { host.onTesterAdjustStarburstRotationDurationMs(-100); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_minus10).setOnClickListener { host.onTesterAdjustStarburstRotationDurationMs(-10); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_plus10).setOnClickListener { host.onTesterAdjustStarburstRotationDurationMs(10); refreshStarburstDisplays(host) }
        view.findViewById<MaterialButton>(R.id.btn_starburst_rotation_speed_plus100).setOnClickListener { host.onTesterAdjustStarburstRotationDurationMs(100); refreshStarburstDisplays(host) }
        btnStarburstRotationSpeedValue?.setOnClickListener {
            showSetValueDialog(getString(R.string.tester_menu_starburst_rotation_speed), host.testerStarburstRotationDurationMs()) {
                host.onTesterSetStarburstRotationDurationMs(it)
                refreshStarburstDisplays(host)
            }
        }

        btnStarburstRotationToggle?.setOnClickListener {
            host.onTesterSetStarburstRotationEnabled(!host.testerIsStarburstRotationEnabled())
            refreshStarburstDisplays(host)
        }

        view.findViewById<MaterialButton>(R.id.btn_starburst_copy_values).setOnClickListener {
            copyStarburstValuesToClipboard(host)
        }
        view.findViewById<MaterialButton>(R.id.btn_starburst_apply_auto).setOnClickListener {
            host.onTesterApplyAutoStarburstLayout()
            refreshStarburstDisplays(host)
        }
    }

    private fun bindPopupControls(view: View, host: Host) {
        fun bindDecimal(buttonId: Int, labelRes: Int, getter: () -> Float, setter: (Float) -> Unit) {
            view.findViewById<MaterialButton>(buttonId).setOnClickListener {
                showSetDecimalValueDialog(getString(labelRes), getter()) {
                    setter(it)
                    refreshPopupDisplays(view, host)
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btn_dev_popup_apply_auto).setOnClickListener {
            host.onDevApplyAutoWinPopupRatios()
            refreshPopupDisplays(view, host)
        }

        bindDecimal(R.id.btn_dev_gem_image_height, R.string.develop_menu_gem_image_height, host::devGemImageHeightDp, host::onDevSetGemImageHeight)
        bindDecimal(R.id.btn_dev_gem_offset_x, R.string.develop_menu_gem_offset_x, host::devGemOffsetXDp, host::onDevSetGemOffsetX)
        bindDecimal(R.id.btn_dev_gem_offset_y, R.string.develop_menu_gem_offset_y, host::devGemOffsetYDp, host::onDevSetGemOffsetY)
        bindDecimal(R.id.btn_dev_ticket_image_height, R.string.develop_menu_ticket_image_height, host::devTicketImageHeightDp, host::onDevSetTicketImageHeight)
        bindDecimal(R.id.btn_dev_ticket_offset_x, R.string.develop_menu_ticket_offset_x, host::devTicketOffsetXDp, host::onDevSetTicketOffsetX)
        bindDecimal(R.id.btn_dev_ticket_offset_y, R.string.develop_menu_ticket_offset_y, host::devTicketOffsetYDp, host::onDevSetTicketOffsetY)
        bindDecimal(R.id.btn_dev_wand_image_height, R.string.develop_menu_wand_image_height, host::devWandImageHeightDp, host::onDevSetWandImageHeight)
        bindDecimal(R.id.btn_dev_wand_offset_x, R.string.develop_menu_wand_offset_x, host::devWandOffsetXDp, host::onDevSetWandOffsetX)
        bindDecimal(R.id.btn_dev_wand_offset_y, R.string.develop_menu_wand_offset_y, host::devWandOffsetYDp, host::onDevSetWandOffsetY)
        bindDecimal(R.id.btn_dev_reward_text_size, R.string.develop_menu_reward_text_size, host::devRewardTextSizeSp, host::onDevSetRewardTextSize)
        bindDecimal(R.id.btn_dev_gem_number_offset_x, R.string.develop_menu_gem_number_offset_x, host::devGemNumberOffsetXDp, host::onDevSetGemNumberOffsetX)
        bindDecimal(R.id.btn_dev_gem_number_offset_y, R.string.develop_menu_gem_number_offset_y, host::devGemNumberOffsetYDp, host::onDevSetGemNumberOffsetY)
        bindDecimal(R.id.btn_dev_ticket_number_offset_x, R.string.develop_menu_ticket_number_offset_x, host::devTicketNumberOffsetXDp, host::onDevSetTicketNumberOffsetX)
        bindDecimal(R.id.btn_dev_ticket_number_offset_y, R.string.develop_menu_ticket_number_offset_y, host::devTicketNumberOffsetYDp, host::onDevSetTicketNumberOffsetY)
        bindDecimal(R.id.btn_dev_wand_number_offset_x, R.string.develop_menu_wand_number_offset_x, host::devWandNumberOffsetXDp, host::onDevSetWandNumberOffsetX)
        bindDecimal(R.id.btn_dev_wand_number_offset_y, R.string.develop_menu_wand_number_offset_y, host::devWandNumberOffsetYDp, host::onDevSetWandNumberOffsetY)
        bindDecimal(R.id.btn_dev_button_row_offset_x, R.string.develop_menu_button_row_offset_x, host::devButtonRowOffsetXDp, host::onDevSetButtonRowOffsetX)
        bindDecimal(R.id.btn_dev_button_row_offset_y, R.string.develop_menu_button_row_offset_y, host::devButtonRowOffsetYDp, host::onDevSetButtonRowOffsetY)
        bindDecimal(R.id.btn_dev_popup_button0_scale_x, R.string.develop_menu_button0_scale_x, host::devPopupButton0ScaleX, host::onDevSetPopupButton0ScaleX)
        bindDecimal(R.id.btn_dev_popup_button0_scale_y, R.string.develop_menu_button0_scale_y, host::devPopupButton0ScaleY, host::onDevSetPopupButton0ScaleY)
        bindDecimal(R.id.btn_dev_popup_button0_scale, R.string.develop_menu_button0_scale, host::devPopupButton0Scale, host::onDevSetPopupButton0Scale)
        bindDecimal(R.id.btn_dev_popup_button1_scale_x, R.string.develop_menu_button1_scale_x, host::devPopupButton1ScaleX, host::onDevSetPopupButton1ScaleX)
        bindDecimal(R.id.btn_dev_popup_button1_scale_y, R.string.develop_menu_button1_scale_y, host::devPopupButton1ScaleY, host::onDevSetPopupButton1ScaleY)
        bindDecimal(R.id.btn_dev_popup_button1_scale, R.string.develop_menu_button1_scale, host::devPopupButton1Scale, host::onDevSetPopupButton1Scale)
        bindDecimal(R.id.btn_dev_popup_button2_scale_x, R.string.develop_menu_button2_scale_x, host::devPopupButton2ScaleX, host::onDevSetPopupButton2ScaleX)
        bindDecimal(R.id.btn_dev_popup_button2_scale_y, R.string.develop_menu_button2_scale_y, host::devPopupButton2ScaleY, host::onDevSetPopupButton2ScaleY)
        bindDecimal(R.id.btn_dev_popup_button2_scale, R.string.develop_menu_button2_scale, host::devPopupButton2Scale, host::onDevSetPopupButton2Scale)
        bindDecimal(R.id.btn_dev_popup_desc_text_size, R.string.develop_menu_popup_desc_text_size, host::devPopupDescriptionTextSizeSp, host::onDevSetPopupDescriptionTextSize)
        bindDecimal(R.id.btn_dev_popup_desc_offset_x, R.string.develop_menu_popup_desc_offset_x, host::devPopupDescriptionOffsetXDp, host::onDevSetPopupDescriptionOffsetX)
        bindDecimal(R.id.btn_dev_popup_desc_offset_y, R.string.develop_menu_popup_desc_offset_y, host::devPopupDescriptionOffsetYDp, host::onDevSetPopupDescriptionOffsetY)
        bindDecimal(R.id.btn_dev_victory_text_size, R.string.develop_menu_victory_text_size, host::devVictoryTextSizeSp, host::onDevSetVictoryTextSize)
        bindDecimal(R.id.btn_dev_victory_offset_x, R.string.develop_menu_victory_offset_x, host::devVictoryOffsetXDp, host::onDevSetVictoryOffsetX)
        bindDecimal(R.id.btn_dev_victory_offset_y, R.string.develop_menu_victory_offset_y, host::devVictoryOffsetYDp, host::onDevSetVictoryOffsetY)
        bindDecimal(R.id.btn_dev_shuffle_second_clip_delay_ms, R.string.develop_menu_shuffle_second_clip_delay_ms, host::devShuffleSecondClipDelayMs, host::onDevSetShuffleSecondClipDelayMs)
        bindDecimal(R.id.btn_dev_shuffle_tail_delay_ms, R.string.develop_menu_shuffle_tail_delay_ms, host::devShuffleTailDelayMs, host::onDevSetShuffleTailDelayMs)
        bindDecimal(R.id.btn_dev_deal_card_interval_ms, R.string.develop_menu_deal_card_interval_ms, host::devDealCardIntervalMs, host::onDevSetDealCardIntervalMs)

        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_x_portrait, R.string.develop_menu_locked_pile_ad_offset_x_portrait, host::devLockedPileAdOffsetXPortraitPx, host::onDevSetLockedPileAdOffsetXPortraitPx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_y_portrait, R.string.develop_menu_locked_pile_ad_offset_y_portrait, host::devLockedPileAdOffsetYPortraitPx, host::onDevSetLockedPileAdOffsetYPortraitPx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_x_portrait, R.string.develop_menu_locked_pile_ad_scale_x_portrait, host::devLockedPileAdScaleXPortrait, host::onDevSetLockedPileAdScaleXPortrait)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_y_portrait, R.string.develop_menu_locked_pile_ad_scale_y_portrait, host::devLockedPileAdScaleYPortrait, host::onDevSetLockedPileAdScaleYPortrait)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_x_landscape, R.string.develop_menu_locked_pile_ad_offset_x_landscape, host::devLockedPileAdOffsetXLandscapePx, host::onDevSetLockedPileAdOffsetXLandscapePx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_y_landscape, R.string.develop_menu_locked_pile_ad_offset_y_landscape, host::devLockedPileAdOffsetYLandscapePx, host::onDevSetLockedPileAdOffsetYLandscapePx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_x_landscape, R.string.develop_menu_locked_pile_ad_scale_x_landscape, host::devLockedPileAdScaleXLandscape, host::onDevSetLockedPileAdScaleXLandscape)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_y_landscape, R.string.develop_menu_locked_pile_ad_scale_y_landscape, host::devLockedPileAdScaleYLandscape, host::onDevSetLockedPileAdScaleYLandscape)
        bindDecimal(R.id.btn_dev_landscape_pile_overall_offset_x, R.string.develop_menu_landscape_pile_overall_offset_x, host::devLandscapePileOverallOffsetXDp, host::onDevSetLandscapePileOverallOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_overall_offset_y, R.string.develop_menu_landscape_pile_overall_offset_y, host::devLandscapePileOverallOffsetYDp, host::onDevSetLandscapePileOverallOffsetY)
        bindDecimal(R.id.btn_dev_landscape_pile_foundation_offset_x, R.string.develop_menu_landscape_pile_foundation_offset_x, host::devLandscapePileFoundationOffsetXDp, host::onDevSetLandscapePileFoundationOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_foundation_offset_y, R.string.develop_menu_landscape_pile_foundation_offset_y, host::devLandscapePileFoundationOffsetYDp, host::onDevSetLandscapePileFoundationOffsetY)
        bindDecimal(R.id.btn_dev_landscape_pile_drawwaste_offset_x, R.string.develop_menu_landscape_pile_drawwaste_offset_x, host::devLandscapePileDrawWasteOffsetXDp, host::onDevSetLandscapePileDrawWasteOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_drawwaste_offset_y, R.string.develop_menu_landscape_pile_drawwaste_offset_y, host::devLandscapePileDrawWasteOffsetYDp, host::onDevSetLandscapePileDrawWasteOffsetY)
        bindDecimal(R.id.btn_dev_landscape_pile_stock_offset_x, R.string.develop_menu_landscape_pile_stock_offset_x, host::devLandscapePileStockOffsetXDp, host::onDevSetLandscapePileStockOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_stock_offset_y, R.string.develop_menu_landscape_pile_stock_offset_y, host::devLandscapePileStockOffsetYDp, host::onDevSetLandscapePileStockOffsetY)
        bindDecimal(R.id.btn_dev_landscape_pile_waste_offset_x, R.string.develop_menu_landscape_pile_waste_offset_x, host::devLandscapePileWasteOffsetXDp, host::onDevSetLandscapePileWasteOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_waste_offset_y, R.string.develop_menu_landscape_pile_waste_offset_y, host::devLandscapePileWasteOffsetYDp, host::onDevSetLandscapePileWasteOffsetY)
        bindDecimal(R.id.btn_dev_landscape_pile_tableau_offset_x, R.string.develop_menu_landscape_pile_tableau_offset_x, host::devLandscapePileTableauOffsetXDp, host::onDevSetLandscapePileTableauOffsetX)
        bindDecimal(R.id.btn_dev_landscape_pile_tableau_offset_y, R.string.develop_menu_landscape_pile_tableau_offset_y, host::devLandscapePileTableauOffsetYDp, host::onDevSetLandscapePileTableauOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_overall_offset_x, R.string.develop_menu_portrait_pile_overall_offset_x, host::devPortraitPileOverallOffsetXDp, host::onDevSetPortraitPileOverallOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_overall_offset_y, R.string.develop_menu_portrait_pile_overall_offset_y, host::devPortraitPileOverallOffsetYDp, host::onDevSetPortraitPileOverallOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_foundation_offset_x, R.string.develop_menu_portrait_pile_foundation_offset_x, host::devPortraitPileFoundationOffsetXDp, host::onDevSetPortraitPileFoundationOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_foundation_offset_y, R.string.develop_menu_portrait_pile_foundation_offset_y, host::devPortraitPileFoundationOffsetYDp, host::onDevSetPortraitPileFoundationOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_drawwaste_offset_x, R.string.develop_menu_portrait_pile_drawwaste_offset_x, host::devPortraitPileDrawWasteOffsetXDp, host::onDevSetPortraitPileDrawWasteOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_drawwaste_offset_y, R.string.develop_menu_portrait_pile_drawwaste_offset_y, host::devPortraitPileDrawWasteOffsetYDp, host::onDevSetPortraitPileDrawWasteOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_stock_offset_x, R.string.develop_menu_portrait_pile_stock_offset_x, host::devPortraitPileStockOffsetXDp, host::onDevSetPortraitPileStockOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_stock_offset_y, R.string.develop_menu_portrait_pile_stock_offset_y, host::devPortraitPileStockOffsetYDp, host::onDevSetPortraitPileStockOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_waste_offset_x, R.string.develop_menu_portrait_pile_waste_offset_x, host::devPortraitPileWasteOffsetXDp, host::onDevSetPortraitPileWasteOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_waste_offset_y, R.string.develop_menu_portrait_pile_waste_offset_y, host::devPortraitPileWasteOffsetYDp, host::onDevSetPortraitPileWasteOffsetY)
        bindDecimal(R.id.btn_dev_portrait_pile_tableau_offset_x, R.string.develop_menu_portrait_pile_tableau_offset_x, host::devPortraitPileTableauOffsetXDp, host::onDevSetPortraitPileTableauOffsetX)
        bindDecimal(R.id.btn_dev_portrait_pile_tableau_offset_y, R.string.develop_menu_portrait_pile_tableau_offset_y, host::devPortraitPileTableauOffsetYDp, host::onDevSetPortraitPileTableauOffsetY)
        bindDecimal(R.id.btn_dev_landscape_banner_small_width, R.string.develop_menu_landscape_banner_small_width, host::devLandscapeBannerSmallWidthDp, host::onDevSetLandscapeBannerSmallWidthDp)
        bindDecimal(R.id.btn_dev_landscape_banner_small_height, R.string.develop_menu_landscape_banner_small_height, host::devLandscapeBannerSmallHeightDp, host::onDevSetLandscapeBannerSmallHeightDp)
        bindDecimal(R.id.btn_dev_landscape_banner_medium_width, R.string.develop_menu_landscape_banner_medium_width, host::devLandscapeBannerMediumWidthDp, host::onDevSetLandscapeBannerMediumWidthDp)
        bindDecimal(R.id.btn_dev_landscape_banner_medium_height, R.string.develop_menu_landscape_banner_medium_height, host::devLandscapeBannerMediumHeightDp, host::onDevSetLandscapeBannerMediumHeightDp)
        bindDecimal(R.id.btn_dev_landscape_banner_large_width, R.string.develop_menu_landscape_banner_large_width, host::devLandscapeBannerLargeWidthDp, host::onDevSetLandscapeBannerLargeWidthDp)
        bindDecimal(R.id.btn_dev_landscape_banner_large_height, R.string.develop_menu_landscape_banner_large_height, host::devLandscapeBannerLargeHeightDp, host::onDevSetLandscapeBannerLargeHeightDp)
        bindDecimal(R.id.btn_dev_landscape_banner_small_offset_x, R.string.develop_menu_landscape_banner_small_offset_x, host::devSmallDeviceLandscapeBannerOffsetXDp, host::onDevSetSmallDeviceLandscapeBannerOffsetX)
        bindDecimal(R.id.btn_dev_landscape_banner_small_offset_y, R.string.develop_menu_landscape_banner_small_offset_y, host::devSmallDeviceLandscapeBannerOffsetYDp, host::onDevSetSmallDeviceLandscapeBannerOffsetY)
        bindDecimal(R.id.btn_dev_landscape_banner_medium_offset_x, R.string.develop_menu_landscape_banner_medium_offset_x, host::devMediumDeviceLandscapeBannerOffsetXDp, host::onDevSetMediumDeviceLandscapeBannerOffsetX)
        bindDecimal(R.id.btn_dev_landscape_banner_medium_offset_y, R.string.develop_menu_landscape_banner_medium_offset_y, host::devMediumDeviceLandscapeBannerOffsetYDp, host::onDevSetMediumDeviceLandscapeBannerOffsetY)
        bindDecimal(R.id.btn_dev_landscape_banner_large_offset_x, R.string.develop_menu_landscape_banner_large_offset_x, host::devLargeDeviceLandscapeBannerOffsetXDp, host::onDevSetLargeDeviceLandscapeBannerOffsetX)
        bindDecimal(R.id.btn_dev_landscape_banner_large_offset_y, R.string.develop_menu_landscape_banner_large_offset_y, host::devLargeDeviceLandscapeBannerOffsetYDp, host::onDevSetLargeDeviceLandscapeBannerOffsetY)
        bindDecimal(R.id.btn_dev_scoreboard_offset_x, R.string.develop_menu_scoreboard_offset_x, host::devScoreboardOffsetXDp, host::onDevSetScoreboardOffsetX)
        bindDecimal(R.id.btn_dev_scoreboard_offset_y, R.string.develop_menu_scoreboard_offset_y, host::devScoreboardOffsetYDp, host::onDevSetScoreboardOffsetY)
        bindDecimal(R.id.btn_dev_gem_reward_offset_x, R.string.develop_menu_gem_reward_offset_x, host::devGemRewardOffsetXDp, host::onDevSetGemRewardOffsetX)
        bindDecimal(R.id.btn_dev_gem_reward_offset_y, R.string.develop_menu_gem_reward_offset_y, host::devGemRewardOffsetYDp, host::onDevSetGemRewardOffsetY)
        bindDecimal(R.id.btn_dev_ticket_reward_offset_x, R.string.develop_menu_ticket_reward_offset_x, host::devTicketRewardOffsetXDp, host::onDevSetTicketRewardOffsetX)
        bindDecimal(R.id.btn_dev_ticket_reward_offset_y, R.string.develop_menu_ticket_reward_offset_y, host::devTicketRewardOffsetYDp, host::onDevSetTicketRewardOffsetY)
    }

    private fun bindBottomControlAdjustments(view: View, host: Host) {
        fun bind(btnId: Int, labelRes: Int, getter: () -> Float, setter: (Float) -> Unit) {
            view.findViewById<MaterialButton>(btnId).setOnClickListener {
                showSetDecimalValueDialog(getString(labelRes), getter()) {
                    setter(it)
                    refreshBottomControlDisplays(view, host)
                }
            }
        }
        bind(R.id.btn_dev_undo_control_scale, R.string.develop_menu_bottom_control_undo_scale, host::devUndoControlScale, host::onDevSetUndoControlScale)
        bind(R.id.btn_dev_undo_control_offset_x, R.string.develop_menu_bottom_control_undo_offset_x, host::devUndoControlOffsetXDp, host::onDevSetUndoControlOffsetX)
        bind(R.id.btn_dev_undo_control_offset_y, R.string.develop_menu_bottom_control_undo_offset_y, host::devUndoControlOffsetYDp, host::onDevSetUndoControlOffsetY)
        bind(R.id.btn_dev_redo_control_scale, R.string.develop_menu_bottom_control_redo_scale, host::devRedoControlScale, host::onDevSetRedoControlScale)
        bind(R.id.btn_dev_redo_control_offset_x, R.string.develop_menu_bottom_control_redo_offset_x, host::devRedoControlOffsetXDp, host::onDevSetRedoControlOffsetX)
        bind(R.id.btn_dev_redo_control_offset_y, R.string.develop_menu_bottom_control_redo_offset_y, host::devRedoControlOffsetYDp, host::onDevSetRedoControlOffsetY)
        bind(R.id.btn_dev_hint_control_scale, R.string.develop_menu_bottom_control_hint_scale, host::devHintControlScale, host::onDevSetHintControlScale)
        bind(R.id.btn_dev_hint_control_offset_x, R.string.develop_menu_bottom_control_hint_offset_x, host::devHintControlOffsetXDp, host::onDevSetHintControlOffsetX)
        bind(R.id.btn_dev_hint_control_offset_y, R.string.develop_menu_bottom_control_hint_offset_y, host::devHintControlOffsetYDp, host::onDevSetHintControlOffsetY)
        bind(R.id.btn_dev_magic_wand_control_scale, R.string.develop_menu_bottom_control_magic_wand_scale, host::devMagicWandControlScale, host::onDevSetMagicWandControlScale)
        bind(R.id.btn_dev_magic_wand_control_offset_x, R.string.develop_menu_bottom_control_magic_wand_offset_x, host::devMagicWandControlOffsetXDp, host::onDevSetMagicWandControlOffsetX)
        bind(R.id.btn_dev_magic_wand_control_offset_y, R.string.develop_menu_bottom_control_magic_wand_offset_y, host::devMagicWandControlOffsetYDp, host::onDevSetMagicWandControlOffsetY)
        bind(R.id.btn_dev_play_control_scale, R.string.develop_menu_bottom_control_play_scale, host::devPlayControlScale, host::onDevSetPlayControlScale)
        bind(R.id.btn_dev_play_control_offset_x, R.string.develop_menu_bottom_control_play_offset_x, host::devPlayControlOffsetXDp, host::onDevSetPlayControlOffsetX)
        bind(R.id.btn_dev_play_control_offset_y, R.string.develop_menu_bottom_control_play_offset_y, host::devPlayControlOffsetYDp, host::onDevSetPlayControlOffsetY)
        bind(R.id.btn_dev_auto_control_scale, R.string.develop_menu_bottom_control_auto_scale, host::devAutoControlScale, host::onDevSetAutoControlScale)
        bind(R.id.btn_dev_auto_control_offset_x, R.string.develop_menu_bottom_control_auto_offset_x, host::devAutoControlOffsetXDp, host::onDevSetAutoControlOffsetX)
        bind(R.id.btn_dev_auto_control_offset_y, R.string.develop_menu_bottom_control_auto_offset_y, host::devAutoControlOffsetYDp, host::onDevSetAutoControlOffsetY)
    }

    private fun bindRewardHudAdjustments(view: View, host: Host) {
        fun bind(btnId: Int, labelRes: Int, getter: () -> Float, setter: (Float) -> Unit) {
            view.findViewById<MaterialButton>(btnId).setOnClickListener {
                showSetDecimalValueDialog(getString(labelRes), getter()) {
                    setter(it)
                    refreshRewardHudDisplays(view, host)
                }
            }
        }
        bind(R.id.btn_dev_gem_reward_scale, R.string.develop_menu_gem_reward_scale, host::devGemRewardScale, host::onDevSetGemRewardScale)
        bind(R.id.btn_dev_gem_reward_counter_offset_x, R.string.develop_menu_gem_reward_counter_offset_x, host::devGemRewardCounterOffsetXDp, host::onDevSetGemRewardCounterOffsetX)
        bind(R.id.btn_dev_gem_reward_counter_offset_y, R.string.develop_menu_gem_reward_counter_offset_y, host::devGemRewardCounterOffsetYDp, host::onDevSetGemRewardCounterOffsetY)
        bind(R.id.btn_dev_gem_reward_counter_scale, R.string.develop_menu_gem_reward_counter_scale, host::devGemRewardCounterScale, host::onDevSetGemRewardCounterScale)
        bind(R.id.btn_dev_ticket_reward_scale, R.string.develop_menu_ticket_reward_scale, host::devTicketRewardScale, host::onDevSetTicketRewardScale)
        bind(R.id.btn_dev_ticket_reward_counter_offset_x, R.string.develop_menu_ticket_reward_counter_offset_x, host::devTicketRewardCounterOffsetXDp, host::onDevSetTicketRewardCounterOffsetX)
        bind(R.id.btn_dev_ticket_reward_counter_offset_y, R.string.develop_menu_ticket_reward_counter_offset_y, host::devTicketRewardCounterOffsetYDp, host::onDevSetTicketRewardCounterOffsetY)
        bind(R.id.btn_dev_ticket_reward_counter_scale, R.string.develop_menu_ticket_reward_counter_scale, host::devTicketRewardCounterScale, host::onDevSetTicketRewardCounterScale)
    }

    private fun bindLandscapeBannerBoxChoice(view: View, host: Host) {
        view.findViewById<MaterialButton>(R.id.btn_dev_banner_box_choice).setOnClickListener {
            host.onDevCycleBannerBoxChoice()
            refreshLandscapeBannerBoxChoice(view, host)
        }
    }

    private fun bindAspectRatioControls(view: View, host: Host) {
        fun bindDec(btnId: Int, labelRes: Int, getter: () -> Float, setter: (Float) -> Unit) {
            view.findViewById<MaterialButton>(btnId).setOnClickListener {
                showSetDecimalValueDialog(getString(labelRes), getter()) {
                    setter(it)
                    refreshAspectRatioDisplays(view, host)
                }
            }
        }
        bindDec(R.id.btn_dev_aspect_portrait_slim_x,    R.string.develop_menu_aspect_portrait_slim_x,    host::devAspectPortraitSlimXDp,    host::onDevSetAspectPortraitSlimX)
        bindDec(R.id.btn_dev_aspect_portrait_classic_x,  R.string.develop_menu_aspect_portrait_classic_x,  host::devAspectPortraitClassicXDp,  host::onDevSetAspectPortraitClassicX)
        bindDec(R.id.btn_dev_aspect_portrait_broad_x,    R.string.develop_menu_aspect_portrait_broad_x,    host::devAspectPortraitBroadXDp,    host::onDevSetAspectPortraitBroadX)
        bindDec(R.id.btn_dev_aspect_portrait_square_x,   R.string.develop_menu_aspect_portrait_square_x,   host::devAspectPortraitSquareXDp,   host::onDevSetAspectPortraitSquareX)
        bindDec(R.id.btn_dev_aspect_portrait_slim_y,    R.string.develop_menu_aspect_portrait_slim_y,    host::devAspectPortraitSlimYDp,    host::onDevSetAspectPortraitSlimY)
        bindDec(R.id.btn_dev_aspect_portrait_classic_y,  R.string.develop_menu_aspect_portrait_classic_y,  host::devAspectPortraitClassicYDp,  host::onDevSetAspectPortraitClassicY)
        bindDec(R.id.btn_dev_aspect_portrait_broad_y,    R.string.develop_menu_aspect_portrait_broad_y,    host::devAspectPortraitBroadYDp,    host::onDevSetAspectPortraitBroadY)
        bindDec(R.id.btn_dev_aspect_portrait_square_y,   R.string.develop_menu_aspect_portrait_square_y,   host::devAspectPortraitSquareYDp,   host::onDevSetAspectPortraitSquareY)
        bindDec(R.id.btn_dev_aspect_landscape_slim_x,    R.string.develop_menu_aspect_landscape_slim_x,    host::devAspectLandscapeSlimXDp,    host::onDevSetAspectLandscapeSlimX)
        bindDec(R.id.btn_dev_aspect_landscape_classic_x, R.string.develop_menu_aspect_landscape_classic_x, host::devAspectLandscapeClassicXDp, host::onDevSetAspectLandscapeClassicX)
        bindDec(R.id.btn_dev_aspect_landscape_broad_x,   R.string.develop_menu_aspect_landscape_broad_x,   host::devAspectLandscapeBroadXDp,   host::onDevSetAspectLandscapeBroadX)
        bindDec(R.id.btn_dev_aspect_landscape_square_x,  R.string.develop_menu_aspect_landscape_square_x,  host::devAspectLandscapeSquareXDp,  host::onDevSetAspectLandscapeSquareX)
        bindDec(R.id.btn_dev_aspect_landscape_slim_y,    R.string.develop_menu_aspect_landscape_slim_y,    host::devAspectLandscapeSlimYDp,    host::onDevSetAspectLandscapeSlimY)
        bindDec(R.id.btn_dev_aspect_landscape_classic_y, R.string.develop_menu_aspect_landscape_classic_y, host::devAspectLandscapeClassicYDp, host::onDevSetAspectLandscapeClassicY)
        bindDec(R.id.btn_dev_aspect_landscape_broad_y,   R.string.develop_menu_aspect_landscape_broad_y,   host::devAspectLandscapeBroadYDp,   host::onDevSetAspectLandscapeBroadY)
        bindDec(R.id.btn_dev_aspect_landscape_square_y,  R.string.develop_menu_aspect_landscape_square_y,  host::devAspectLandscapeSquareYDp,  host::onDevSetAspectLandscapeSquareY)
    }

    private fun refreshAspectRatioDisplays(root: View, host: Host) {
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_slim_x).text    = fmt(host.devAspectPortraitSlimXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_classic_x).text = fmt(host.devAspectPortraitClassicXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_broad_x).text   = fmt(host.devAspectPortraitBroadXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_square_x).text  = fmt(host.devAspectPortraitSquareXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_slim_y).text    = fmt(host.devAspectPortraitSlimYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_classic_y).text = fmt(host.devAspectPortraitClassicYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_broad_y).text   = fmt(host.devAspectPortraitBroadYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_portrait_square_y).text  = fmt(host.devAspectPortraitSquareYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_slim_x).text   = fmt(host.devAspectLandscapeSlimXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_classic_x).text= fmt(host.devAspectLandscapeClassicXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_broad_x).text  = fmt(host.devAspectLandscapeBroadXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_square_x).text = fmt(host.devAspectLandscapeSquareXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_slim_y).text   = fmt(host.devAspectLandscapeSlimYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_classic_y).text= fmt(host.devAspectLandscapeClassicYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_broad_y).text  = fmt(host.devAspectLandscapeBroadYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_aspect_landscape_square_y).text = fmt(host.devAspectLandscapeSquareYDp())
        root.findViewById<TextView>(R.id.tv_dev_aspect_current_category).text =
            getString(R.string.develop_menu_aspect_current_device_format, host.devCurrentAspectCategoryLabel())
    }

    private fun refreshAllDisplays(root: View, host: Host) {
        refreshStarburstDisplays(host)
        refreshPopupDisplays(root, host)
        refreshBottomControlDisplays(root, host)
        refreshRewardHudDisplays(root, host)
        refreshLandscapeBannerBoxChoice(root, host)
        refreshAspectRatioDisplays(root, host)
    }

    private fun refreshStarburstDisplays(host: Host) {
        btnStarburstPivotXValue?.text = host.testerStarburstPivotOffsetX().toString()
        btnStarburstPivotYValue?.text = host.testerStarburstPivotOffsetY().toString()
        btnStarburstPositionXValue?.text = host.testerStarburstPositionX().toString()
        btnStarburstPositionYValue?.text = host.testerStarburstPositionY().toString()
        btnStarburstScaleValue?.text = String.format(Locale.US, "%.2fx", host.testerStarburstScale())
        btnStarburstRotationSpeedValue?.text = host.testerStarburstRotationDurationMs().toString()
        val enabled = host.testerIsStarburstRotationEnabled()
        tvStarburstRotationStatus?.text = if (enabled) getString(R.string.tester_menu_starburst_rotation_running) else getString(R.string.tester_menu_starburst_rotation_stopped)
        btnStarburstRotationToggle?.text = if (enabled) getString(R.string.tester_menu_starburst_rotation_stop) else getString(R.string.tester_menu_starburst_rotation_start)
    }

    private fun refreshPopupDisplays(root: View, host: Host) {
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_image_height).text = fmt(host.devGemImageHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_offset_x).text = fmt(host.devGemOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_offset_y).text = fmt(host.devGemOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_image_height).text = fmt(host.devTicketImageHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_offset_x).text = fmt(host.devTicketOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_offset_y).text = fmt(host.devTicketOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_wand_image_height).text = fmt(host.devWandImageHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_wand_offset_x).text = fmt(host.devWandOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_wand_offset_y).text = fmt(host.devWandOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_reward_text_size).text = fmt(host.devRewardTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_number_offset_x).text = fmt(host.devGemNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_number_offset_y).text = fmt(host.devGemNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_number_offset_x).text = fmt(host.devTicketNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_number_offset_y).text = fmt(host.devTicketNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_wand_number_offset_x).text = fmt(host.devWandNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_wand_number_offset_y).text = fmt(host.devWandNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_button_row_offset_x).text = fmt(host.devButtonRowOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_button_row_offset_y).text = fmt(host.devButtonRowOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button0_scale_x).text = fmt(host.devPopupButton0ScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button0_scale_y).text = fmt(host.devPopupButton0ScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button0_scale).text = fmt(host.devPopupButton0Scale())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button1_scale_x).text = fmt(host.devPopupButton1ScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button1_scale_y).text = fmt(host.devPopupButton1ScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button1_scale).text = fmt(host.devPopupButton1Scale())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button2_scale_x).text = fmt(host.devPopupButton2ScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button2_scale_y).text = fmt(host.devPopupButton2ScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_button2_scale).text = fmt(host.devPopupButton2Scale())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_desc_text_size).text = fmt(host.devPopupDescriptionTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_desc_offset_x).text = fmt(host.devPopupDescriptionOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_popup_desc_offset_y).text = fmt(host.devPopupDescriptionOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_text_size).text = fmt(host.devVictoryTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_offset_x).text = fmt(host.devVictoryOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_offset_y).text = fmt(host.devVictoryOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_shuffle_second_clip_delay_ms).text = fmt(host.devShuffleSecondClipDelayMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_shuffle_tail_delay_ms).text = fmt(host.devShuffleTailDelayMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_deal_card_interval_ms).text = fmt(host.devDealCardIntervalMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_x_portrait).text = fmt(host.devLockedPileAdOffsetXPortraitPx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_y_portrait).text = fmt(host.devLockedPileAdOffsetYPortraitPx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_x_portrait).text = fmt(host.devLockedPileAdScaleXPortrait())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_y_portrait).text = fmt(host.devLockedPileAdScaleYPortrait())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_x_landscape).text = fmt(host.devLockedPileAdOffsetXLandscapePx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_y_landscape).text = fmt(host.devLockedPileAdOffsetYLandscapePx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_x_landscape).text = fmt(host.devLockedPileAdScaleXLandscape())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_y_landscape).text = fmt(host.devLockedPileAdScaleYLandscape())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_overall_offset_x).text = fmt(host.devLandscapePileOverallOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_overall_offset_y).text = fmt(host.devLandscapePileOverallOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_foundation_offset_x).text = fmt(host.devLandscapePileFoundationOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_foundation_offset_y).text = fmt(host.devLandscapePileFoundationOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_drawwaste_offset_x).text = fmt(host.devLandscapePileDrawWasteOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_drawwaste_offset_y).text = fmt(host.devLandscapePileDrawWasteOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_stock_offset_x).text = fmt(host.devLandscapePileStockOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_stock_offset_y).text = fmt(host.devLandscapePileStockOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_waste_offset_x).text = fmt(host.devLandscapePileWasteOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_waste_offset_y).text = fmt(host.devLandscapePileWasteOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_tableau_offset_x).text = fmt(host.devLandscapePileTableauOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_pile_tableau_offset_y).text = fmt(host.devLandscapePileTableauOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_overall_offset_x).text = fmt(host.devPortraitPileOverallOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_overall_offset_y).text = fmt(host.devPortraitPileOverallOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_foundation_offset_x).text = fmt(host.devPortraitPileFoundationOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_foundation_offset_y).text = fmt(host.devPortraitPileFoundationOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_drawwaste_offset_x).text = fmt(host.devPortraitPileDrawWasteOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_drawwaste_offset_y).text = fmt(host.devPortraitPileDrawWasteOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_stock_offset_x).text = fmt(host.devPortraitPileStockOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_stock_offset_y).text = fmt(host.devPortraitPileStockOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_waste_offset_x).text = fmt(host.devPortraitPileWasteOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_waste_offset_y).text = fmt(host.devPortraitPileWasteOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_tableau_offset_x).text = fmt(host.devPortraitPileTableauOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_portrait_pile_tableau_offset_y).text = fmt(host.devPortraitPileTableauOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_small_width).text = fmt(host.devLandscapeBannerSmallWidthDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_small_height).text = fmt(host.devLandscapeBannerSmallHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_medium_width).text = fmt(host.devLandscapeBannerMediumWidthDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_medium_height).text = fmt(host.devLandscapeBannerMediumHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_large_width).text = fmt(host.devLandscapeBannerLargeWidthDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_large_height).text = fmt(host.devLandscapeBannerLargeHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_small_offset_x).text = fmt(host.devSmallDeviceLandscapeBannerOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_small_offset_y).text = fmt(host.devSmallDeviceLandscapeBannerOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_medium_offset_x).text = fmt(host.devMediumDeviceLandscapeBannerOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_medium_offset_y).text = fmt(host.devMediumDeviceLandscapeBannerOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_large_offset_x).text = fmt(host.devLargeDeviceLandscapeBannerOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_landscape_banner_large_offset_y).text = fmt(host.devLargeDeviceLandscapeBannerOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_scoreboard_offset_x).text = fmt(host.devScoreboardOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_scoreboard_offset_y).text = fmt(host.devScoreboardOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_offset_x).text = fmt(host.devGemRewardOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_offset_y).text = fmt(host.devGemRewardOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_offset_x).text = fmt(host.devTicketRewardOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_offset_y).text = fmt(host.devTicketRewardOffsetYDp())
    }

    private fun refreshBottomControlDisplays(root: View, host: Host) {
        root.findViewById<MaterialButton>(R.id.btn_dev_undo_control_scale).text = fmt(host.devUndoControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_undo_control_offset_x).text = fmt(host.devUndoControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_undo_control_offset_y).text = fmt(host.devUndoControlOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_redo_control_scale).text = fmt(host.devRedoControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_redo_control_offset_x).text = fmt(host.devRedoControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_redo_control_offset_y).text = fmt(host.devRedoControlOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_hint_control_scale).text = fmt(host.devHintControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_hint_control_offset_x).text = fmt(host.devHintControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_hint_control_offset_y).text = fmt(host.devHintControlOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_magic_wand_control_scale).text = fmt(host.devMagicWandControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_magic_wand_control_offset_x).text = fmt(host.devMagicWandControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_magic_wand_control_offset_y).text = fmt(host.devMagicWandControlOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_play_control_scale).text = fmt(host.devPlayControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_play_control_offset_x).text = fmt(host.devPlayControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_play_control_offset_y).text = fmt(host.devPlayControlOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_auto_control_scale).text = fmt(host.devAutoControlScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_auto_control_offset_x).text = fmt(host.devAutoControlOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_auto_control_offset_y).text = fmt(host.devAutoControlOffsetYDp())
    }

    private fun refreshRewardHudDisplays(root: View, host: Host) {
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_scale).text = fmt(host.devGemRewardScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_counter_offset_x).text = fmt(host.devGemRewardCounterOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_counter_offset_y).text = fmt(host.devGemRewardCounterOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_reward_counter_scale).text = fmt(host.devGemRewardCounterScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_scale).text = fmt(host.devTicketRewardScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_counter_offset_x).text = fmt(host.devTicketRewardCounterOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_counter_offset_y).text = fmt(host.devTicketRewardCounterOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_reward_counter_scale).text = fmt(host.devTicketRewardCounterScale())
    }

    private fun refreshLandscapeBannerBoxChoice(root: View, host: Host) {
        root.findViewById<MaterialButton>(R.id.btn_dev_banner_box_choice).text = host.devLandscapeBannerAdBoxChoiceLabel()
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)

    private fun setSectionExpanded(content: View, arrow: TextView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        arrow.text = if (expanded) "▴" else "▾"
    }

    private fun readExpandStateFromArgs(): ExpandState {
        val b = arguments ?: return ExpandState()
        return ExpandState(
            starburstExpanded = b.getBoolean(ARG_STARBURST_EXPANDED, false),
            popupExpanded = b.getBoolean(ARG_POPUP_EXPANDED, false),
            cardPilesExpanded = b.getBoolean(ARG_CARD_PILES_EXPANDED, false),
            adsRewardsExpanded = b.getBoolean(ARG_ADS_REWARDS_EXPANDED, false),
            shuffleExpanded = b.getBoolean(ARG_SHUFFLE_EXPANDED, false),
            aspectRatioExpanded = b.getBoolean(ARG_ASPECT_RATIO_EXPANDED, false)
        )
    }

    private fun showSetValueDialog(label: String, current: Int, onValueSet: (Int) -> Unit) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.tester_menu_set_value_hint)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text.length)
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.tester_menu_set_value_title, label))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onValueSet(editText.text.toString().trim().toIntOrNull() ?: current)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSetSignedValueDialog(label: String, current: Int, onValueSet: (Int) -> Unit) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.tester_menu_set_value_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(current.toString())
            setSelection(text.length)
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.tester_menu_set_value_title, label))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onValueSet(editText.text.toString().trim().toIntOrNull() ?: current)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSetDecimalValueDialog(label: String, current: Float, onValueSet: (Float) -> Unit) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.tester_menu_set_value_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(String.format(Locale.US, "%.2f", current))
            setSelection(text.length)
            setPadding(40, 24, 40, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.tester_menu_set_value_title, label))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onValueSet(editText.text.toString().trim().toFloatOrNull() ?: current)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyStarburstValuesToClipboard(host: Host) {
        val values = buildString {
            appendLine("// Starburst tuning values")
            appendLine("positionX = ${host.testerStarburstPositionX()}")
            appendLine("positionY = ${host.testerStarburstPositionY()}")
            appendLine("scale = ${String.format(Locale.US, "%.2f", host.testerStarburstScale())}")
            appendLine("pivotOffsetX = ${host.testerStarburstPivotOffsetX()}")
            appendLine("pivotOffsetY = ${host.testerStarburstPivotOffsetY()}")
            appendLine("rotationDurationMs = ${host.testerStarburstRotationDurationMs()}")
            append("rotationEnabled = ${host.testerIsStarburstRotationEnabled()}")
        }
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Starburst Values", values))
        Toast.makeText(requireContext(), R.string.tester_menu_starburst_values_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "develop_menu_dialog"
        private const val ARG_STARBURST_EXPANDED = "arg_starburst_expanded"
        private const val ARG_POPUP_EXPANDED = "arg_popup_expanded"
        private const val ARG_CARD_PILES_EXPANDED = "arg_card_piles_expanded"
        private const val ARG_ADS_REWARDS_EXPANDED = "arg_ads_rewards_expanded"
        private const val ARG_SHUFFLE_EXPANDED = "arg_shuffle_expanded"
        private const val ARG_ASPECT_RATIO_EXPANDED = "arg_aspect_ratio_expanded"

        fun newInstance(state: ExpandState = ExpandState()): DevelopMenuDialogFragment {
            return DevelopMenuDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STARBURST_EXPANDED, state.starburstExpanded)
                    putBoolean(ARG_POPUP_EXPANDED, state.popupExpanded)
                    putBoolean(ARG_CARD_PILES_EXPANDED, state.cardPilesExpanded)
                    putBoolean(ARG_ADS_REWARDS_EXPANDED, state.adsRewardsExpanded)
                    putBoolean(ARG_SHUFFLE_EXPANDED, state.shuffleExpanded)
                    putBoolean(ARG_ASPECT_RATIO_EXPANDED, state.aspectRatioExpanded)
                }
            }
        }
    }
}
