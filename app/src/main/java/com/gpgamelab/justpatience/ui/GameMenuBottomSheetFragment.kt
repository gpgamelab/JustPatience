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
import com.gpgamelab.justpatience.util.UiScaleUtil

class GameMenuBottomSheetFragment : BottomSheetDialogFragment() {

    data class ExpandState(
        val statisticsExpanded: Boolean = false,
        val informationExpanded: Boolean = false,
        val settingsExpanded: Boolean = false
    )

    interface Host {
        fun onGameMenuStatisticsSummary()
        fun onGameMenuStatisticsHistory()
        fun onGameMenuResetStats()
        fun onGameMenuOpenAbout()
        fun onGameMenuOpenHowToPlay()
        fun onGameMenuRateUs()
        fun onGameMenuShareApp()
        fun onGameMenuContactUs()
        fun onGameMenuOpenPrivacyPolicy()
        fun onGameMenuOpenTermsOfService()
        fun onGameMenuEditNickname()
        fun onGameMenuDrawCards()
        fun onGameMenuDeckCount()
        fun onGameMenuWasteRecycles()
        fun onGameMenuSoundToggle()
        fun onGameMenuShowGameTimerToggle()
        fun onGameMenuShowScoreToggle()
        fun onGameMenuShowMovesToggle()
        fun onGameMenuShowCardAnimationsToggle()
        fun onGameMenuAutoCompleteToggle()
        fun onGameMenuHapticsToggle()
        fun onGameMenuTapToMoveToggle()
        fun onGameMenuBoardLayout()
        fun onGameMenuScoreMethod()
        fun onGameMenuFoundationToTableauToggle()
        fun onGameMenuEnforceFoundationBalanceToggle()
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
        UiScaleUtil.applyBaselineScale(view, requireContext())

        val host = activity as? Host ?: return
        var expandState = readExpandStateFromArgs()
        val currentNickname = arguments?.getString(ARG_CURRENT_NICKNAME).orEmpty().trim()
        val currentDrawSize = arguments?.getInt(ARG_CURRENT_DRAW_SIZE, DEFAULT_DRAW_SIZE) ?: DEFAULT_DRAW_SIZE
        val currentDeckCount = arguments?.getInt(ARG_CURRENT_DECK_COUNT, DEFAULT_DECK_COUNT) ?: DEFAULT_DECK_COUNT
        val currentInfiniteRecycles = arguments?.getBoolean(ARG_CURRENT_INFINITE_RECYCLES, true) ?: true
        val currentRecycleCount = arguments?.getInt(ARG_CURRENT_RECYCLE_COUNT, DEFAULT_RECYCLE_COUNT) ?: DEFAULT_RECYCLE_COUNT
        val currentSoundOn = arguments?.getBoolean(ARG_CURRENT_SOUND_ON, true) ?: true
        val currentShowGameTimer = arguments?.getBoolean(ARG_CURRENT_SHOW_GAME_TIMER, true) ?: true
        val currentShowScore = arguments?.getBoolean(ARG_CURRENT_SHOW_SCORE, true) ?: true
        val currentShowMoves = arguments?.getBoolean(ARG_CURRENT_SHOW_MOVES, true) ?: true
        val currentShowCardAnimations = arguments?.getBoolean(ARG_CURRENT_SHOW_CARD_ANIMATIONS, true) ?: true
        val currentAutoComplete = arguments?.getBoolean(ARG_CURRENT_AUTO_COMPLETE, true) ?: true
        val currentHaptics = arguments?.getBoolean(ARG_CURRENT_HAPTICS, false) ?: false
        val currentTapToMove = arguments?.getBoolean(ARG_CURRENT_TAP_TO_MOVE, true) ?: true
        val currentScoreMethod = arguments?.getString(ARG_CURRENT_SCORE_METHOD) ?: "windows"
        val currentFoundationToTableau = arguments?.getBoolean(ARG_CURRENT_FOUNDATION_TO_TABLEAU, false) ?: false
        val currentEnforceFoundationBalance = arguments?.getBoolean(ARG_CURRENT_ENFORCE_FOUNDATION_BALANCE, false) ?: false
        if (currentNickname.isNotEmpty()) {
            val nicknameLabel = view.findViewById<TextView>(R.id.menu_common_nickname_text)
            nicknameLabel.text = getString(
                R.string.game_menu_my_nickname_with_value,
                formatNicknameForMenu(currentNickname)
            )
            nicknameLabel.contentDescription = getString(
                R.string.game_menu_my_nickname_with_value,
                currentNickname
            )
        }
        val normalizedDrawSize = if (currentDrawSize == 3) 3 else 1
        val normalizedDeckCount = if (currentDeckCount == 2) 2 else 1
        val drawCardsLabel = view.findViewById<TextView>(R.id.menu_common_draw_cards_text)
        drawCardsLabel.text = getString(R.string.game_menu_draw_cards_with_value, normalizedDrawSize)

        val deckLabel = if (normalizedDeckCount == 2) {
            getString(R.string.game_menu_deck_two)
        } else {
            getString(R.string.game_menu_deck_one)
        }
        view.findViewById<TextView>(R.id.menu_common_deck_count_text).text =
            getString(R.string.game_menu_deck_count_with_value, deckLabel)

        val recyclesLabel = view.findViewById<TextView>(R.id.menu_common_waste_recycles_text)
        recyclesLabel.text = if (currentInfiniteRecycles) {
            getString(R.string.game_menu_waste_recycles_with_value,
                getString(R.string.settings_recycle_unlimited))
        } else {
            getString(R.string.game_menu_waste_recycles_with_value, currentRecycleCount.toString())
        }

        val stateEnabled = getString(R.string.setting_state_enabled)
        val stateDisabled = getString(R.string.setting_state_disabled)
        val stateOn = getString(R.string.setting_state_on)
        val stateOff = getString(R.string.setting_state_off)
        view.findViewById<TextView>(R.id.menu_settings_sound_text).text = getString(
            R.string.game_menu_sound_with_value,
            if (currentSoundOn) stateOn else stateOff
        )
        view.findViewById<TextView>(R.id.menu_advanced_show_timer_text).text = getString(
            R.string.game_menu_show_game_timer_with_value,
            if (currentShowGameTimer) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_show_score_text).text = getString(
            R.string.game_menu_show_score_with_value,
            if (currentShowScore) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_show_moves_text).text = getString(
            R.string.game_menu_show_moves_with_value,
            if (currentShowMoves) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_show_card_animations_text).text = getString(
            R.string.game_menu_show_card_movement_animations_with_value,
            if (currentShowCardAnimations) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_auto_complete_text).text = getString(
            R.string.game_menu_auto_complete_with_value,
            if (currentAutoComplete) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_haptics_text).text = getString(
            R.string.game_menu_haptics_with_value,
            if (currentHaptics) stateOn else stateOff
        )
        view.findViewById<TextView>(R.id.menu_advanced_tap_to_move_text).text = getString(
            R.string.game_menu_tap_to_move_with_value,
            if (currentTapToMove) stateEnabled else stateDisabled
        )
        val scoreMethodLabel = when (currentScoreMethod) {
            "vegas"            -> getString(R.string.score_method_vegas)
            "vegas_cumulative" -> getString(R.string.score_method_vegas_cumulative)
            "completion"       -> getString(R.string.score_method_completion)
            else               -> getString(R.string.score_method_windows)
        }
        view.findViewById<TextView>(R.id.menu_advanced_score_method_text).text = getString(
            R.string.game_menu_score_method_with_value, scoreMethodLabel
        )
        view.findViewById<TextView>(R.id.menu_advanced_foundation_to_tableau_text).text = getString(
            R.string.game_menu_foundation_to_tableau_with_value,
            if (currentFoundationToTableau) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_enforce_foundation_balance_text).text = getString(
            R.string.game_menu_enforce_foundation_balance_with_value,
            if (currentEnforceFoundationBalance) stateEnabled else stateDisabled
        )

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

        setSectionExpanded(statisticsContent, statisticsArrow, expandState.statisticsExpanded)
        setSectionExpanded(informationContent, informationArrow, expandState.informationExpanded)
        setSectionExpanded(settingsContent, settingsArrow, expandState.settingsExpanded)

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
            expandState = expandState.copy(settingsExpanded = expanded)
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
        view.findViewById<View>(R.id.menu_share_app_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShareApp() }
        }
        view.findViewById<View>(R.id.menu_contact_us_row).setOnClickListener {
            dismissAndRun { host.onGameMenuContactUs() }
        }
        view.findViewById<View>(R.id.menu_privacy_policy_row).setOnClickListener {
            dismissAndRun { host.onGameMenuOpenPrivacyPolicy() }
        }
        view.findViewById<View>(R.id.menu_terms_of_service_row).setOnClickListener {
            dismissAndRun { host.onGameMenuOpenTermsOfService() }
        }
        view.findViewById<View>(R.id.menu_common_nickname_row).setOnClickListener {
            dismissAndRun { host.onGameMenuEditNickname() }
        }
        view.findViewById<View>(R.id.menu_common_draw_cards_row).setOnClickListener {
            dismissAndRun { host.onGameMenuDrawCards() }
        }
        view.findViewById<View>(R.id.menu_common_deck_count_row).setOnClickListener {
            dismissAndRun { host.onGameMenuDeckCount() }
        }
        view.findViewById<View>(R.id.menu_common_waste_recycles_row).setOnClickListener {
            dismissAndRun { host.onGameMenuWasteRecycles() }
        }
        view.findViewById<View>(R.id.menu_settings_sound_row).setOnClickListener {
            dismissAndRun { host.onGameMenuSoundToggle() }
        }
        view.findViewById<View>(R.id.menu_about_row).setOnClickListener {
            dismissAndRun { host.onGameMenuOpenAbout() }
        }

        view.findViewById<View>(R.id.menu_advanced_show_timer_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShowGameTimerToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_show_score_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShowScoreToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_show_moves_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShowMovesToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_show_card_animations_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShowCardAnimationsToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_auto_complete_row).setOnClickListener {
            dismissAndRun { host.onGameMenuAutoCompleteToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_haptics_row).setOnClickListener {
            dismissAndRun { host.onGameMenuHapticsToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_tap_to_move_row).setOnClickListener {
            dismissAndRun { host.onGameMenuTapToMoveToggle() }
        }

        // Board layout popup
        view.findViewById<View>(R.id.menu_advanced_board_layout_row).setOnClickListener {
            dismissAndRun { host.onGameMenuBoardLayout() }
        }

        // Score method popup
        view.findViewById<View>(R.id.menu_advanced_score_method_row).setOnClickListener {
            dismissAndRun { host.onGameMenuScoreMethod() }
        }

        // Foundation to tableau toggle
        view.findViewById<View>(R.id.menu_advanced_foundation_to_tableau_row).setOnClickListener {
            dismissAndRun { host.onGameMenuFoundationToTableauToggle() }
        }
        view.findViewById<View>(R.id.menu_advanced_enforce_foundation_balance_row).setOnClickListener {
            dismissAndRun { host.onGameMenuEnforceFoundationBalanceToggle() }
        }

        // Premium Acct toggle is now in the Testers Menu (btn_testers on the game board).

        // Remaining advanced items that open settings
        val settingsRows = intArrayOf()
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
        dialog?.window?.let { window ->
            val heightPx = (resources.displayMetrics.heightPixels * 0.9).toInt()
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
        }
    }

    private fun dismissAndRun(action: () -> Unit) {
        dismiss()
        activity?.window?.decorView?.post(action) ?: action()
    }

    private fun setSectionExpanded(content: View, arrow: TextView, expanded: Boolean) {
        content.visibility = if (expanded) View.VISIBLE else View.GONE
        arrow.text = if (expanded) "▴" else "▾"
    }

    private fun formatNicknameForMenu(nickname: String): String {
        val trimmed = nickname.trim()
        if (trimmed.length <= MAX_MENU_NICKNAME_LENGTH) return trimmed
        return trimmed.take(MAX_MENU_NICKNAME_LENGTH) + "..."
    }

    private fun readExpandStateFromArgs(): ExpandState {
        val b = arguments ?: return ExpandState()
        return ExpandState(
            statisticsExpanded = b.getBoolean(ARG_STATISTICS_EXPANDED, false),
            informationExpanded = b.getBoolean(ARG_INFORMATION_EXPANDED, false),
            settingsExpanded = b.getBoolean(ARG_SETTINGS_EXPANDED, false)
        )
    }

    companion object {
        const val TAG = "game_menu_bottom_sheet"
        private const val ARG_STATISTICS_EXPANDED = "arg_statistics_expanded"
        private const val ARG_INFORMATION_EXPANDED = "arg_information_expanded"
        private const val ARG_SETTINGS_EXPANDED = "arg_settings_expanded"
        private const val ARG_CURRENT_NICKNAME = "arg_current_nickname"
        private const val ARG_CURRENT_DRAW_SIZE = "arg_current_draw_size"
        private const val ARG_CURRENT_DECK_COUNT = "arg_current_deck_count"
        private const val ARG_CURRENT_INFINITE_RECYCLES = "arg_current_infinite_recycles"
        private const val ARG_CURRENT_RECYCLE_COUNT = "arg_current_recycle_count"
        private const val ARG_CURRENT_SOUND_ON = "arg_current_sound_on"
        private const val ARG_CURRENT_SHOW_GAME_TIMER = "arg_current_show_game_timer"
        private const val ARG_CURRENT_SHOW_SCORE = "arg_current_show_score"
        private const val ARG_CURRENT_SHOW_MOVES = "arg_current_show_moves"
        private const val ARG_CURRENT_SHOW_CARD_ANIMATIONS = "arg_current_show_card_animations"
        private const val ARG_CURRENT_AUTO_COMPLETE = "arg_current_auto_complete"
        private const val ARG_CURRENT_HAPTICS = "arg_current_haptics"
        private const val ARG_CURRENT_TAP_TO_MOVE = "arg_current_tap_to_move"
        private const val ARG_CURRENT_SCORE_METHOD = "arg_current_score_method"
        private const val ARG_CURRENT_FOUNDATION_TO_TABLEAU = "arg_current_foundation_to_tableau"
        private const val ARG_CURRENT_ENFORCE_FOUNDATION_BALANCE = "arg_current_enforce_foundation_balance"
        private const val MAX_MENU_NICKNAME_LENGTH = 20
        private const val DEFAULT_DRAW_SIZE = 3
        private const val DEFAULT_DECK_COUNT = 1
        private const val DEFAULT_RECYCLE_COUNT = 3

        fun newInstance(
            state: ExpandState = ExpandState(),
            currentNickname: String = "",
            currentDrawSize: Int = DEFAULT_DRAW_SIZE,
            currentDeckCount: Int = DEFAULT_DECK_COUNT,
            currentInfiniteRecycles: Boolean = true,
            currentRecycleCount: Int = DEFAULT_RECYCLE_COUNT,
            currentSoundOn: Boolean = true,
            currentShowGameTimer: Boolean = true,
            currentShowScore: Boolean = true,
            currentShowMoves: Boolean = true,
            currentShowCardAnimations: Boolean = true,
            currentAutoComplete: Boolean = true,
            currentHaptics: Boolean = false,
            currentTapToMove: Boolean = true,
            currentScoreMethod: String = "windows",
            currentFoundationToTableau: Boolean = false,
            currentEnforceFoundationBalance: Boolean = false
        ): GameMenuBottomSheetFragment {
            return GameMenuBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    // ...existing puts...
                    putBoolean(ARG_STATISTICS_EXPANDED, state.statisticsExpanded)
                    putBoolean(ARG_INFORMATION_EXPANDED, state.informationExpanded)
                    putBoolean(ARG_SETTINGS_EXPANDED, state.settingsExpanded)
                    putString(ARG_CURRENT_NICKNAME, currentNickname)
                    putInt(ARG_CURRENT_DRAW_SIZE, currentDrawSize)
                    putInt(ARG_CURRENT_DECK_COUNT, currentDeckCount)
                    putBoolean(ARG_CURRENT_INFINITE_RECYCLES, currentInfiniteRecycles)
                    putInt(ARG_CURRENT_RECYCLE_COUNT, currentRecycleCount)
                    putBoolean(ARG_CURRENT_SOUND_ON, currentSoundOn)
                    putBoolean(ARG_CURRENT_SHOW_GAME_TIMER, currentShowGameTimer)
                    putBoolean(ARG_CURRENT_SHOW_SCORE, currentShowScore)
                    putBoolean(ARG_CURRENT_SHOW_MOVES, currentShowMoves)
                    putBoolean(ARG_CURRENT_SHOW_CARD_ANIMATIONS, currentShowCardAnimations)
                    putBoolean(ARG_CURRENT_AUTO_COMPLETE, currentAutoComplete)
                    putBoolean(ARG_CURRENT_HAPTICS, currentHaptics)
                    putBoolean(ARG_CURRENT_TAP_TO_MOVE, currentTapToMove)
                    putString(ARG_CURRENT_SCORE_METHOD, currentScoreMethod)
                    putBoolean(ARG_CURRENT_FOUNDATION_TO_TABLEAU, currentFoundationToTableau)
                    putBoolean(ARG_CURRENT_ENFORCE_FOUNDATION_BALANCE, currentEnforceFoundationBalance)
                }
            }
        }
    }
}




































