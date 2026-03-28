package com.gpgamelab.justpatience.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.SettingsActivity
import com.gpgamelab.justpatience.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "HomeActivity"

/**
 * Home/Landing page - Main menu for the solitaire game.
 * Shows quick stats, game status, and navigation to game, settings, and shop.
 */
class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private var hasGameInProgress: Boolean = false
    private val isDebugBuild: Boolean by lazy {
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_home_menu)
            setupToolbar()
            setupListeners()
            observeGameStats()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            if (toolbar != null) {
                setSupportActionBar(toolbar)
                supportActionBar?.title = getString(R.string.app_name)
            } else {
                if (isDebugBuild) {
                    Log.w(TAG, "Toolbar not found in layout")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar", e)
        }
    }

    private fun setupListeners() {
        try {
            findViewById<Button>(R.id.btn_play_game)?.setOnClickListener {
                try {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("from_home", true)
                    intent.putExtra("force_new_game", true)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting GameActivity", e)
                }
            }

            findViewById<Button>(R.id.btn_continue_game)?.setOnClickListener {
                try {
                    if (hasGameInProgress) {
                        startActivity(Intent(this, GameActivity::class.java))
                    } else {
                        Toast.makeText(this, R.string.no_saved_game_to_continue, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting GameActivity from continue", e)
                }
            }

            // ...existing listeners...

            findViewById<Button>(R.id.btn_settings)?.setOnClickListener {
                try {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting SettingsActivity", e)
                }
            }

            findViewById<Button>(R.id.btn_how_to_play)?.setOnClickListener {
                try {
                    startActivity(Intent(this, HowToPlayActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting HowToPlayActivity", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up listeners", e)
        }
    }

    private fun observeGameStats() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    try {
                        viewModel.totalGamesPlayed.collect { games ->
                            findViewById<TextView>(R.id.tv_games_played)?.text = games.toString()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting games played", e)
                    }
                }

                launch {
                    try {
                        viewModel.totalGamesWon.collect { wins ->
                            findViewById<TextView>(R.id.tv_games_won)?.text = wins.toString()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting games won", e)
                    }
                }

                launch {
                    try {
                        viewModel.winRate.collect { rate ->
                            findViewById<TextView>(R.id.tv_win_rate)?.text =
                                String.format(Locale.getDefault(), "%.1f%%", rate)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting win rate", e)
                    }
                }

                launch {
                    try {
                        viewModel.highestScore.collect { score ->
                            findViewById<TextView>(R.id.tv_highest_score)?.text =
                                score?.toString() ?: "0"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting highest score", e)
                    }
                }

                launch {
                    try {
                        viewModel.hasGameInProgress.collect { hasInProgress ->
                            hasGameInProgress = hasInProgress
                            findViewById<Button>(R.id.btn_continue_game)?.apply {
                                visibility = if (hasInProgress) View.VISIBLE else View.GONE
                                isEnabled = hasInProgress
                                alpha = 1.0f
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting continue state", e)
                    }
                }

                launch {
                    try {
                        viewModel.lastGameSummary.collect { summary ->
                            if (summary != null) {
                                findViewById<TextView>(R.id.tv_last_game_summary)?.text = summary
                                findViewById<TextView>(R.id.tv_last_game_summary)?.visibility = View.VISIBLE
                            } else {
                                findViewById<TextView>(R.id.tv_last_game_summary)?.visibility = View.GONE
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error collecting last game summary", e)
                    }
                }
            }
        }
    }
}
