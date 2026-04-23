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
        val tableauExpanded: Boolean = false,
        val dailyPopupExpanded: Boolean = false,
        val unlockPopupExpanded: Boolean = false,
        val shuffleExpanded: Boolean = false
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
        fun devRewardTextSizeSp(): Float
        fun devGemNumberOffsetXDp(): Float
        fun devGemNumberOffsetYDp(): Float
        fun devTicketNumberOffsetXDp(): Float
        fun devTicketNumberOffsetYDp(): Float
        fun devButtonRowOffsetXDp(): Float
        fun devButtonRowOffsetYDp(): Float
        fun devClaimScaleX(): Float
        fun devClaimScaleY(): Float
        fun devClaimScale(): Float
        fun devMultiplierScaleX(): Float
        fun devMultiplierScaleY(): Float
        fun devMultiplierScale(): Float
        fun devVictoryTextSizeSp(): Float
        fun devVictoryOffsetXDp(): Float
        fun devVictoryOffsetYDp(): Float
        fun devDailyTitleOffsetYPx(): Float
        fun devDailyTitleTextSizeSp(): Float
        fun devDailyGemImageHeightDp(): Float
        fun devDailyGemOffsetXDp(): Float
        fun devDailyGemOffsetYDp(): Float
        fun devDailyTicketImageHeightDp(): Float
        fun devDailyTicketOffsetXDp(): Float
        fun devDailyTicketOffsetYDp(): Float
        fun devDailyRewardTextSizeSp(): Float
        fun devDailyGemNumberOffsetXDp(): Float
        fun devDailyGemNumberOffsetYDp(): Float
        fun devDailyTicketNumberOffsetXDp(): Float
        fun devDailyTicketNumberOffsetYDp(): Float
        fun devDailyButtonRowOffsetXDp(): Float
        fun devDailyButtonRowOffsetYDp(): Float
        fun devDailyClaimScaleX(): Float
        fun devDailyClaimScaleY(): Float
        fun devDailyClaimScale(): Float
        fun devDailyMultiplierScaleX(): Float
        fun devDailyMultiplierScaleY(): Float
        fun devDailyMultiplierScale(): Float

        fun onDevSetGemImageHeight(value: Float)
        fun onDevSetGemOffsetX(value: Float)
        fun onDevSetGemOffsetY(value: Float)
        fun onDevSetTicketImageHeight(value: Float)
        fun onDevSetTicketOffsetX(value: Float)
        fun onDevSetTicketOffsetY(value: Float)
        fun onDevSetRewardTextSize(value: Float)
        fun onDevSetGemNumberOffsetX(value: Float)
        fun onDevSetGemNumberOffsetY(value: Float)
        fun onDevSetTicketNumberOffsetX(value: Float)
        fun onDevSetTicketNumberOffsetY(value: Float)
        fun onDevSetButtonRowOffsetX(value: Float)
        fun onDevSetButtonRowOffsetY(value: Float)
        fun onDevSetClaimScaleX(value: Float)
        fun onDevSetClaimScaleY(value: Float)
        fun onDevSetClaimScale(value: Float)
        fun onDevSetMultiplierScaleX(value: Float)
        fun onDevSetMultiplierScaleY(value: Float)
        fun onDevSetMultiplierScale(value: Float)
        fun onDevSetVictoryTextSize(value: Float)
        fun onDevSetVictoryOffsetX(value: Float)
        fun onDevSetVictoryOffsetY(value: Float)
        fun onDevSetDailyTitleOffsetY(value: Float)
        fun onDevSetDailyTitleTextSize(value: Float)
        fun onDevSetDailyGemImageHeight(value: Float)
        fun onDevSetDailyGemOffsetX(value: Float)
        fun onDevSetDailyGemOffsetY(value: Float)
        fun onDevSetDailyTicketImageHeight(value: Float)
        fun onDevSetDailyTicketOffsetX(value: Float)
        fun onDevSetDailyTicketOffsetY(value: Float)
        fun onDevSetDailyRewardTextSize(value: Float)
        fun onDevSetDailyGemNumberOffsetX(value: Float)
        fun onDevSetDailyGemNumberOffsetY(value: Float)
        fun onDevSetDailyTicketNumberOffsetX(value: Float)
        fun onDevSetDailyTicketNumberOffsetY(value: Float)
        fun onDevSetDailyButtonRowOffsetX(value: Float)
        fun onDevSetDailyButtonRowOffsetY(value: Float)
        fun onDevSetDailyClaimScaleX(value: Float)
        fun onDevSetDailyClaimScaleY(value: Float)
        fun onDevSetDailyClaimScale(value: Float)
        fun onDevSetDailyMultiplierScaleX(value: Float)
        fun onDevSetDailyMultiplierScaleY(value: Float)
        fun onDevSetDailyMultiplierScale(value: Float)
        fun onDevApplyAutoWinPopupRatios()
        fun onDevApplyAutoDailyPopupRatios()

        // Unlock help popup
        fun devUnlockFrameScaleX(): Float
        fun devUnlockFrameScaleY(): Float
        fun devUnlockDescTextSizeSp(): Float
        fun devUnlockDescOffsetXDp(): Float
        fun devUnlockDescOffsetYDp(): Float
        fun devUnlockAdBtnScaleX(): Float
        fun devUnlockAdBtnScaleY(): Float
        fun devUnlockAdBtnOffsetXDp(): Float
        fun devUnlockAdBtnOffsetYDp(): Float
        fun devUnlockCancelBtnScaleX(): Float
        fun devUnlockCancelBtnScaleY(): Float
        fun devUnlockCancelBtnOffsetXDp(): Float
        fun devUnlockCancelBtnOffsetYDp(): Float
        fun devLockedPileAdOffsetXPortraitPx(): Float
        fun devLockedPileAdOffsetYPortraitPx(): Float
        fun devLockedPileAdScaleXPortrait(): Float
        fun devLockedPileAdScaleYPortrait(): Float
        fun devLockedPileAdOffsetXLandscapePx(): Float
        fun devLockedPileAdOffsetYLandscapePx(): Float
        fun devLockedPileAdScaleXLandscape(): Float
        fun devLockedPileAdScaleYLandscape(): Float
        fun onDevSetUnlockFrameScaleX(value: Float)
        fun onDevSetUnlockFrameScaleY(value: Float)
        fun onDevSetUnlockDescTextSize(value: Float)
        fun onDevSetUnlockDescOffsetX(value: Float)
        fun onDevSetUnlockDescOffsetY(value: Float)
        fun onDevSetUnlockAdBtnScaleX(value: Float)
        fun onDevSetUnlockAdBtnScaleY(value: Float)
        fun onDevSetUnlockAdBtnOffsetX(value: Float)
        fun onDevSetUnlockAdBtnOffsetY(value: Float)
        fun onDevSetUnlockCancelBtnScaleX(value: Float)
        fun onDevSetUnlockCancelBtnScaleY(value: Float)
        fun onDevSetUnlockCancelBtnOffsetX(value: Float)
        fun onDevSetUnlockCancelBtnOffsetY(value: Float)
        fun onDevSetLockedPileAdOffsetXPortraitPx(value: Float)
        fun onDevSetLockedPileAdOffsetYPortraitPx(value: Float)
        fun onDevSetLockedPileAdScaleXPortrait(value: Float)
        fun onDevSetLockedPileAdScaleYPortrait(value: Float)
        fun onDevSetLockedPileAdOffsetXLandscapePx(value: Float)
        fun onDevSetLockedPileAdOffsetYLandscapePx(value: Float)
        fun onDevSetLockedPileAdScaleXLandscape(value: Float)
        fun onDevSetLockedPileAdScaleYLandscape(value: Float)

        // Shuffle/deal timing
        fun devShuffleSecondClipDelayMs(): Float
        fun devShuffleTailDelayMs(): Float
        fun devDealCardIntervalMs(): Float
        fun onDevSetShuffleSecondClipDelayMs(value: Float)
        fun onDevSetShuffleTailDelayMs(value: Float)
        fun onDevSetDealCardIntervalMs(value: Float)

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

        val dailyPopupHeader = view.findViewById<View>(R.id.layout_develop_daily_popup_header)
        val dailyPopupArrow = view.findViewById<TextView>(R.id.tv_develop_daily_popup_arrow)
        val dailyPopupContent = view.findViewById<LinearLayout>(R.id.layout_develop_daily_popup_content)
        var dailyPopupExpanded = expandState.dailyPopupExpanded
        setSectionExpanded(dailyPopupContent, dailyPopupArrow, dailyPopupExpanded)
        dailyPopupHeader.setOnClickListener {
            dailyPopupExpanded = !dailyPopupExpanded
            setSectionExpanded(dailyPopupContent, dailyPopupArrow, dailyPopupExpanded)
            expandState = expandState.copy(dailyPopupExpanded = dailyPopupExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        val unlockPopupHeader = view.findViewById<View>(R.id.layout_develop_unlock_popup_header)
        val unlockPopupArrow = view.findViewById<TextView>(R.id.tv_develop_unlock_popup_arrow)
        val unlockPopupContent = view.findViewById<LinearLayout>(R.id.layout_develop_unlock_popup_content)
        var unlockPopupExpanded = expandState.unlockPopupExpanded
        setSectionExpanded(unlockPopupContent, unlockPopupArrow, unlockPopupExpanded)
        unlockPopupHeader.setOnClickListener {
            unlockPopupExpanded = !unlockPopupExpanded
            setSectionExpanded(unlockPopupContent, unlockPopupArrow, unlockPopupExpanded)
            expandState = expandState.copy(unlockPopupExpanded = unlockPopupExpanded)
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

        val tableauHeader = view.findViewById<View>(R.id.layout_develop_tableau_header)
        val tableauArrow = view.findViewById<TextView>(R.id.tv_develop_tableau_arrow)
        val tableauContent = view.findViewById<LinearLayout>(R.id.layout_develop_tableau_content)
        var tableauExpanded = expandState.tableauExpanded
        setSectionExpanded(tableauContent, tableauArrow, tableauExpanded)
        tableauHeader.setOnClickListener {
            tableauExpanded = !tableauExpanded
            setSectionExpanded(tableauContent, tableauArrow, tableauExpanded)
            expandState = expandState.copy(tableauExpanded = tableauExpanded)
            host.onDevExpandStateChanged(expandState)
        }

        bindStarburstControls(view, host)
        bindPopupControls(view, host)
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
        bindDecimal(R.id.btn_dev_reward_text_size, R.string.develop_menu_reward_text_size, host::devRewardTextSizeSp, host::onDevSetRewardTextSize)
        bindDecimal(R.id.btn_dev_gem_number_offset_x, R.string.develop_menu_gem_number_offset_x, host::devGemNumberOffsetXDp, host::onDevSetGemNumberOffsetX)
        bindDecimal(R.id.btn_dev_gem_number_offset_y, R.string.develop_menu_gem_number_offset_y, host::devGemNumberOffsetYDp, host::onDevSetGemNumberOffsetY)
        bindDecimal(R.id.btn_dev_ticket_number_offset_x, R.string.develop_menu_ticket_number_offset_x, host::devTicketNumberOffsetXDp, host::onDevSetTicketNumberOffsetX)
        bindDecimal(R.id.btn_dev_ticket_number_offset_y, R.string.develop_menu_ticket_number_offset_y, host::devTicketNumberOffsetYDp, host::onDevSetTicketNumberOffsetY)
        bindDecimal(R.id.btn_dev_button_row_offset_x, R.string.develop_menu_button_row_offset_x, host::devButtonRowOffsetXDp, host::onDevSetButtonRowOffsetX)
        bindDecimal(R.id.btn_dev_button_row_offset_y, R.string.develop_menu_button_row_offset_y, host::devButtonRowOffsetYDp, host::onDevSetButtonRowOffsetY)
        bindDecimal(R.id.btn_dev_claim_scale_x, R.string.develop_menu_claim_scale_x, host::devClaimScaleX, host::onDevSetClaimScaleX)
        bindDecimal(R.id.btn_dev_claim_scale_y, R.string.develop_menu_claim_scale_y, host::devClaimScaleY, host::onDevSetClaimScaleY)
        bindDecimal(R.id.btn_dev_claim_scale, R.string.develop_menu_claim_scale, host::devClaimScale, host::onDevSetClaimScale)
        bindDecimal(R.id.btn_dev_multiplier_scale_x, R.string.develop_menu_multiplier_scale_x, host::devMultiplierScaleX, host::onDevSetMultiplierScaleX)
        bindDecimal(R.id.btn_dev_multiplier_scale_y, R.string.develop_menu_multiplier_scale_y, host::devMultiplierScaleY, host::onDevSetMultiplierScaleY)
        bindDecimal(R.id.btn_dev_multiplier_scale, R.string.develop_menu_multiplier_scale, host::devMultiplierScale, host::onDevSetMultiplierScale)
        bindDecimal(R.id.btn_dev_victory_text_size, R.string.develop_menu_victory_text_size, host::devVictoryTextSizeSp, host::onDevSetVictoryTextSize)
        bindDecimal(R.id.btn_dev_victory_offset_x, R.string.develop_menu_victory_offset_x, host::devVictoryOffsetXDp, host::onDevSetVictoryOffsetX)
        bindDecimal(R.id.btn_dev_victory_offset_y, R.string.develop_menu_victory_offset_y, host::devVictoryOffsetYDp, host::onDevSetVictoryOffsetY)
        bindDecimal(R.id.btn_dev_shuffle_second_clip_delay_ms, R.string.develop_menu_shuffle_second_clip_delay_ms, host::devShuffleSecondClipDelayMs, host::onDevSetShuffleSecondClipDelayMs)
        bindDecimal(R.id.btn_dev_shuffle_tail_delay_ms, R.string.develop_menu_shuffle_tail_delay_ms, host::devShuffleTailDelayMs, host::onDevSetShuffleTailDelayMs)
        bindDecimal(R.id.btn_dev_deal_card_interval_ms, R.string.develop_menu_deal_card_interval_ms, host::devDealCardIntervalMs, host::onDevSetDealCardIntervalMs)

        view.findViewById<MaterialButton>(R.id.btn_dev_daily_popup_apply_auto).setOnClickListener {
            host.onDevApplyAutoDailyPopupRatios()
            refreshPopupDisplays(view, host)
        }

        bindDecimal(R.id.btn_dev_daily_title_offset_y, R.string.develop_menu_daily_title_offset_y, host::devDailyTitleOffsetYPx, host::onDevSetDailyTitleOffsetY)
        bindDecimal(R.id.btn_dev_daily_title_text_size, R.string.develop_menu_daily_title_text_size, host::devDailyTitleTextSizeSp, host::onDevSetDailyTitleTextSize)
        bindDecimal(R.id.btn_dev_daily_gem_image_height, R.string.develop_menu_gem_image_height, host::devDailyGemImageHeightDp, host::onDevSetDailyGemImageHeight)
        bindDecimal(R.id.btn_dev_daily_gem_offset_x, R.string.develop_menu_gem_offset_x, host::devDailyGemOffsetXDp, host::onDevSetDailyGemOffsetX)
        bindDecimal(R.id.btn_dev_daily_gem_offset_y, R.string.develop_menu_gem_offset_y, host::devDailyGemOffsetYDp, host::onDevSetDailyGemOffsetY)
        bindDecimal(R.id.btn_dev_daily_ticket_image_height, R.string.develop_menu_ticket_image_height, host::devDailyTicketImageHeightDp, host::onDevSetDailyTicketImageHeight)
        bindDecimal(R.id.btn_dev_daily_ticket_offset_x, R.string.develop_menu_ticket_offset_x, host::devDailyTicketOffsetXDp, host::onDevSetDailyTicketOffsetX)
        bindDecimal(R.id.btn_dev_daily_ticket_offset_y, R.string.develop_menu_ticket_offset_y, host::devDailyTicketOffsetYDp, host::onDevSetDailyTicketOffsetY)
        bindDecimal(R.id.btn_dev_daily_reward_text_size, R.string.develop_menu_reward_text_size, host::devDailyRewardTextSizeSp, host::onDevSetDailyRewardTextSize)
        bindDecimal(R.id.btn_dev_daily_gem_number_offset_x, R.string.develop_menu_gem_number_offset_x, host::devDailyGemNumberOffsetXDp, host::onDevSetDailyGemNumberOffsetX)
        bindDecimal(R.id.btn_dev_daily_gem_number_offset_y, R.string.develop_menu_gem_number_offset_y, host::devDailyGemNumberOffsetYDp, host::onDevSetDailyGemNumberOffsetY)
        bindDecimal(R.id.btn_dev_daily_ticket_number_offset_x, R.string.develop_menu_ticket_number_offset_x, host::devDailyTicketNumberOffsetXDp, host::onDevSetDailyTicketNumberOffsetX)
        bindDecimal(R.id.btn_dev_daily_ticket_number_offset_y, R.string.develop_menu_ticket_number_offset_y, host::devDailyTicketNumberOffsetYDp, host::onDevSetDailyTicketNumberOffsetY)
        bindDecimal(R.id.btn_dev_daily_button_row_offset_x, R.string.develop_menu_button_row_offset_x, host::devDailyButtonRowOffsetXDp, host::onDevSetDailyButtonRowOffsetX)
        bindDecimal(R.id.btn_dev_daily_button_row_offset_y, R.string.develop_menu_button_row_offset_y, host::devDailyButtonRowOffsetYDp, host::onDevSetDailyButtonRowOffsetY)
        bindDecimal(R.id.btn_dev_daily_claim_scale_x, R.string.develop_menu_claim_scale_x, host::devDailyClaimScaleX, host::onDevSetDailyClaimScaleX)
        bindDecimal(R.id.btn_dev_daily_claim_scale_y, R.string.develop_menu_claim_scale_y, host::devDailyClaimScaleY, host::onDevSetDailyClaimScaleY)
        bindDecimal(R.id.btn_dev_daily_claim_scale, R.string.develop_menu_claim_scale, host::devDailyClaimScale, host::onDevSetDailyClaimScale)
        bindDecimal(R.id.btn_dev_daily_multiplier_scale_x, R.string.develop_menu_multiplier_scale_x, host::devDailyMultiplierScaleX, host::onDevSetDailyMultiplierScaleX)
        bindDecimal(R.id.btn_dev_daily_multiplier_scale_y, R.string.develop_menu_multiplier_scale_y, host::devDailyMultiplierScaleY, host::onDevSetDailyMultiplierScaleY)
        bindDecimal(R.id.btn_dev_daily_multiplier_scale, R.string.develop_menu_multiplier_scale, host::devDailyMultiplierScale, host::onDevSetDailyMultiplierScale)

        // Unlock popup controls
        bindDecimal(R.id.btn_dev_unlock_frame_scale_x, R.string.develop_menu_unlock_frame_scale_x, host::devUnlockFrameScaleX, host::onDevSetUnlockFrameScaleX)
        bindDecimal(R.id.btn_dev_unlock_frame_scale_y, R.string.develop_menu_unlock_frame_scale_y, host::devUnlockFrameScaleY, host::onDevSetUnlockFrameScaleY)
        bindDecimal(R.id.btn_dev_unlock_desc_text_size, R.string.develop_menu_unlock_desc_text_size, host::devUnlockDescTextSizeSp, host::onDevSetUnlockDescTextSize)
        bindDecimal(R.id.btn_dev_unlock_desc_offset_x, R.string.develop_menu_unlock_desc_offset_x, host::devUnlockDescOffsetXDp, host::onDevSetUnlockDescOffsetX)
        bindDecimal(R.id.btn_dev_unlock_desc_offset_y, R.string.develop_menu_unlock_desc_offset_y, host::devUnlockDescOffsetYDp, host::onDevSetUnlockDescOffsetY)
        bindDecimal(R.id.btn_dev_unlock_ad_btn_scale_x, R.string.develop_menu_unlock_ad_btn_scale_x, host::devUnlockAdBtnScaleX, host::onDevSetUnlockAdBtnScaleX)
        bindDecimal(R.id.btn_dev_unlock_ad_btn_scale_y, R.string.develop_menu_unlock_ad_btn_scale_y, host::devUnlockAdBtnScaleY, host::onDevSetUnlockAdBtnScaleY)
        bindDecimal(R.id.btn_dev_unlock_ad_btn_offset_x, R.string.develop_menu_unlock_ad_btn_offset_x, host::devUnlockAdBtnOffsetXDp, host::onDevSetUnlockAdBtnOffsetX)
        bindDecimal(R.id.btn_dev_unlock_ad_btn_offset_y, R.string.develop_menu_unlock_ad_btn_offset_y, host::devUnlockAdBtnOffsetYDp, host::onDevSetUnlockAdBtnOffsetY)
        bindDecimal(R.id.btn_dev_unlock_cancel_btn_scale_x, R.string.develop_menu_unlock_cancel_btn_scale_x, host::devUnlockCancelBtnScaleX, host::onDevSetUnlockCancelBtnScaleX)
        bindDecimal(R.id.btn_dev_unlock_cancel_btn_scale_y, R.string.develop_menu_unlock_cancel_btn_scale_y, host::devUnlockCancelBtnScaleY, host::onDevSetUnlockCancelBtnScaleY)
        bindDecimal(R.id.btn_dev_unlock_cancel_btn_offset_x, R.string.develop_menu_unlock_cancel_btn_offset_x, host::devUnlockCancelBtnOffsetXDp, host::onDevSetUnlockCancelBtnOffsetX)
        bindDecimal(R.id.btn_dev_unlock_cancel_btn_offset_y, R.string.develop_menu_unlock_cancel_btn_offset_y, host::devUnlockCancelBtnOffsetYDp, host::onDevSetUnlockCancelBtnOffsetY)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_x_portrait, R.string.develop_menu_locked_pile_ad_offset_x_portrait, host::devLockedPileAdOffsetXPortraitPx, host::onDevSetLockedPileAdOffsetXPortraitPx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_y_portrait, R.string.develop_menu_locked_pile_ad_offset_y_portrait, host::devLockedPileAdOffsetYPortraitPx, host::onDevSetLockedPileAdOffsetYPortraitPx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_x_portrait, R.string.develop_menu_locked_pile_ad_scale_x_portrait, host::devLockedPileAdScaleXPortrait, host::onDevSetLockedPileAdScaleXPortrait)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_y_portrait, R.string.develop_menu_locked_pile_ad_scale_y_portrait, host::devLockedPileAdScaleYPortrait, host::onDevSetLockedPileAdScaleYPortrait)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_x_landscape, R.string.develop_menu_locked_pile_ad_offset_x_landscape, host::devLockedPileAdOffsetXLandscapePx, host::onDevSetLockedPileAdOffsetXLandscapePx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_offset_y_landscape, R.string.develop_menu_locked_pile_ad_offset_y_landscape, host::devLockedPileAdOffsetYLandscapePx, host::onDevSetLockedPileAdOffsetYLandscapePx)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_x_landscape, R.string.develop_menu_locked_pile_ad_scale_x_landscape, host::devLockedPileAdScaleXLandscape, host::onDevSetLockedPileAdScaleXLandscape)
        bindDecimal(R.id.btn_dev_locked_pile_ad_scale_y_landscape, R.string.develop_menu_locked_pile_ad_scale_y_landscape, host::devLockedPileAdScaleYLandscape, host::onDevSetLockedPileAdScaleYLandscape)
    }

    private fun refreshAllDisplays(root: View, host: Host) {
        refreshStarburstDisplays(host)
        refreshPopupDisplays(root, host)
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
        root.findViewById<MaterialButton>(R.id.btn_dev_reward_text_size).text = fmt(host.devRewardTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_number_offset_x).text = fmt(host.devGemNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_gem_number_offset_y).text = fmt(host.devGemNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_number_offset_x).text = fmt(host.devTicketNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_ticket_number_offset_y).text = fmt(host.devTicketNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_button_row_offset_x).text = fmt(host.devButtonRowOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_button_row_offset_y).text = fmt(host.devButtonRowOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_claim_scale_x).text = fmt(host.devClaimScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_claim_scale_y).text = fmt(host.devClaimScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_claim_scale).text = fmt(host.devClaimScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_multiplier_scale_x).text = fmt(host.devMultiplierScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_multiplier_scale_y).text = fmt(host.devMultiplierScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_multiplier_scale).text = fmt(host.devMultiplierScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_text_size).text = fmt(host.devVictoryTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_offset_x).text = fmt(host.devVictoryOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_victory_offset_y).text = fmt(host.devVictoryOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_shuffle_second_clip_delay_ms).text = fmt(host.devShuffleSecondClipDelayMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_shuffle_tail_delay_ms).text = fmt(host.devShuffleTailDelayMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_deal_card_interval_ms).text = fmt(host.devDealCardIntervalMs())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_title_offset_y).text = fmt(host.devDailyTitleOffsetYPx())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_title_text_size).text = fmt(host.devDailyTitleTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_gem_image_height).text = fmt(host.devDailyGemImageHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_gem_offset_x).text = fmt(host.devDailyGemOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_gem_offset_y).text = fmt(host.devDailyGemOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_ticket_image_height).text = fmt(host.devDailyTicketImageHeightDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_ticket_offset_x).text = fmt(host.devDailyTicketOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_ticket_offset_y).text = fmt(host.devDailyTicketOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_reward_text_size).text = fmt(host.devDailyRewardTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_gem_number_offset_x).text = fmt(host.devDailyGemNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_gem_number_offset_y).text = fmt(host.devDailyGemNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_ticket_number_offset_x).text = fmt(host.devDailyTicketNumberOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_ticket_number_offset_y).text = fmt(host.devDailyTicketNumberOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_button_row_offset_x).text = fmt(host.devDailyButtonRowOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_button_row_offset_y).text = fmt(host.devDailyButtonRowOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_claim_scale_x).text = fmt(host.devDailyClaimScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_claim_scale_y).text = fmt(host.devDailyClaimScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_claim_scale).text = fmt(host.devDailyClaimScale())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_multiplier_scale_x).text = fmt(host.devDailyMultiplierScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_multiplier_scale_y).text = fmt(host.devDailyMultiplierScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_daily_multiplier_scale).text = fmt(host.devDailyMultiplierScale())
        // Unlock popup
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_frame_scale_x).text = fmt(host.devUnlockFrameScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_frame_scale_y).text = fmt(host.devUnlockFrameScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_desc_text_size).text = fmt(host.devUnlockDescTextSizeSp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_desc_offset_x).text = fmt(host.devUnlockDescOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_desc_offset_y).text = fmt(host.devUnlockDescOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_ad_btn_scale_x).text = fmt(host.devUnlockAdBtnScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_ad_btn_scale_y).text = fmt(host.devUnlockAdBtnScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_ad_btn_offset_x).text = fmt(host.devUnlockAdBtnOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_ad_btn_offset_y).text = fmt(host.devUnlockAdBtnOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_cancel_btn_scale_x).text = fmt(host.devUnlockCancelBtnScaleX())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_cancel_btn_scale_y).text = fmt(host.devUnlockCancelBtnScaleY())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_cancel_btn_offset_x).text = fmt(host.devUnlockCancelBtnOffsetXDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_unlock_cancel_btn_offset_y).text = fmt(host.devUnlockCancelBtnOffsetYDp())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_x_portrait).text = fmt(host.devLockedPileAdOffsetXPortraitPx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_y_portrait).text = fmt(host.devLockedPileAdOffsetYPortraitPx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_x_portrait).text = fmt(host.devLockedPileAdScaleXPortrait())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_y_portrait).text = fmt(host.devLockedPileAdScaleYPortrait())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_x_landscape).text = fmt(host.devLockedPileAdOffsetXLandscapePx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_offset_y_landscape).text = fmt(host.devLockedPileAdOffsetYLandscapePx())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_x_landscape).text = fmt(host.devLockedPileAdScaleXLandscape())
        root.findViewById<MaterialButton>(R.id.btn_dev_locked_pile_ad_scale_y_landscape).text = fmt(host.devLockedPileAdScaleYLandscape())
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
            tableauExpanded = b.getBoolean(ARG_TABLEAU_EXPANDED, false),
            dailyPopupExpanded = b.getBoolean(ARG_DAILY_POPUP_EXPANDED, false),
            unlockPopupExpanded = b.getBoolean(ARG_UNLOCK_POPUP_EXPANDED, false),
            shuffleExpanded = b.getBoolean(ARG_SHUFFLE_EXPANDED, false)
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
        private const val ARG_TABLEAU_EXPANDED = "arg_tableau_expanded"
        private const val ARG_DAILY_POPUP_EXPANDED = "arg_daily_popup_expanded"
        private const val ARG_UNLOCK_POPUP_EXPANDED = "arg_unlock_popup_expanded"
        private const val ARG_SHUFFLE_EXPANDED = "arg_shuffle_expanded"

        fun newInstance(state: ExpandState = ExpandState()): DevelopMenuDialogFragment {
            return DevelopMenuDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STARBURST_EXPANDED, state.starburstExpanded)
                    putBoolean(ARG_POPUP_EXPANDED, state.popupExpanded)
                    putBoolean(ARG_TABLEAU_EXPANDED, state.tableauExpanded)
                    putBoolean(ARG_DAILY_POPUP_EXPANDED, state.dailyPopupExpanded)
                    putBoolean(ARG_UNLOCK_POPUP_EXPANDED, state.unlockPopupExpanded)
                    putBoolean(ARG_SHUFFLE_EXPANDED, state.shuffleExpanded)
                }
            }
        }
    }
}
