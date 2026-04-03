package com.gpgamelab.justpatience.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.gpgamelab.justpatience.BuildConfig
import com.gpgamelab.justpatience.ads.AdManager
import com.gpgamelab.justpatience.assets.AndroidAssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.SettingsActivity
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.util.UiScaleUtil
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlin.math.sqrt

class GameActivity : AppCompatActivity(), GameMenuBottomSheetFragment.Host {

    private data class WinPopupUiConfig(
        val dialogWidthPercentLandscape: Float = 0.40f,
        val dialogWidthPercentPortrait: Float = 0.95f,
        val dialogHeightPercent: Float = 0.80f,
        val rewardTopPercent: Float = 0.64f,
        val rewardBottomPercent: Float = rewardTopPercent + 0.1f,
        val buttonsTopPercent: Float = 0.82f,
        val buttonsBottomPercent: Float = buttonsTopPercent + 0.1f,
        val continueButtonWidthPercent: Float = 0.30f,
        val multiplierButtonWidthPercent: Float = 0.30f,
        val buttonGapDp: Float = 18f,
        val rewardAmountTextSp: Float = 44f,
        val buttonTextSpLandscape: Float = 16f,
        val buttonTextSpPortrait: Float = 30f,
        val rewardAmountToGemGapDp: Float = 40f,
        val rewardRowMinWidthDp: Float = 160f,
        val rewardRowMaxWidthDp: Float = 360f,
        val buttonHorizontalPaddingDp: Float = 30f
    )

    // Single tweak cluster for the custom win popup. Adjust these values instead of
    // searching through the XML when you want to nudge positions/sizes.
    private val winPopupUiConfig = WinPopupUiConfig()

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // Optional helper UI manager (keeps minimal behavior; extend as needed)
    private lateinit var uiManager: CardStackUIManager

    // Ad management
    private lateinit var adManager: AdManager
    private lateinit var statsManager: GameStatsManager
    private lateinit var settingsManager: SettingsManager

    // Non-premium users must unlock the grouped move controls once per game via ad.
    private var isControlGroupAdUnlocked = false
    private var isControlGroupAdFlowInProgress = false

    private var winDialogShowing: Boolean = false
    private var winCelebrationPlayed: Boolean = false
    private var showWinAnimation: Boolean = true
    private var isPremiumAccount: Boolean = false
    private var forceNewGameOnLaunch: Boolean = false
    private var gameMenuExpandState = GameMenuBottomSheetFragment.ExpandState()
    private var pendingWinUiAfterAnimation: Boolean = false
    private var gemTotal: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Scale the bottom control-button row (and info panel) vertically using the
        // raw baseline factor – no extreme-aspect correction – so portrait phones get
        // slightly taller touch targets while landscape phones stay compact.
        UiScaleUtil.applyScreenVerticalScale(binding.controlButtonsLayout, this)
        UiScaleUtil.applyScreenVerticalScale(binding.gameInfoPanel, this)

        // Wire viewModel into GameBoardView
        binding.gameBoardView.viewModel = viewModel
        
        // Wire GameBoardView back into viewModel for animation scheduling
        viewModel.gameBoardView = binding.gameBoardView

        // Wire AssetResolver into GameBoardView
        binding.gameBoardView.assetResolver = AndroidAssetResolver(this)
        binding.gameBoardView.bindToViewModel(this)

        // Optional manager (no heavy rendering here)
        uiManager = CardStackUIManager(this, binding.root, viewModel)

        // Initialize and load banner ads
        adManager = AdManager(this)
        adManager.initializeAds()
        adManager.loadBannerAd(binding.adView)
        adManager.loadRewardedAd()
        adManager.loadRewardedInterstitialAd()

        statsManager = GameStatsManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)
        renderGemHud(gemTotal)

        // Launcher starts directly in GameActivity; default behavior is resume-or-new.
        forceNewGameOnLaunch = intent.getBooleanExtra("force_new_game", false)
        viewModel.initializeForLaunch(forceNewGameOnLaunch)

        updateOverlayVisibility()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    settingsManager.gamePlaySettingsFlow.collect { settings ->
                        isPremiumAccount = settings.premiumAcct
                        if (isPremiumAccount) {
                            isControlGroupAdUnlocked = true
                        }
                        updateOverlayVisibility()
                    }
                }

                launch {
                    settingsManager.getTotalGemsFlow().collect { persistedTotal ->
                        gemTotal = persistedTotal
                        renderGemHud(gemTotal)
                    }
                }

                launch {
                    viewModel.game.collect { g ->
                        binding.tvScore.text = getString(R.string.score_format, g.score)
                        binding.tvMoves.text = getString(R.string.moves_format, g.moves)

                        if (g.status != GameStatus.WON) {
                            winCelebrationPlayed = false
                            pendingWinUiAfterAnimation = false
                        } else {
                            showWinCelebrationThenDialog()
                        }
                    }
                }

                launch {
                    viewModel.gameTime.collect { seconds ->
                        binding.tvTime.text = formatTime(seconds)
                    }
                }

                launch {
                    viewModel.showGameTimer.collect { shouldShow ->
                        binding.tvTime.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showScore.collect { shouldShow ->
                        binding.tvScore.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showMoves.collect { shouldShow ->
                        binding.tvMoves.visibility = if (shouldShow) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.showWinAnimation.collect { enabled ->
                        showWinAnimation = enabled
                    }
                }

                launch {
                    viewModel.canUndo.collect { canUndo ->
                        findViewById<ImageView>(R.id.undo_main)?.setImageResource(
                            if (canUndo) R.drawable.undo_red else R.drawable.undo_gray
                        )
                    }
                }

                launch {
                    viewModel.canRedo.collect { canRedo ->
                        findViewById<ImageView>(R.id.redo_main)?.setImageResource(
                            if (canRedo) R.drawable.redo_blue else R.drawable.redo_gray
                        )
                    }
                }
            }
        }

        // Simple button hookups (if present in layout)
        binding.btnUndo.setOnClickListener {
            runAfterControlGroupUnlock { handleUndoClick() }
        }
        binding.btnRedo.setOnClickListener {
            runAfterControlGroupUnlock { handleRedoClick() }
        }
        binding.btnNewGame.setOnClickListener {
            resetControlGroupAdRequirement()
            winCelebrationPlayed = false
            updateOverlayVisibility()
            viewModel.startNewGame()
        }
        findViewById<Button>(R.id.btn_restart).setOnClickListener {
            runAfterControlGroupUnlock { handleRestartClick() }
        }
        findViewById<Button>(R.id.btn_hint)?.setOnClickListener {
            runAfterControlGroupUnlock {
                if (!viewModel.showManualHints()) {
                    Toast.makeText(this@GameActivity, R.string.no_hints_available, Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.btnStats.setOnClickListener { showGameMenu() }
        findViewById<Button>(R.id.btn_auto_move)?.setOnClickListener { buttonView ->
            runAfterControlGroupUnlock {
                buttonView.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val movesMade = viewModel.performAutoMove()
                        if (movesMade == 0) {
                            Toast.makeText(this@GameActivity, "No moves available", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        buttonView.isEnabled = true
                    }
                }
            }
        }

        applyResponsiveControlSizing()
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    private fun renderGemHud(total: Int) {
        val safeTotal = total.coerceAtLeast(0)
        binding.ivGemBag.setImageResource(
            if (safeTotal == 0) R.drawable.gems_empty_bag_1536x1536
            else R.drawable.gems_purple_bag_1536x1536
        )
        binding.tvGemCount.text = safeTotal.toString()
        binding.ivGemBag.contentDescription = getString(R.string.gems_count_content_description, safeTotal)
    }


    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_game_title)
            .setMessage(R.string.restart_game_message)
            .setPositiveButton(R.string.restart_game_text) { _, _ -> viewModel.restartGame() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGameEndDialog(isWin: Boolean) {
        if (!isWin || isFinishing || isDestroyed || winDialogShowing) return

        winDialogShowing = true
        val baseReward = 10

        if (isPremiumAccount) {
            showWinRewardChoiceDialog(baseReward)
            return
        }

        val shown = adManager.showRewardedInterstitialAd(
            onCompleted = { showWinRewardChoiceDialog(baseReward) }
        )

        if (!shown) {
            adManager.loadRewardedInterstitialAd()
            showWinRewardChoiceDialog(baseReward)
        }
    }

    private fun showWinRewardChoiceDialog(baseReward: Int) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showWinRewardChoiceDialog(baseReward) }
            return
        }

        if (isFinishing || isDestroyed) {
            winDialogShowing = false
            return
        }

        val adMultiplier = if (isPremiumAccount) 5 else 2

        val dialogView = layoutInflater.inflate(R.layout.dialog_win_reward_choice, null)
        val rewardAmount = dialogView.findViewById<TextView>(R.id.tv_main_reward_amount)
        val continueButton = dialogView.findViewById<Button>(R.id.btn_win_continue)
        val multiplierButton = dialogView.findViewById<Button>(R.id.btn_win_multiplier)

        applyWinPopupUiConfig(dialogView, winPopupUiConfig)

        rewardAmount.text = getString(R.string.win_reward_popup_amount, baseReward)
        continueButton.text = getString(R.string.win_reward_popup_continue)
        multiplierButton.text = getString(R.string.win_reward_popup_multiplier, adMultiplier)

        val dialog = Dialog(this).apply {
            setContentView(dialogView)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        continueButton.setOnClickListener {
            dialog.dismiss()
            completeWinRewardFlow(baseReward)
        }
        multiplierButton.setOnClickListener {
            dialog.dismiss()
            showWinMultiplierRewardAd(baseReward, adMultiplier)
        }

        dialog.setOnShowListener {
            val widthPercent = getWinPopupWidthPercent(winPopupUiConfig)
            val widthPx = (resources.displayMetrics.widthPixels * widthPercent).toInt().coerceAtLeast(1)
            val heightPx = (resources.displayMetrics.heightPixels * winPopupUiConfig.dialogHeightPercent).toInt().coerceAtLeast(1)
            dialog.window?.setLayout(widthPx, heightPx)
            dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
        dialog.setOnDismissListener { winDialogShowing = false }
        dialog.show()
    }

    private fun applyWinPopupUiConfig(dialogView: View, config: WinPopupUiConfig) {
        setGuidelinePercent(dialogView, R.id.guideline_reward_top, config.rewardTopPercent)
        setGuidelinePercent(dialogView, R.id.guideline_reward_bottom, config.rewardBottomPercent)
        setGuidelinePercent(dialogView, R.id.guideline_buttons_top, config.buttonsTopPercent)
        setGuidelinePercent(dialogView, R.id.guideline_buttons_bottom, config.buttonsBottomPercent)

        dialogView.findViewById<TextView>(R.id.tv_main_reward_amount)?.let { rewardText ->
            rewardText.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.rewardAmountTextSp)
            (rewardText.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = dpToPx(config.rewardAmountToGemGapDp)
                rewardText.layoutParams = lp
            }
        }

        dialogView.findViewById<LinearLayout>(R.id.layout_reward_row)?.let { rewardRow ->
            (rewardRow.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
                lp.matchConstraintMinWidth = dpToPx(config.rewardRowMinWidthDp)
                lp.matchConstraintMaxWidth = dpToPx(config.rewardRowMaxWidthDp)
                rewardRow.layoutParams = lp
            }
        }

        val widthPercent = getWinPopupWidthPercent(config)
        val popupWidthPx = (resources.displayMetrics.widthPixels * widthPercent).toInt().coerceAtLeast(1)
        val continueButtonWidthPx = (popupWidthPx * config.continueButtonWidthPercent).toInt().coerceAtLeast(1)
        val multiplierButtonWidthPx = (popupWidthPx * config.multiplierButtonWidthPercent).toInt().coerceAtLeast(1)
        val buttonGapPx = dpToPx(config.buttonGapDp)

        dialogView.findViewById<Button>(R.id.btn_win_continue)?.let { button ->
            applyWinPopupButtonConfig(button, continueButtonWidthPx, config, endMarginPx = buttonGapPx)
        }
        dialogView.findViewById<Button>(R.id.btn_win_multiplier)?.let { button ->
            applyWinPopupButtonConfig(button, multiplierButtonWidthPx, config, endMarginPx = 0)
        }
    }

    private fun setGuidelinePercent(root: View, guidelineId: Int, percent: Float) {
        val guideline = root.findViewById<View>(guidelineId) ?: return
        (guideline.layoutParams as? ConstraintLayout.LayoutParams)?.let { lp ->
            lp.guidePercent = percent.coerceIn(0f, 1f)
            guideline.layoutParams = lp
        }
    }

    private fun getWinPopupWidthPercent(config: WinPopupUiConfig): Float {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            config.dialogWidthPercentLandscape
        } else {
            config.dialogWidthPercentPortrait
        }
    }

    private fun getWinPopupButtonTextSp(config: WinPopupUiConfig): Float {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            config.buttonTextSpLandscape
        } else {
            config.buttonTextSpPortrait
        }
    }

    private fun applyWinPopupButtonConfig(button: Button, widthPx: Int, config: WinPopupUiConfig, endMarginPx: Int) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, getWinPopupButtonTextSp(config))
        val horizontalPadding = dpToPx(config.buttonHorizontalPaddingDp)
        button.setPaddingRelative(horizontalPadding, button.paddingTop, horizontalPadding, button.paddingBottom)
        (button.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            lp.width = widthPx
            lp.marginEnd = endMarginPx
            button.layoutParams = lp
        }
    }

    private fun showWinMultiplierRewardAd(baseReward: Int, multiplier: Int) {
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                val gemsToAward = if (rewardEarned) baseReward * multiplier else baseReward
                completeWinRewardFlow(gemsToAward)
            }
        )

        if (!shown) {
            Toast.makeText(this, R.string.optional_ad_not_ready, Toast.LENGTH_SHORT).show()
            completeWinRewardFlow(baseReward)
            adManager.loadRewardedAd()
        }
    }

    private fun awardGems(amount: Int) {
        gemTotal = (gemTotal + amount).coerceAtLeast(0)
        renderGemHud(gemTotal)
        lifecycleScope.launch {
            settingsManager.setTotalGems(gemTotal)
        }
    }

    private fun completeWinRewardFlow(gemsToAward: Int) {
        awardGems(gemsToAward)
        resetControlGroupAdRequirement()
        winCelebrationPlayed = false
        updateOverlayVisibility()
        viewModel.startNewGame()
    }

    private fun showStatsDialog() {
        StatsDialogFragment.newInstance().show(supportFragmentManager, "stats_dialog")
    }

    private fun showGameMenu() {
        lifecycleScope.launch {
            if (supportFragmentManager.findFragmentByTag(GameMenuBottomSheetFragment.TAG) != null) return@launch
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            GameMenuBottomSheetFragment.newInstance(
                state = gameMenuExpandState,
                currentNickname = currentSettings.playerDisplayName,
                currentDrawSize = currentSettings.drawSize,
                currentInfiniteRecycles = currentSettings.infiniteRecycles,
                currentRecycleCount = currentSettings.recycleCount,
                currentMuteMusic = currentSettings.muteMusic,
                currentMuteCardSounds = currentSettings.muteCardSound,
                currentMuteWinSound = currentSettings.muteWinSound,
                currentShowGameTimer = currentSettings.showGameTimer,
                currentShowScore = currentSettings.showScore,
                currentShowMoves = currentSettings.showMoves,
                currentShowCardAnimations = currentSettings.showCardAnimations,
                currentShowWinAnimation = currentSettings.showWinAnimation,
                currentAutoComplete = currentSettings.autoComplete,
                currentHaptics = currentSettings.haptics,
                currentTapToMove = currentSettings.tapToMove,
                currentFullScreen = currentSettings.fullScreen,
                currentScoreMethod = currentSettings.scoreMethod,
                currentFoundationToTableau = currentSettings.allowFoundationToTableauDrag,
                currentPremiumAcct = currentSettings.premiumAcct
            ).show(
                supportFragmentManager,
                GameMenuBottomSheetFragment.TAG
            )
        }
    }

    override fun onGameMenuStatisticsSummary() {
        StatsSummaryDialogFragment.newInstance().show(supportFragmentManager, "stats_summary_dialog")
    }

    override fun onGameMenuStatisticsHistory() {
        showStatsDialog()
    }

    override fun onGameMenuResetStats() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_reset_stats_title)
            .setMessage(R.string.confirm_reset_stats_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                lifecycleScope.launch {
                    statsManager.deleteAllGameRecords()
                    settingsManager.resetStats()
                    Toast.makeText(this@GameActivity, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onGameMenuOpenAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    override fun onGameMenuOpenHowToPlay() {
        startActivity(Intent(this, HowToPlayActivity::class.java))
    }

    override fun onGameMenuRateUs() {
        // FakeReviewManager is used in debug builds so the review sheet can be tested
        // without the app being installed from the Play Store.
        // In release builds the real manager is used; Google Play may silently suppress
        // the sheet (quota, already reviewed, etc.) — openPlayStoreListing() is the fallback.
        val manager = if (BuildConfig.DEBUG) {
            FakeReviewManager(this)
        } else {
            ReviewManagerFactory.create(this)
        }

        manager.requestReviewFlow()
            .addOnCompleteListener { task: com.google.android.gms.tasks.Task<ReviewInfo> ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result)
                        .addOnCompleteListener {
                            Log.d("GameActivity", "In-app review flow completed")
                        }
                } else {
                    Log.w("GameActivity", "In-app review unavailable, opening Play Store")
                    openPlayStoreListing()
                }
            }
    }

    override fun onGameMenuShareApp() {
        val appUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val shareText = getString(R.string.share_app_message_format, appUrl)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        if (shareIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, R.string.share_app_no_target, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_app_chooser_title)))
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.share_app_no_target, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGameMenuContactUs() {
        val supportEmail = getString(R.string.contact_us_email_address)
        val subject = getString(R.string.contact_us_subject)
        val body = buildContactUsBody()
        val emailUri = Uri.fromParts("mailto", supportEmail, null)
            .buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()
        val emailIntent = Intent(Intent.ACTION_SENDTO, emailUri)

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_us_chooser_title)))
            return
        }

        val fallbackUrl = getString(
            R.string.contact_us_web_fallback_url_format,
            Uri.encode(supportEmail),
            Uri.encode(subject),
            Uri.encode(body)
        )
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
        if (webIntent.resolveActivity(packageManager) != null) {
            startActivity(webIntent)
        } else {
            Toast.makeText(this, R.string.contact_us_no_target, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onGameMenuOpenTermsOfService() {
        val tosHtml = readRawResourceText(R.raw.terms_of_service_2026_03_27)
        val tosUrl = getString(R.string.terms_of_service_website_url)
        val linkBlock = """
            <p><b>${getString(R.string.terms_of_service_link_title)}</b><br>
            <a href="$tosUrl">$tosUrl</a></p>
        """.trimIndent()
        val tosText = HtmlCompat.fromHtml(tosHtml + linkBlock, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val textView = TextView(this).apply {
            text = tosText
            movementMethod = LinkMovementMethod.getInstance()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(40, 28, 40, 28)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }
        UiScaleUtil.applyBaselineScale(scrollView, this)

        AlertDialog.Builder(this)
            .setTitle(R.string.terms_of_service_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.terms_of_service_open_website) { _, _ ->
                openTermsOfServiceWebsite(tosUrl)
            }
            .show()
    }

    override fun onGameMenuEditNickname() {
        lifecycleScope.launch {
            val currentNickname = settingsManager.gamePlaySettingsFlow.first().playerDisplayName
            showNicknameDialog(currentNickname)
        }
    }

    override fun onGameMenuDrawCards() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showDrawCardsDialog(currentSettings.drawSize)
        }
    }

    override fun onGameMenuWasteRecycles() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showWasteRecyclesDialog(currentSettings.infiniteRecycles, currentSettings.recycleCount)
        }
    }

    override fun onGameMenuMuteMusicToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_game_music),
                enabled = currentSettings.muteMusic
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteMusic = enabled))
                }
            }
        }
    }

    override fun onGameMenuMuteCardSoundsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_card_movement_sounds),
                enabled = currentSettings.muteCardSound
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteCardSound = enabled))
                }
            }
        }
    }

    override fun onGameMenuMuteWinSoundToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_mute_win_sound),
                enabled = currentSettings.muteWinSound
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(muteWinSound = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowGameTimerToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_game_timer),
                enabled = currentSettings.showGameTimer
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showGameTimer = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowScoreToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_score),
                enabled = currentSettings.showScore
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showScore = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowMovesToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_moves),
                enabled = currentSettings.showMoves
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showMoves = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowCardAnimationsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_card_movement_animations),
                enabled = currentSettings.showCardAnimations
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showCardAnimations = enabled))
                }
            }
        }
    }

    override fun onGameMenuShowWinAnimationToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_show_win_animation),
                enabled = currentSettings.showWinAnimation
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(showWinAnimation = enabled))
                }
            }
        }
    }

    override fun onGameMenuAutoCompleteToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_auto_complete),
                enabled = currentSettings.autoComplete
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(autoComplete = enabled))
                }
            }
        }
    }

    override fun onGameMenuHapticsToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_haptics),
                enabled = currentSettings.haptics
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(haptics = enabled))
                }
            }
        }
    }

    override fun onGameMenuTapToMoveToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_tap_to_move),
                enabled = currentSettings.tapToMove
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(tapToMove = enabled))
                }
            }
        }
    }

    override fun onGameMenuFullScreenToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_full_screen),
                enabled = currentSettings.fullScreen
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(latest.copy(fullScreen = enabled))
                }
            }
        }
    }

    override fun onGameMenuBoardLayout() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showBoardLayoutDialog(currentSettings.boardLayout)
        }
    }

    override fun onGameMenuScoreMethod() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showScoreMethodDialog(currentSettings.scoreMethod)
        }
    }

    override fun onGameMenuFoundationToTableauToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_foundation_to_tableau),
                enabled = currentSettings.allowFoundationToTableauDrag
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        latest.copy(allowFoundationToTableauDrag = enabled)
                    )
                }
            }
        }
    }

    override fun onGameMenuPremiumAcctToggle() {
        lifecycleScope.launch {
            val currentSettings = settingsManager.gamePlaySettingsFlow.first()
            showEnableDisableDialog(
                title = getString(R.string.game_menu_premium_acct),
                enabled = currentSettings.premiumAcct
            ) { enabled ->
                lifecycleScope.launch {
                    val latest = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        latest.copy(premiumAcct = enabled)
                    )
                }
            }
        }
    }

    override fun onGameMenuOpenPrivacyPolicy() {
        val policyHtml = readRawResourceText(R.raw.privacy_policy_2026_03_17)
        val policyUrl = getString(R.string.privacy_policy_website_url)
        val linkBlock = """
            <p><b>${getString(R.string.privacy_policy_link_title)}</b><br>
            <a href="$policyUrl">$policyUrl</a></p>
        """.trimIndent()
        val policyText = HtmlCompat.fromHtml(policyHtml + linkBlock, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val textView = TextView(this).apply {
            text = policyText
            movementMethod = LinkMovementMethod.getInstance()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(40, 28, 40, 28)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
        }
        UiScaleUtil.applyBaselineScale(scrollView, this)

        AlertDialog.Builder(this)
            .setTitle(R.string.privacy_policy_dialog_title)
            .setView(scrollView)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.privacy_policy_open_website) { _, _ ->
                openPrivacyPolicyWebsite(policyUrl)
            }
            .show()
    }

    private fun buildContactUsBody(): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val androidVersion = "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val localeTag = resources.configuration.locales[0].toLanguageTag()

        return getString(
            R.string.contact_us_body_template,
            getString(R.string.app_name),
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            packageName,
            androidVersion,
            deviceName,
            localeTag
        )
    }

    private fun showNicknameDialog(currentNickname: String) {
        val nicknameInput = EditText(this).apply {
            hint = getString(R.string.nickname_dialog_hint)
            setText(currentNickname)
            setSelection(text.length)
            setSingleLine()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nickname_dialog_title)
            .setView(nicknameInput)
            .setPositiveButton(R.string.nickname_dialog_save) { _, _ ->
                val updatedNickname = nicknameInput.text?.toString()?.trim().orEmpty()
                lifecycleScope.launch {
                    settingsManager.setPlayerDisplayName(updatedNickname)
                    Toast.makeText(this@GameActivity, R.string.nickname_dialog_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    private fun showDrawCardsDialog(currentDrawSize: Int) {
        val drawOptions = arrayOf(
            getString(R.string.settings_draw_1),
            getString(R.string.settings_draw_3)
        )
        val checkedItem = if (currentDrawSize == 3) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_draw_cards)
            .setSingleChoiceItems(drawOptions, checkedItem) { dialog, which ->
                val selectedDrawSize = if (which == 1) 3 else 1
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(currentSettings.copy(drawSize = selectedDrawSize))
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showWasteRecyclesDialog(isInfinite: Boolean, recycleCount: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_waste_recycles, null)
        UiScaleUtil.applyBaselineScale(dialogView, this)
        val btnMinus = dialogView.findViewById<android.widget.Button>(R.id.btn_recycles_minus)
        val btnPlus  = dialogView.findViewById<android.widget.Button>(R.id.btn_recycles_plus)
        val countText = dialogView.findViewById<TextView>(R.id.text_recycles_count)
        val unlimitedSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_recycles_unlimited)
        val countRow = dialogView.findViewById<android.view.View>(R.id.recycles_count_row)

        var currentCount = recycleCount.coerceAtLeast(1)

        fun updateCountDisplay() {
            countText.text = currentCount.toString()
        }

        fun updateEnabled() {
            val limited = !unlimitedSwitch.isChecked
            countRow.alpha = if (limited) 1f else 0.35f
            btnMinus.isEnabled = limited
            btnPlus.isEnabled = limited
        }

        unlimitedSwitch.isChecked = isInfinite
        updateCountDisplay()
        updateEnabled()

        btnMinus.setOnClickListener {
            if (currentCount > 1) {
                currentCount--
                updateCountDisplay()
            }
        }
        btnPlus.setOnClickListener {
            if (currentCount < 99) {
                currentCount++
                updateCountDisplay()
            }
        }
        unlimitedSwitch.setOnCheckedChangeListener { _, _ -> updateEnabled() }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_waste_recycles)
            .setView(dialogView)
            .setPositiveButton(R.string.nickname_dialog_save) { _, _ ->
                val saveInfinite = unlimitedSwitch.isChecked
                val saveCount = currentCount
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(
                            infiniteRecycles = saveInfinite,
                            recycleCount = saveCount
                        )
                    )
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showBoardLayoutDialog(currentLayout: String) {
        val boardLayoutOptions = arrayOf(
            getString(R.string.settings_board_layout_right),
            getString(R.string.settings_board_layout_left)
        )
        val boardLayoutKeys = listOf("right_hand", "left_hand")
        val checkedItem = boardLayoutKeys.indexOf(currentLayout).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_board_layout)
            .setSingleChoiceItems(boardLayoutOptions, checkedItem) { dialog, which ->
                val selectedLayout = boardLayoutKeys[which]
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(boardLayout = selectedLayout)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showScoreMethodDialog(currentMethod: String) {
        val scoreMethodKeys = listOf("windows", "vegas", "vegas_cumulative", "completion")
        val scoreMethodOptions = arrayOf(
            getString(R.string.score_method_windows),
            getString(R.string.score_method_vegas),
            getString(R.string.score_method_vegas_cumulative),
            getString(R.string.score_method_completion)
        )
        val checkedItem = scoreMethodKeys.indexOf(currentMethod).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.game_menu_score_method)
            .setSingleChoiceItems(scoreMethodOptions, checkedItem) { dialog, which ->
                val selectedMethod = scoreMethodKeys[which]
                lifecycleScope.launch {
                    val currentSettings = settingsManager.gamePlaySettingsFlow.first()
                    settingsManager.saveGamePlaySettings(
                        currentSettings.copy(scoreMethod = selectedMethod)
                    )
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEnableDisableDialog(
        title: String,
        enabled: Boolean,
        onSelection: (Boolean) -> Unit
    ) {
        val options = arrayOf(
            getString(R.string.setting_state_enabled),
            getString(R.string.setting_state_disabled)
        )
        val checkedItem = if (enabled) 0 else 1

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                onSelection(which == 0)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openPrivacyPolicyWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast.makeText(this, R.string.privacy_policy_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTermsOfServiceWebsite(url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast.makeText(this, R.string.terms_of_service_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun readRawResourceText(@RawRes resId: Int): String {
        return resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    private fun openPlayStoreListing() {
        val appPackageName = packageName
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    override fun onGameMenuOpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onGameMenuExitApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finishAffinity()
    }

    override fun onGameMenuExpandStateChanged(state: GameMenuBottomSheetFragment.ExpandState) {
        gameMenuExpandState = state
    }

    override fun onResume() {
        super.onResume()
        // Player has returned – restart the hint inactivity countdown from scratch.
        viewModel.resumeHintTimerAfterNonPlayerActivity()
    }

    override fun onPause() {
        super.onPause()
        // Avoid recording losses on transient pauses (ads, dialogs, rotation).
        viewModel.saveGame()
        // Pause hints while the activity is not in foreground (ad overlay, etc.).
        viewModel.pauseHintTimerForNonPlayerActivity()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            viewModel.stopGame()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyResponsiveControlSizing()
    }

    private fun resetControlGroupAdRequirement() {
        isControlGroupAdUnlocked = isPremiumAccount
        isControlGroupAdFlowInProgress = false
        updateOverlayVisibility()
    }

    private fun runAfterControlGroupUnlock(action: () -> Unit) {
        if (isPremiumAccount || isControlGroupAdUnlocked) {
            action()
            return
        }

        if (isControlGroupAdFlowInProgress) return
        isControlGroupAdFlowInProgress = true
        showControlGroupUnlockAd(action, retriesRemaining = 1)
    }

    private fun showControlGroupUnlockAd(action: () -> Unit, retriesRemaining: Int) {
        val shown = adManager.showRewardedAd(
            onFinished = { rewardEarned ->
                if (rewardEarned) {
                    isControlGroupAdUnlocked = true
                    isControlGroupAdFlowInProgress = false
                    updateOverlayVisibility()
                    action()
                    return@showRewardedAd
                }

                if (retriesRemaining > 0) {
                    showControlGroupUnlockAd(action, retriesRemaining - 1)
                } else {
                    isControlGroupAdFlowInProgress = false
                    Toast.makeText(this, R.string.control_group_ad_required, Toast.LENGTH_SHORT).show()
                    adManager.loadRewardedAd()
                }
            }
        )

        if (!shown) {
            if (retriesRemaining > 0) {
                adManager.loadRewardedAd()
                binding.root.postDelayed(
                    { showControlGroupUnlockAd(action, retriesRemaining - 1) },
                    350L
                )
            } else {
                isControlGroupAdFlowInProgress = false
                Toast.makeText(this, R.string.control_group_ad_required, Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun updateOverlayVisibility() {
        findViewById<ImageView>(R.id.group_ad_overlay)?.visibility =
            if (isPremiumAccount || isControlGroupAdUnlocked) View.GONE else View.VISIBLE
    }

    private fun handleUndoClick() {
        viewModel.undo()
    }

    private fun handleRedoClick() {
        viewModel.redo()
    }

    private fun handleRestartClick() {
        showRestartDialog()
    }

    private fun applyResponsiveControlSizing() {
        val config = resources.configuration
        val widthDp = config.screenWidthDp.toFloat()
        val heightDp = config.screenHeightDp.toFloat()
        val isLandscapeNow = config.orientation == Configuration.ORIENTATION_LANDSCAPE

        val factors = UiScaleUtil.calculateBaselineScaleFactors(widthDp, heightDp)
        val widthScale = factors.horizontal.coerceIn(0.56f, 1.70f)
        val heightScale = factors.vertical.coerceIn(0.56f, 1.70f)
        val textScale = factors.text.coerceIn(0.64f, 1.35f)

        // Portrait button widths already have a 1.75× boost to compensate for the narrow
        // phone screen.  Applying the extreme-aspect compression on top (which the full
        // widthScale carries) would double-penalise the width.  Strip the per-axis factor
        // out so portrait button widths track only the baseline (screen vs. baseline tablet)
        // ratio, not the additional phone-shape compression.
        val portraitButtonWidthScale = if (!isLandscapeNow && factors.isExtremeAspect && factors.horizontalFactor != 0f)
            (factors.horizontal / factors.horizontalFactor).coerceIn(0.56f, 1.70f)
        else
            widthScale

        applyTopHudSizing(isLandscapeNow, widthScale, heightScale, textScale)
        applyBottomControlsSizing(isLandscapeNow, widthScale, heightScale, textScale, portraitButtonWidthScale)
    }

    private fun applyTopHudSizing(
        isLandscape: Boolean,
        widthScale: Float,
        heightScale: Float,
        textScale: Float
    ) {
        val panel = findViewById<LinearLayout>(R.id.game_info_panel) ?: return

        panel.setPaddingRelative(
            dpToPx(16f * widthScale),
            dpToPx(8f * heightScale),
            dpToPx(16f * widthScale),
            dpToPx(8f * heightScale)
        )

        val statTextSp = (if (isLandscape) 14f else 16f) * textScale
        binding.tvScore.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)
        binding.tvMoves.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)
        binding.tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, statTextSp)

        val gemBagBaseDp = if (isLandscape) 22f else 24f
        val gemCountBaseSp = if (isLandscape) 10f else 11f
        val gemMinWidthBaseDp = if (isLandscape) 26f else 28f
        val gemSizePx = dpToPx(gemBagBaseDp * textScale)

        resizeFrame(binding.ivGemBag, gemSizePx, gemSizePx)
        binding.tvGemCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, gemCountBaseSp * textScale)
        binding.tvGemCount.minWidth = dpToPx(gemMinWidthBaseDp * widthScale)
        binding.tvGemCount.setPaddingRelative(
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale),
            dpToPx(6f * widthScale),
            dpToPx(1f * heightScale)
        )
        (binding.tvGemCount.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
            lp.topMargin = 0
            lp.marginEnd = dpToPx(6f * widthScale)
            binding.tvGemCount.layoutParams = lp
        }
    }

    private fun applyBottomControlsSizing(
        isLandscape: Boolean,
        widthScale: Float,
        heightScale: Float,
        textScale: Float,
        portraitButtonWidthScale: Float = widthScale
    ) {
        val controlPanel = findViewById<LinearLayout>(R.id.control_buttons_layout)
        val moveGroup = findViewById<FrameLayout>(R.id.move_controls_group)
        val btnHint = findViewById<Button?>(R.id.btn_hint)
        val btnRestart = findViewById<Button?>(R.id.btn_restart)
        val btnAuto = findViewById<Button?>(R.id.btn_auto_move)
        val undoMain = findViewById<ImageView?>(R.id.undo_main)
        val redoMain = findViewById<ImageView?>(R.id.redo_main)

        val controlTextSp = 8f * textScale
        // Both orientations use heightScale:
        //   Portrait  → heightScale carries the expansion factor (long axis) → buttons grow taller ✓
        //   Landscape → heightScale carries the compression factor (short axis) → buttons shrink ✓
        val controlHeightScale = heightScale
        val portraitWidthBoost = 1.75f

        if (!isLandscape) {
            // Portrait uses fixed button widths; portraitButtonWidthScale strips the extreme-aspect
            // compression so the existing portraitWidthBoost is not double-penalised.
            setButtonSizeDp(binding.btnNewGame, 50f * portraitWidthBoost * portraitButtonWidthScale, 80f * controlHeightScale)
            setButtonSizeDp(binding.btnStats, 56f * portraitWidthBoost * portraitButtonWidthScale, 80f * controlHeightScale)
            setButtonSizeDp(btnHint, 50f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            setButtonSizeDp(btnRestart, 62f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            setButtonSizeDp(btnAuto, 52f * portraitWidthBoost * portraitButtonWidthScale, 72f * controlHeightScale)
            (controlPanel?.layoutParams)?.let { lp ->
                lp.height = dpToPx(80f * controlHeightScale)
                controlPanel.layoutParams = lp
            }
        } else {
            controlPanel?.setPaddingRelative(
                dpToPx(8f * widthScale),
                controlPanel.paddingTop,
                dpToPx(8f * widthScale),
                controlPanel.paddingBottom
            )
        }

        applyButtonScale(binding.btnNewGame, controlTextSp, textScale)
        applyButtonScale(binding.btnStats, controlTextSp, textScale)
        btnHint?.let { applyButtonScale(it, controlTextSp, textScale) }
        btnRestart?.let { applyButtonScale(it, controlTextSp, textScale) }
        btnAuto?.let { applyButtonScale(it, controlTextSp, textScale) }

        // Portrait undo/redo widths also use the baseline-only scale (no extra compression).
        val undoWidthScale = if (isLandscape) widthScale else portraitButtonWidthScale
        val undoFrameW = if (isLandscape) 40f else 30f * portraitWidthBoost
        val undoFrameH = if (isLandscape) 40f else 72f
        val undoImgW = if (isLandscape) 40f else 30f * portraitWidthBoost
        val undoImgH = if (isLandscape) 40f else 66f

        resizeFrame(binding.btnUndo, dpToPx(undoFrameW * undoWidthScale), dpToPx(undoFrameH * controlHeightScale))
        resizeFrame(binding.btnRedo, dpToPx(undoFrameW * undoWidthScale), dpToPx(undoFrameH * controlHeightScale))
        undoMain?.let { resizeFrame(it, dpToPx(undoImgW * undoWidthScale), dpToPx(undoImgH * controlHeightScale)) }
        redoMain?.let { resizeFrame(it, dpToPx(undoImgW * undoWidthScale), dpToPx(undoImgH * controlHeightScale)) }

        moveGroup?.setPaddingRelative(
            dpToPx((if (isLandscape) 4f else 3f) * widthScale),
            dpToPx((if (isLandscape) 4f else 2f) * heightScale),
            dpToPx((if (isLandscape) 4f else 3f) * widthScale),
            dpToPx((if (isLandscape) 4f else 2f) * heightScale)
        )

        val overlaySize = dpToPx(14f * textScale)
        findViewById<View?>(R.id.group_ad_overlay)?.let { resizeFrame(it, overlaySize, overlaySize) }
        (findViewById<View?>(R.id.group_ad_overlay)?.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            lp.marginStart = dpToPx(1f * widthScale)
            lp.topMargin = dpToPx(1f * heightScale)
            findViewById<View>(R.id.group_ad_overlay).layoutParams = lp
        }

        if (binding.btnStats is MaterialButton) {
            binding.btnStats.iconSize = dpToPx(48f * textScale)
            binding.btnStats.iconPadding = dpToPx(if (isLandscape) -12f * textScale else -4f * textScale)
        }
    }

    private fun applyButtonScale(button: Button, textSp: Float, scale: Float) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
        button.minWidth = dpToPx(76f * scale)
        val horizontal = dpToPx(12f * scale)
        val vertical = dpToPx(6f * scale)
        button.setPaddingRelative(horizontal, vertical, horizontal, vertical)
    }

    private fun setButtonSizeDp(button: Button?, widthDp: Float, heightDp: Float) {
        button ?: return
        val lp = button.layoutParams ?: return
        lp.width = dpToPx(widthDp)
        lp.height = dpToPx(heightDp)
        button.layoutParams = lp
    }

    private fun resizeFrame(view: View, widthPx: Int, heightPx: Int) {
        val lp = view.layoutParams ?: return
        lp.width = widthPx
        lp.height = heightPx
        view.layoutParams = lp
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt().coerceAtLeast(1)
    }

    private fun showWinCelebrationThenDialog() {
        if (isFinishing || isDestroyed || winDialogShowing) return

        if (binding.gameBoardView.isCardAnimationActive()) {
            if (!pendingWinUiAfterAnimation) {
                pendingWinUiAfterAnimation = true
                waitForBoardAnimationThenShowWinUi()
            }
            return
        }

        pendingWinUiAfterAnimation = false

        if (!showWinAnimation || winCelebrationPlayed) {
            showGameEndDialog(true)
            return
        }

        winCelebrationPlayed = true
        showGameEndDialog(true)
    }

    private fun waitForBoardAnimationThenShowWinUi() {
        binding.gameBoardView.postOnAnimation {
            if (isFinishing || isDestroyed) {
                pendingWinUiAfterAnimation = false
                return@postOnAnimation
            }

            if (viewModel.game.value.status != GameStatus.WON) {
                pendingWinUiAfterAnimation = false
                return@postOnAnimation
            }

            if (binding.gameBoardView.isCardAnimationActive()) {
                waitForBoardAnimationThenShowWinUi()
            } else {
                pendingWinUiAfterAnimation = false
                showWinCelebrationThenDialog()
            }
        }
    }

    private fun playWinVideo(onFinished: () -> Unit): Boolean {
        // Win videos have been removed - this method is now a no-op
        return false
    }

    private fun stopWinVideoPlayback() {
        // Win videos have been removed - this method is now a no-op
    }

    /**
     * Extension function for weighted random selection.
     * Takes a list of (item, weight) pairs and returns a randomly selected item
     * based on the weights.
     */
    private fun <T> List<Pair<T, Int>>.weightedRandom(): T {
        val totalWeight = this.sumOf { it.second }
        var random = (0 until totalWeight).random()

        for ((item, weight) in this) {
            if (random < weight) {
                return item
            }
            random -= weight
        }

        return this.first().first // Fallback (should never reach here)
    }
}
