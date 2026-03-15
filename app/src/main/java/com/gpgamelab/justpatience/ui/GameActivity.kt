package com.gpgamelab.justpatience.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
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
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.data.GameStatsManager
import kotlinx.coroutines.launch
import kotlin.math.min

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // Optional helper UI manager (keeps minimal behavior; extend as needed)
    private lateinit var uiManager: CardStackUIManager

    // Ad management
    private lateinit var adManager: AdManager
    private lateinit var statsManager: GameStatsManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.app_name)

        // Wire viewModel into GameBoardView
        binding.gameBoardView.viewModel = viewModel

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

        statsManager = GameStatsManager(applicationContext)

        // Defer home-start ad decision until we read total games played.
        pendingHomeStartInterstitial = intent.getBooleanExtra("from_home", false)

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
            maybeShowStartInterstitial()
            enableUndo = false
            enableRedo = false
            enableRestart = false
            winCelebrationPlayed = false
            updateOverlayVisibility()
            viewModel.startNewGame()
        }
        findViewById<Button>(R.id.btn_restart).setOnClickListener { handleRestartClick() }
        binding.btnStats.setOnClickListener { showStatsDialog() }
        findViewById<Button>(R.id.btn_auto_move)?.setOnClickListener {
            val movesMade = viewModel.performAutoMove()
            if (movesMade == 0) {
                Toast.makeText(this, "No moves available", Toast.LENGTH_SHORT).show()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_game, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Navigate back to home page
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return true
            }
            R.id.action_new_game -> {
                maybeShowStartInterstitial()
                enableUndo = false
                enableRedo = false
                enableRestart = false
                winCelebrationPlayed = false
                updateOverlayVisibility()
                viewModel.startNewGame()
            }
            R.id.action_restart -> showRestartDialog()
            R.id.action_undo -> {
                handleUndoClick()
            }
            R.id.action_settings -> startActivity(
                Intent(
                    this,
                    com.gpgamelab.justpatience.SettingsActivity::class.java
                )
            )

            else -> return super.onOptionsItemSelected(item)
        }
        return true
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
        AlertDialog.Builder(this)
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
                updateOverlayVisibility()
                viewModel.startNewGame()
            }
            .setNeutralButton(android.R.string.cancel, null)
            .setOnDismissListener { winDialogShowing = false }
            .show()
    }

    private fun showStatsDialog() {
        StatsDialogFragment.newInstance().show(supportFragmentManager, "stats_dialog")
    }

    override fun onPause() {
        super.onPause()
        // Avoid recording losses on transient pauses (ads, dialogs, rotation).
        stopWinVideoPlayback()
        viewModel.saveGame()
    }

    override fun onStop() {
        super.onStop()
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
            total <= 50 -> total % 5 == 0
            else -> total % 4 == 0
        }
    }

    private fun maybeShowStartInterstitial() {
        if (!shouldShowStartInterstitial(totalGamesPlayed)) return

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
        val orientationScale = if (isLandscapeNow) 0.88f else 1.0f
        val scale = (baseScale * orientationScale).coerceIn(0.72f, 1.10f)

        val controlTextSp = 14f * scale * 0.90f
        val statsTextSp = controlTextSp * 0.90f // Keep STATS 10% smaller in all cases.

        applyButtonScale(binding.btnNewGame, controlTextSp, scale)
        findViewById<Button?>(R.id.btn_restart)?.let { applyButtonScale(it, controlTextSp, scale) }
        applyButtonScale(binding.btnStats, statsTextSp, scale)
        findViewById<Button?>(R.id.btn_auto_move)?.let { applyButtonScale(it, controlTextSp, scale) }

        val iconSizePx = dpToPx(48f * scale)
        val overlaySizePx = dpToPx(24f * scale)

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

        if (winCelebrationPlayed) {
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
