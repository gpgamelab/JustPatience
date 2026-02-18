package com.gpgamelab.justpatience.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.assets.AndroidAssetResolver
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.databinding.ActivityGameBinding
import com.gpgamelab.justpatience.model.GameStatus
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()

    // Optional helper UI manager (keeps minimal behavior; extend as needed)
    private lateinit var uiManager: CardStackUIManager


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

        // Optional manager (no heavy rendering here)
        uiManager = CardStackUIManager(this, binding.root, viewModel)

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.game.collect { g ->
                    binding.tvScore.text = getString(R.string.score_format, g.score)
                    binding.tvMoves.text = getString(R.string.moves_format, g.moves)
                    binding.gameBoardView.postInvalidateOnAnimation()
                    if (g.status == GameStatus.WON) showGameEndDialog(true)
                }
            }
        }

        // Simple button hookups (if present in layout)
        binding.btnUndo.setOnClickListener { viewModel.undo() }
        binding.btnNewGame.setOnClickListener { viewModel.startNewGame() }
        binding.btnRestart.setOnClickListener { showRestartDialog() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_game, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_game -> viewModel.startNewGame()
            R.id.action_restart -> showRestartDialog()
            R.id.action_undo -> viewModel.undo()
            R.id.action_settings -> startActivity(Intent(this, com.gpgamelab.justpatience.SettingsActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_game_title)
            .setMessage(R.string.restart_game_message)
            .setPositiveButton(R.string.restart_game_text) { _, _ -> null } //viewModel.restart() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showGameEndDialog(isWin: Boolean) {
        if (isWin) {
            AlertDialog.Builder(this)
                .setTitle(R.string.win_dialog_title)
                .setMessage(getString(R.string.win_dialog_message, viewModel.game.value.score, viewModel.game.value.moves))
                .setPositiveButton(R.string.new_game_button_text) { _, _ -> viewModel.startNewGame() }
                .setNeutralButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveGame()
    }
}
