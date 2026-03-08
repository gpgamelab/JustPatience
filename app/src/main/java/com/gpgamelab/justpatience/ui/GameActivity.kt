package com.gpgamelab.justpatience.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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

    // Ad frequency counters
    private var totalGamesPlayed: Int = 0
    private var pendingHomeStartInterstitial: Boolean = false
    private var handledHomeStartInterstitial: Boolean = false
    private var winDialogShowing: Boolean = false

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

                        if (g.status == GameStatus.WON) showGameEndDialog(true)
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
            updateOverlayVisibility()
            viewModel.startNewGame()
        }
        binding.btnRestart.setOnClickListener { showRestartDialog() }
        binding.btnStats.setOnClickListener { showStatsDialog() }

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
        viewModel.saveGame()
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
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
    }

    private fun handleUndoClick() {
        if (enableUndo) {
            viewModel.undo()
            return
        }

        val shown = adManager.showRewardedAd {
            enableUndo = true
            updateOverlayVisibility()
            viewModel.undo()
        }

        // If ad isn't ready, gracefully unlock once and continue gameplay.
        if (!shown) {
            enableUndo = true
            updateOverlayVisibility()
            viewModel.undo()
            adManager.loadRewardedAd()
        }
    }

    private fun handleRedoClick() {
        if (enableRedo) {
            viewModel.redo()
            return
        }

        val shown = adManager.showRewardedAd {
            enableRedo = true
            updateOverlayVisibility()
            viewModel.redo()
        }

        // If ad isn't ready, gracefully unlock once and continue gameplay.
        if (!shown) {
            enableRedo = true
            updateOverlayVisibility()
            viewModel.redo()
            adManager.loadRewardedAd()
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
        applyButtonScale(binding.btnRestart, controlTextSp, scale)
        applyButtonScale(binding.btnStats, statsTextSp, scale)

        val iconSizePx = dpToPx(48f * scale)
        val overlaySizePx = dpToPx(24f * scale)

        resizeFrame(binding.btnUndo, iconSizePx, iconSizePx)
        resizeFrame(binding.btnRedo, iconSizePx, iconSizePx)
        resizeFrame(findViewById(R.id.undo_overlay), overlaySizePx, overlaySizePx)
        resizeFrame(findViewById(R.id.redo_overlay), overlaySizePx, overlaySizePx)
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
}
