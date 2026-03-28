package com.gpgamelab.justpatience.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.ads.AdManager
import com.gpgamelab.justpatience.assets.AndroidAssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.SettingsActivity
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.data.GameStatsManager
import kotlinx.coroutines.launch
import kotlin.math.min

class GameActivity : AppCompatActivity(), GameMenuBottomSheetFragment.Host {

    companion object {
        private const val GAME_LAUNCH_TAG = "GameLaunch"
    }

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // Optional helper UI manager (keeps minimal behavior; extend as needed)
    private lateinit var uiManager: CardStackUIManager

    // Ad management
    private lateinit var adManager: AdManager
    private lateinit var statsManager: GameStatsManager
    private lateinit var settingsManager: SettingsManager

    // Ad enable flags for undo and redo
    private var enableUndo = false
    private var enableRedo = false
    private var enableRestart = false

    // Ad frequency counters
    private var totalGamesPlayed: Int = 0
    private var pendingHomeStartInterstitial: Boolean = false
    private var handledHomeStartInterstitial: Boolean = false
    private var winDialogShowing: Boolean = false
    private var winCelebrationPlayed: Boolean = false
    private var isWinVideoPlaying: Boolean = false
    private var hasShownWinRewardedInterstitialForCurrentWin: Boolean = false
    private var showWinAnimation: Boolean = true
    private var forceNewGameOnLaunch: Boolean = false
    private var gameMenuExpandState = GameMenuBottomSheetFragment.ExpandState()
    private var pendingWinUiAfterAnimation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        adManager.loadInterstitialAd()
        adManager.loadRewardedAd()
        adManager.loadRewardedInterstitialAd()

        statsManager = GameStatsManager(applicationContext)
        settingsManager = SettingsManager(applicationContext)

        // Defer home-start ad decision until we read total games played.
        pendingHomeStartInterstitial = intent.getBooleanExtra("from_home", false)
        forceNewGameOnLaunch = intent.getBooleanExtra("force_new_game", false)
        Log.d(
            GAME_LAUNCH_TAG,
            "onCreate: from_home=$pendingHomeStartInterstitial force_new_game=$forceNewGameOnLaunch"
        )

        // Launch mode is explicit: PLAY forces fresh deal, Continue attempts resume.
        viewModel.initializeForLaunch(forceNewGameOnLaunch)

        updateOverlayVisibility()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    statsManager.getTotalGamesPlayed().collect { total ->
                        totalGamesPlayed = total
                        if (pendingHomeStartInterstitial && !handledHomeStartInterstitial) {
                            maybeShowStartInterstitial()
                            handledHomeStartInterstitial = true
                        }
                    }
                }

                launch {
                    viewModel.game.collect { g ->
                        binding.tvScore.text = getString(R.string.score_format, g.score)
                        binding.tvMoves.text = getString(R.string.moves_format, g.moves)

                        if (g.status != GameStatus.WON) {
                            winCelebrationPlayed = false
                            hasShownWinRewardedInterstitialForCurrentWin = false
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
                    viewModel.showWinAnimation.collect { enabled ->
                        showWinAnimation = enabled
                    }
                }

                launch {
                    viewModel.canUndo.collect { canUndo ->
                        findViewById<ImageView>(R.id.undo_main)?.setImageResource(
                            if (canUndo) R.drawable.undo_red else R.drawable.undo_gray
                        )
                        updateOverlayVisibility()
                    }
                }

                launch {
                    viewModel.canRedo.collect { canRedo ->
                        findViewById<ImageView>(R.id.redo_main)?.setImageResource(
                            if (canRedo) R.drawable.redo_blue else R.drawable.redo_gray
                        )
                        updateOverlayVisibility()
                    }
                }
            }
        }

        // Simple button hookups (if present in layout)
        binding.btnUndo.setOnClickListener {
            handleUndoClick()
        }
        binding.btnRedo.setOnClickListener {
            handleRedoClick()
        }
        binding.btnNewGame.setOnClickListener {
            // If the current game is in-progress it will be finalized (recorded) inside
            // startNewGame(), but that write is async so totalGamesPlayed is still the
            // pre-record value here.  Pass +1 so the threshold check is accurate.
            val pending = if (viewModel.game.value.status == GameStatus.IN_PROGRESS) 1 else 0
            maybeShowStartInterstitial(pending)
            enableUndo = false
            enableRedo = false
            enableRestart = false
            winCelebrationPlayed = false
            hasShownWinRewardedInterstitialForCurrentWin = false
            updateOverlayVisibility()
            viewModel.startNewGame()
        }
        findViewById<Button>(R.id.btn_restart).setOnClickListener { handleRestartClick() }
        binding.btnStats.setOnClickListener { showGameMenu() }
        findViewById<Button>(R.id.btn_auto_move)?.setOnClickListener { buttonView ->
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

        applyResponsiveControlSizing()
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
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
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.win_dialog_title)
            .setMessage(
                getString(
                    R.string.win_dialog_message,
                    viewModel.game.value.score,
                    viewModel.game.value.moves
                )
            )
            .setPositiveButton(R.string.new_game_button_text) { _, _ ->
                maybeShowStartInterstitial()
                enableUndo = false
                enableRedo = false
                enableRestart = false
                winCelebrationPlayed = false
                hasShownWinRewardedInterstitialForCurrentWin = false
                updateOverlayVisibility()
                viewModel.startNewGame()
            }
            .setNeutralButton(R.string.continue_without_reward, null)
            .setOnDismissListener { winDialogShowing = false }

        if (!hasShownWinRewardedInterstitialForCurrentWin) {
            builder.setNegativeButton(R.string.watch_optional_ad) { _, _ ->
                val shown = adManager.showRewardedInterstitialAd(
                    onCompleted = { showGameEndDialog(true) }
                )

                if (shown) {
                    hasShownWinRewardedInterstitialForCurrentWin = true
                } else {
                    Toast.makeText(this, R.string.optional_ad_not_ready, Toast.LENGTH_SHORT).show()
                    adManager.loadRewardedInterstitialAd()
                    binding.root.post { showGameEndDialog(true) }
                }
            }
        }

        builder.show()
    }

    private fun showStatsDialog() {
        StatsDialogFragment.newInstance().show(supportFragmentManager, "stats_dialog")
    }

    private fun showGameMenu() {
        if (supportFragmentManager.findFragmentByTag(GameMenuBottomSheetFragment.TAG) != null) return
        GameMenuBottomSheetFragment.newInstance(gameMenuExpandState).show(
            supportFragmentManager,
            GameMenuBottomSheetFragment.TAG
        )
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
        Log.d(GAME_LAUNCH_TAG, "onPause: saving current game before background/home transition")
        // Avoid recording losses on transient pauses (ads, dialogs, rotation).
        stopWinVideoPlayback()
        viewModel.saveGame()
        // Pause hints while the activity is not in foreground (ad overlay, etc.).
        viewModel.pauseHintTimerForNonPlayerActivity()
    }

    override fun onStop() {
        super.onStop()
        Log.d(GAME_LAUNCH_TAG, "onStop: isFinishing=$isFinishing")
        if (isFinishing) {
            stopWinVideoPlayback()
            viewModel.stopGame()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reload interstitial ad when orientation changes to ensure proper sizing
        adManager.loadInterstitialAd()
        applyResponsiveControlSizing()
    }

    private fun shouldShowStartInterstitial(total: Int): Boolean {
        return when {
            total < 10 -> false
            total <= 30 -> total % 3 == 0
            else -> total % 2 == 0
        }
    }

    /**
     * @param pendingRecordCount Pass 1 when the current in-progress game is about to be
     * finalized (recorded) by the action that triggered this call (e.g. pressing New Game
     * mid-game).  That recording is async, so totalGamesPlayed is still the pre-record value
     * at this point; adding 1 corrects for the stale count so the threshold check is accurate.
     */
    private fun maybeShowStartInterstitial(pendingRecordCount: Int = 0) {
        if (!shouldShowStartInterstitial(totalGamesPlayed + pendingRecordCount)) return

        // Show immediately if loaded; otherwise show when load completes.
        adManager.setShowOnLoad(true)
        adManager.showInterstitialAd()
    }

    private fun updateOverlayVisibility() {
        findViewById<ImageView>(R.id.undo_overlay)?.visibility = if (enableUndo) View.GONE else View.VISIBLE
        findViewById<ImageView>(R.id.redo_overlay)?.visibility = if (enableRedo) View.GONE else View.VISIBLE
        findViewById<ImageView>(R.id.restart_overlay)?.visibility = if (enableRestart) View.GONE else View.VISIBLE
    }

    private fun handleUndoClick() {
        if (enableUndo) {
            viewModel.undo()
            return
        }

        val shown = adManager.showRewardedAdUndoBtn {
            enableUndo = true
            updateOverlayVisibility()
            viewModel.undo()
        }

        // If ad isn't ready, gracefully unlock once and continue gameplay.
        if (!shown) {
            enableUndo = true
            updateOverlayVisibility()
            viewModel.undo()
            adManager.loadRewardedAdUndoBtn()
        }
    }

    private fun handleRedoClick() {
        if (enableRedo) {
            viewModel.redo()
            return
        }

        val shown = adManager.showRewardedAdRedoBtn {
            enableRedo = true
            updateOverlayVisibility()
            viewModel.redo()
        }

        // If ad isn't ready, gracefully unlock once and continue gameplay.
        if (!shown) {
            enableRedo = true
            updateOverlayVisibility()
            viewModel.redo()
            adManager.loadRewardedAdRedoBtn()
        }
    }

    private fun handleRestartClick() {
        if (enableRestart) {
            showRestartDialog()
            return
        }

        val shown = adManager.showRewardedAdRestartBtn {
            enableRestart = true
            updateOverlayVisibility()
            showRestartDialog()
        }

        // If ad isn't ready, gracefully unlock once and continue gameplay.
        if (!shown) {
            enableRestart = true
            updateOverlayVisibility()
            showRestartDialog()
            adManager.loadRewardedAdRestartBtn()
        }
    }

    private fun applyResponsiveControlSizing() {
        val config = resources.configuration
        val widthDp = config.screenWidthDp.toFloat()
        val heightDp = config.screenHeightDp.toFloat()
        val minDp = min(widthDp, heightDp)
        val isLandscapeNow = config.orientation == Configuration.ORIENTATION_LANDSCAPE

        val baseScale = (minDp / 360f).coerceIn(0.82f, 1.10f)
        val orientationScale = if (isLandscapeNow) 0.88f else 0.95f
        val scale = (baseScale * orientationScale).coerceIn(0.72f, 1.10f)

        val controlTextSp = 8f * scale * 0.90f

        applyButtonScale(binding.btnNewGame, controlTextSp, scale)
        findViewById<Button?>(R.id.btn_restart)?.let { applyButtonScale(it, controlTextSp, scale) }
        applyButtonScale(binding.btnStats, controlTextSp, scale)
        findViewById<Button?>(R.id.btn_auto_move)?.let { applyButtonScale(it, controlTextSp, scale) }

        val iconSizePx = dpToPx(40f * scale)
        val overlaySizePx = dpToPx(8f * scale)

        resizeFrame(binding.btnUndo, iconSizePx, iconSizePx)
        resizeFrame(binding.btnRedo, iconSizePx, iconSizePx)
        findViewById<View?>(R.id.undo_overlay)?.let { resizeFrame(it, overlaySizePx, overlaySizePx) }
        findViewById<View?>(R.id.redo_overlay)?.let { resizeFrame(it, overlaySizePx, overlaySizePx) }
        findViewById<View?>(R.id.restart_overlay)?.let { resizeFrame(it, overlaySizePx, overlaySizePx) }
    }

    private fun applyButtonScale(button: Button, textSp: Float, scale: Float) {
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSp)
        button.minWidth = dpToPx(76f * scale)
        val horizontal = dpToPx(12f * scale)
        val vertical = dpToPx(6f * scale)
        button.setPaddingRelative(horizontal, vertical, horizontal, vertical)
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
        if (isFinishing || isDestroyed || winDialogShowing || isWinVideoPlaying) return

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

        val played = playWinVideo {
            showGameEndDialog(true)
        }

        if (!played) {
            showGameEndDialog(true)
        }
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
        // Weighted video selection: name to weight (higher = more likely)
        // Weights: 50% chance for video_01, 30% for video_02, 20% for video_03
        val weightedVideos = listOf(
            "gpgameslab_solitaire_win_01" to 50,
            "gpgameslab_solitaire_win_02" to 30,
            "gpgameslab_solitaire_win_03" to 20
        )

        // Pick a weighted random video
        val randomVideoName = weightedVideos.weightedRandom()
        val resId = resources.getIdentifier(randomVideoName, "raw", packageName)

        if (resId == 0) {
            return false
        }

        val overlay = findViewById<FrameLayout>(R.id.win_video_overlay) ?: return false
        val videoView = findViewById<VideoView>(R.id.win_video_view) ?: return false

        isWinVideoPlaying = true
        overlay.visibility = View.VISIBLE

        val finishPlayback = {
            stopWinVideoPlayback()
            onFinished()
        }

        videoView.setOnCompletionListener { finishPlayback() }
        videoView.setOnErrorListener { _, _, _ ->
            finishPlayback()
            true
        }

        videoView.setVideoURI(Uri.parse("android.resource://$packageName/$resId"))
        videoView.start()
        return true
    }

    private fun stopWinVideoPlayback() {
        val overlay = findViewById<FrameLayout>(R.id.win_video_overlay)
        val videoView = findViewById<VideoView>(R.id.win_video_view)

        videoView?.stopPlayback()
        overlay?.visibility = View.GONE
        isWinVideoPlaying = false
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
