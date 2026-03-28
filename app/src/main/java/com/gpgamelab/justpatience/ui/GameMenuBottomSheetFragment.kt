package com.gpgamelab.justpatience.ui

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.R as MaterialR
import com.gpgamelab.justpatience.R

class GameMenuBottomSheetFragment : BottomSheetDialogFragment() {

    data class ExpandState(
        val statisticsExpanded: Boolean = false,
        val informationExpanded: Boolean = false,
        val settingsExpanded: Boolean = false,
        val commonExpanded: Boolean = false,
        val advancedExpanded: Boolean = false
    )

    interface Host {
        fun onGameMenuStatisticsSummary()
        fun onGameMenuStatisticsHistory()
        fun onGameMenuResetStats()
        fun onGameMenuOpenAbout()
        fun onGameMenuOpenHowToPlay()
        fun onGameMenuRateUs()
        fun onGameMenuOpenSettings()
        fun onGameMenuExitApp()
        fun onGameMenuExpandStateChanged(state: ExpandState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_game_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val host = activity as? Host ?: return
        var expandState = readExpandStateFromArgs()

        // Collapsible sections
        val statisticsHeader = view.findViewById<View>(R.id.menu_statistics_row)
        val statisticsArrow = view.findViewById<TextView>(R.id.menu_statistics_arrow)
        val statisticsContent = view.findViewById<LinearLayout>(R.id.menu_statistics_content)

        val informationHeader = view.findViewById<View>(R.id.menu_information_row)
        val informationArrow = view.findViewById<TextView>(R.id.menu_information_arrow)
        val informationContent = view.findViewById<LinearLayout>(R.id.menu_information_content)

        val settingsHeader = view.findViewById<View>(R.id.menu_settings_row)
        val settingsArrow = view.findViewById<TextView>(R.id.menu_settings_arrow)
        val settingsContent = view.findViewById<LinearLayout>(R.id.menu_settings_content)

        val commonHeader = view.findViewById<View>(R.id.menu_settings_common_row)
        val commonArrow = view.findViewById<TextView>(R.id.menu_settings_common_arrow)
        val commonContent = view.findViewById<LinearLayout>(R.id.menu_settings_common_content)

        val advancedHeader = view.findViewById<View>(R.id.menu_settings_advanced_row)
        val advancedArrow = view.findViewById<TextView>(R.id.menu_settings_advanced_arrow)
        val advancedContent = view.findViewById<LinearLayout>(R.id.menu_settings_advanced_content)

        setSectionExpanded(statisticsContent, statisticsArrow, expandState.statisticsExpanded)
        setSectionExpanded(informationContent, informationArrow, expandState.informationExpanded)
        setSectionExpanded(settingsContent, settingsArrow, expandState.settingsExpanded)
        setSectionExpanded(commonContent, commonArrow, expandState.settingsExpanded && expandState.commonExpanded)
        setSectionExpanded(advancedContent, advancedArrow, expandState.settingsExpanded && expandState.advancedExpanded)

        statisticsHeader.setOnClickListener {
            val expanded = statisticsContent.visibility != View.VISIBLE
            setSectionExpanded(statisticsContent, statisticsArrow, expanded)
            expandState = expandState.copy(statisticsExpanded = expanded)
            host.onGameMenuExpandStateChanged(expandState)
        }

        informationHeader.setOnClickListener {
            val expanded = informationContent.visibility != View.VISIBLE
            setSectionExpanded(informationContent, informationArrow, expanded)
            expandState = expandState.copy(informationExpanded = expanded)
            host.onGameMenuExpandStateChanged(expandState)
        }

        settingsHeader.setOnClickListener {
            val expanded = settingsContent.visibility != View.VISIBLE
            setSectionExpanded(settingsContent, settingsArrow, expanded)
            setSectionExpanded(commonContent, commonArrow, expanded && expandState.commonExpanded)
            setSectionExpanded(advancedContent, advancedArrow, expanded && expandState.advancedExpanded)
            expandState = expandState.copy(settingsExpanded = expanded)
            host.onGameMenuExpandStateChanged(expandState)
        }

        commonHeader.setOnClickListener {
            val expanded = commonContent.visibility != View.VISIBLE
            setSectionExpanded(commonContent, commonArrow, expanded)
            expandState = expandState.copy(commonExpanded = expanded)
            host.onGameMenuExpandStateChanged(expandState)
        }

        advancedHeader.setOnClickListener {
            val expanded = advancedContent.visibility != View.VISIBLE
            setSectionExpanded(advancedContent, advancedArrow, expanded)
            expandState = expandState.copy(advancedExpanded = expanded)
            host.onGameMenuExpandStateChanged(expandState)
        }

        // Active actions
        view.findViewById<View>(R.id.menu_stats_summary_row).setOnClickListener {
            dismissAndRun { host.onGameMenuStatisticsSummary() }
        }
        view.findViewById<View>(R.id.menu_stats_history_row).setOnClickListener {
            dismissAndRun { host.onGameMenuStatisticsHistory() }
        }
        view.findViewById<View>(R.id.menu_stats_reset_row).setOnClickListener {
            dismissAndRun { host.onGameMenuResetStats() }
        }
        view.findViewById<View>(R.id.menu_how_to_play_row).setOnClickListener {
            dismissAndRun { host.onGameMenuOpenHowToPlay() }
        }
        view.findViewById<View>(R.id.menu_rate_us_row).setOnClickListener {
            dismissAndRun { host.onGameMenuRateUs() }
        }
        view.findViewById<View>(R.id.menu_about_row).setOnClickListener {
            dismissAndRun { host.onGameMenuOpenAbout() }
        }

        // Existing settings screen handles current settings items.
        val settingsRows = intArrayOf(
            R.id.menu_common_nickname_row,
            R.id.menu_common_draw_cards_row,
            R.id.menu_common_waste_recycles_row,
            R.id.menu_common_show_hints_row,
            R.id.menu_common_mute_music_row,
            R.id.menu_common_mute_card_sounds_row,
            R.id.menu_common_mute_win_sound_row,
            R.id.menu_advanced_show_timer_row,
            R.id.menu_advanced_show_card_animations_row,
            R.id.menu_advanced_show_win_animation_row,
            R.id.menu_advanced_board_layout_row,
            R.id.menu_advanced_hint_delay_row
        )
        settingsRows.forEach { id ->
            view.findViewById<View>(id).setOnClickListener {
                dismissAndRun { host.onGameMenuOpenSettings() }
            }
        }

        view.findViewById<View>(R.id.menu_exit_app_row).setOnClickListener {
            dismissAndRun { host.onGameMenuExitApp() }
        }
    }

    override fun onStart() {
        super.onStart()

        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(MaterialR.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val maxHeightFraction = if (isLandscape) 0.50f else 0.30f
        val maxHeight = (resources.displayMetrics.heightPixels * maxHeightFraction).toInt().coerceAtLeast(1)

        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = maxHeight
        }

        behavior.skipCollapsed = true
        behavior.peekHeight = maxHeight
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun dismissAndRun(action: () -> Unit) {
        dismiss()
        activity?.window?.decorView?.post(action) ?: action()
    }

    private fun setSectionExpanded(content: View, arrow: TextView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        arrow.text = if (expanded) "▴" else "▾"
    }

    private fun readExpandStateFromArgs(): ExpandState {
        val b = arguments ?: return ExpandState()
        return ExpandState(
            statisticsExpanded = b.getBoolean(ARG_STATISTICS_EXPANDED, false),
            informationExpanded = b.getBoolean(ARG_INFORMATION_EXPANDED, false),
            settingsExpanded = b.getBoolean(ARG_SETTINGS_EXPANDED, false),
            commonExpanded = b.getBoolean(ARG_COMMON_EXPANDED, false),
            advancedExpanded = b.getBoolean(ARG_ADVANCED_EXPANDED, false)
        )
    }

    companion object {
        const val TAG = "game_menu_bottom_sheet"
        private const val ARG_STATISTICS_EXPANDED = "arg_statistics_expanded"
        private const val ARG_INFORMATION_EXPANDED = "arg_information_expanded"
        private const val ARG_SETTINGS_EXPANDED = "arg_settings_expanded"
        private const val ARG_COMMON_EXPANDED = "arg_common_expanded"
        private const val ARG_ADVANCED_EXPANDED = "arg_advanced_expanded"

        fun newInstance(state: ExpandState = ExpandState()): GameMenuBottomSheetFragment {
            return GameMenuBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_STATISTICS_EXPANDED, state.statisticsExpanded)
                    putBoolean(ARG_INFORMATION_EXPANDED, state.informationExpanded)
                    putBoolean(ARG_SETTINGS_EXPANDED, state.settingsExpanded)
                    putBoolean(ARG_COMMON_EXPANDED, state.commonExpanded)
                    putBoolean(ARG_ADVANCED_EXPANDED, state.advancedExpanded)
                }
            }
        }
    }
}



