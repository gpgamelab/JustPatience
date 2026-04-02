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
        fun onGameMenuShareApp()
        fun onGameMenuContactUs()
        fun onGameMenuOpenPrivacyPolicy()
        fun onGameMenuOpenTermsOfService()
        fun onGameMenuEditNickname()
        fun onGameMenuDrawCards()
        fun onGameMenuWasteRecycles()
        fun onGameMenuMuteMusicToggle()
        fun onGameMenuMuteCardSoundsToggle()
        fun onGameMenuMuteWinSoundToggle()
        fun onGameMenuShowGameTimerToggle()
        fun onGameMenuShowScoreToggle()
        fun onGameMenuShowMovesToggle()
        fun onGameMenuShowCardAnimationsToggle()
        fun onGameMenuShowWinAnimationToggle()
        fun onGameMenuAutoCompleteToggle()
        fun onGameMenuHapticsToggle()
        fun onGameMenuTapToMoveToggle()
        fun onGameMenuFullScreenToggle()
        fun onGameMenuBoardLayout()
        fun onGameMenuScoreMethod()
        fun onGameMenuFoundationToTableauToggle()
        fun onGameMenuPremiumAcctToggle()
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
        val currentNickname = arguments?.getString(ARG_CURRENT_NICKNAME).orEmpty().trim()
        val currentDrawSize = arguments?.getInt(ARG_CURRENT_DRAW_SIZE, DEFAULT_DRAW_SIZE) ?: DEFAULT_DRAW_SIZE
        val currentInfiniteRecycles = arguments?.getBoolean(ARG_CURRENT_INFINITE_RECYCLES, true) ?: true
        val currentRecycleCount = arguments?.getInt(ARG_CURRENT_RECYCLE_COUNT, DEFAULT_RECYCLE_COUNT) ?: DEFAULT_RECYCLE_COUNT
        val currentMuteMusic = arguments?.getBoolean(ARG_CURRENT_MUTE_MUSIC, false) ?: false
        val currentMuteCardSounds = arguments?.getBoolean(ARG_CURRENT_MUTE_CARD_SOUNDS, false) ?: false
        val currentMuteWinSound = arguments?.getBoolean(ARG_CURRENT_MUTE_WIN_SOUND, false) ?: false
        val currentShowGameTimer = arguments?.getBoolean(ARG_CURRENT_SHOW_GAME_TIMER, true) ?: true
        val currentShowScore = arguments?.getBoolean(ARG_CURRENT_SHOW_SCORE, true) ?: true
        val currentShowMoves = arguments?.getBoolean(ARG_CURRENT_SHOW_MOVES, true) ?: true
        val currentShowCardAnimations = arguments?.getBoolean(ARG_CURRENT_SHOW_CARD_ANIMATIONS, true) ?: true
        val currentShowWinAnimation = arguments?.getBoolean(ARG_CURRENT_SHOW_WIN_ANIMATION, true) ?: true
        val currentAutoComplete = arguments?.getBoolean(ARG_CURRENT_AUTO_COMPLETE, true) ?: true
        val currentHaptics = arguments?.getBoolean(ARG_CURRENT_HAPTICS, false) ?: false
        val currentTapToMove = arguments?.getBoolean(ARG_CURRENT_TAP_TO_MOVE, true) ?: true
        val currentFullScreen = arguments?.getBoolean(ARG_CURRENT_FULL_SCREEN, false) ?: false
        val currentScoreMethod = arguments?.getString(ARG_CURRENT_SCORE_METHOD) ?: "windows"
        val currentFoundationToTableau = arguments?.getBoolean(ARG_CURRENT_FOUNDATION_TO_TABLEAU, false) ?: false
        val currentPremiumAcct = arguments?.getBoolean(ARG_CURRENT_PREMIUM_ACCT, false) ?: false
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
        val drawCardsLabel = view.findViewById<TextView>(R.id.menu_common_draw_cards_text)
        drawCardsLabel.text = getString(R.string.game_menu_draw_cards_with_value, normalizedDrawSize)

        val recyclesLabel = view.findViewById<TextView>(R.id.menu_common_waste_recycles_text)
        recyclesLabel.text = if (currentInfiniteRecycles) {
            getString(R.string.game_menu_waste_recycles_with_value,
                getString(R.string.settings_recycle_unlimited))
        } else {
            getString(R.string.game_menu_waste_recycles_with_value, currentRecycleCount.toString())
        }

        val stateEnabled = getString(R.string.setting_state_enabled)
        val stateDisabled = getString(R.string.setting_state_disabled)
        view.findViewById<TextView>(R.id.menu_common_mute_music_text).text = getString(
            R.string.game_menu_mute_game_music_with_value,
            if (currentMuteMusic) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_common_mute_card_sounds_text).text = getString(
            R.string.game_menu_mute_card_movement_sounds_with_value,
            if (currentMuteCardSounds) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_common_mute_win_sound_text).text = getString(
            R.string.game_menu_mute_win_sound_with_value,
            if (currentMuteWinSound) stateEnabled else stateDisabled
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
        view.findViewById<TextView>(R.id.menu_advanced_show_win_animation_text).text = getString(
            R.string.game_menu_show_win_animation_with_value,
            if (currentShowWinAnimation) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_auto_complete_text).text = getString(
            R.string.game_menu_auto_complete_with_value,
            if (currentAutoComplete) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_haptics_text).text = getString(
            R.string.game_menu_haptics_with_value,
            if (currentHaptics) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_tap_to_move_text).text = getString(
            R.string.game_menu_tap_to_move_with_value,
            if (currentTapToMove) stateEnabled else stateDisabled
        )
        view.findViewById<TextView>(R.id.menu_advanced_full_screen_text).text = getString(
            R.string.game_menu_full_screen_with_value,
            if (currentFullScreen) stateEnabled else stateDisabled
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
        view.findViewById<TextView>(R.id.menu_advanced_premium_acct_text).text = getString(
            R.string.game_menu_premium_acct_with_value,
            if (currentPremiumAcct) stateEnabled else stateDisabled
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
        view.findViewById<View>(R.id.menu_common_waste_recycles_row).setOnClickListener {
            dismissAndRun { host.onGameMenuWasteRecycles() }
        }
        view.findViewById<View>(R.id.menu_common_mute_music_row).setOnClickListener {
            dismissAndRun { host.onGameMenuMuteMusicToggle() }
        }
        view.findViewById<View>(R.id.menu_common_mute_card_sounds_row).setOnClickListener {
            dismissAndRun { host.onGameMenuMuteCardSoundsToggle() }
        }
        view.findViewById<View>(R.id.menu_common_mute_win_sound_row).setOnClickListener {
            dismissAndRun { host.onGameMenuMuteWinSoundToggle() }
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
        view.findViewById<View>(R.id.menu_advanced_show_win_animation_row).setOnClickListener {
            dismissAndRun { host.onGameMenuShowWinAnimationToggle() }
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
        view.findViewById<View>(R.id.menu_advanced_full_screen_row).setOnClickListener {
            dismissAndRun { host.onGameMenuFullScreenToggle() }
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

        // Premium Acct toggle
        view.findViewById<View>(R.id.menu_advanced_premium_acct_row).setOnClickListener {
            dismissAndRun { host.onGameMenuPremiumAcctToggle() }
        }

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
        private const val ARG_CURRENT_NICKNAME = "arg_current_nickname"
        private const val ARG_CURRENT_DRAW_SIZE = "arg_current_draw_size"
        private const val ARG_CURRENT_INFINITE_RECYCLES = "arg_current_infinite_recycles"
        private const val ARG_CURRENT_RECYCLE_COUNT = "arg_current_recycle_count"
        private const val ARG_CURRENT_MUTE_MUSIC = "arg_current_mute_music"
        private const val ARG_CURRENT_MUTE_CARD_SOUNDS = "arg_current_mute_card_sounds"
        private const val ARG_CURRENT_MUTE_WIN_SOUND = "arg_current_mute_win_sound"
        private const val ARG_CURRENT_SHOW_GAME_TIMER = "arg_current_show_game_timer"
        private const val ARG_CURRENT_SHOW_SCORE = "arg_current_show_score"
        private const val ARG_CURRENT_SHOW_MOVES = "arg_current_show_moves"
        private const val ARG_CURRENT_SHOW_CARD_ANIMATIONS = "arg_current_show_card_animations"
        private const val ARG_CURRENT_SHOW_WIN_ANIMATION = "arg_current_show_win_animation"
        private const val ARG_CURRENT_AUTO_COMPLETE = "arg_current_auto_complete"
        private const val ARG_CURRENT_HAPTICS = "arg_current_haptics"
        private const val ARG_CURRENT_TAP_TO_MOVE = "arg_current_tap_to_move"
        private const val ARG_CURRENT_FULL_SCREEN = "arg_current_full_screen"
        private const val ARG_CURRENT_SCORE_METHOD = "arg_current_score_method"
        private const val ARG_CURRENT_FOUNDATION_TO_TABLEAU = "arg_current_foundation_to_tableau"
        private const val ARG_CURRENT_PREMIUM_ACCT = "arg_current_premium_acct"
        private const val MAX_MENU_NICKNAME_LENGTH = 20
        private const val DEFAULT_DRAW_SIZE = 3
        private const val DEFAULT_RECYCLE_COUNT = 3

        fun newInstance(
            state: ExpandState = ExpandState(),
            currentNickname: String = "",
            currentDrawSize: Int = DEFAULT_DRAW_SIZE,
            currentInfiniteRecycles: Boolean = true,
            currentRecycleCount: Int = DEFAULT_RECYCLE_COUNT,
            currentMuteMusic: Boolean = false,
            currentMuteCardSounds: Boolean = false,
            currentMuteWinSound: Boolean = false,
            currentShowGameTimer: Boolean = true,
            currentShowScore: Boolean = true,
            currentShowMoves: Boolean = true,
            currentShowCardAnimations: Boolean = true,
            currentShowWinAnimation: Boolean = true,
            currentAutoComplete: Boolean = true,
            currentHaptics: Boolean = false,
            currentTapToMove: Boolean = true,
            currentFullScreen: Boolean = false,
            currentScoreMethod: String = "windows",
            currentFoundationToTableau: Boolean = false,
            currentPremiumAcct: Boolean = false
        ): GameMenuBottomSheetFragment {
            return GameMenuBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    // ...existing puts...
                    putBoolean(ARG_STATISTICS_EXPANDED, state.statisticsExpanded)
                    putBoolean(ARG_INFORMATION_EXPANDED, state.informationExpanded)
                    putBoolean(ARG_SETTINGS_EXPANDED, state.settingsExpanded)
                    putBoolean(ARG_COMMON_EXPANDED, state.commonExpanded)
                    putBoolean(ARG_ADVANCED_EXPANDED, state.advancedExpanded)
                    putString(ARG_CURRENT_NICKNAME, currentNickname)
                    putInt(ARG_CURRENT_DRAW_SIZE, currentDrawSize)
                    putBoolean(ARG_CURRENT_INFINITE_RECYCLES, currentInfiniteRecycles)
                    putInt(ARG_CURRENT_RECYCLE_COUNT, currentRecycleCount)
                    putBoolean(ARG_CURRENT_MUTE_MUSIC, currentMuteMusic)
                    putBoolean(ARG_CURRENT_MUTE_CARD_SOUNDS, currentMuteCardSounds)
                    putBoolean(ARG_CURRENT_MUTE_WIN_SOUND, currentMuteWinSound)
                    putBoolean(ARG_CURRENT_SHOW_GAME_TIMER, currentShowGameTimer)
                    putBoolean(ARG_CURRENT_SHOW_SCORE, currentShowScore)
                    putBoolean(ARG_CURRENT_SHOW_MOVES, currentShowMoves)
                    putBoolean(ARG_CURRENT_SHOW_CARD_ANIMATIONS, currentShowCardAnimations)
                    putBoolean(ARG_CURRENT_SHOW_WIN_ANIMATION, currentShowWinAnimation)
                    putBoolean(ARG_CURRENT_AUTO_COMPLETE, currentAutoComplete)
                    putBoolean(ARG_CURRENT_HAPTICS, currentHaptics)
                    putBoolean(ARG_CURRENT_TAP_TO_MOVE, currentTapToMove)
                    putBoolean(ARG_CURRENT_FULL_SCREEN, currentFullScreen)
                    putString(ARG_CURRENT_SCORE_METHOD, currentScoreMethod)
                    putBoolean(ARG_CURRENT_FOUNDATION_TO_TABLEAU, currentFoundationToTableau)
                    putBoolean(ARG_CURRENT_PREMIUM_ACCT, currentPremiumAcct)
                }
            }
        }
    }
}


































