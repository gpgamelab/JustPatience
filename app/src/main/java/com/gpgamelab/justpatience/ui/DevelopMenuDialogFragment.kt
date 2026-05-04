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
        private const val ARG_SHUFFLE_EXPANDED = "arg_shuffle_expanded"

        fun newInstance(state: ExpandState = ExpandState()): DevelopMenuDialogFragment {
            return DevelopMenuDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STARBURST_EXPANDED, state.starburstExpanded)
                    putBoolean(ARG_POPUP_EXPANDED, state.popupExpanded)
                    putBoolean(ARG_TABLEAU_EXPANDED, state.tableauExpanded)
                    putBoolean(ARG_SHUFFLE_EXPANDED, state.shuffleExpanded)
                }
            }
        }
    }
}
