package com.gpgamelab.justpatience

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.databinding.ActivitySettingsBinding
import com.gpgamelab.justpatience.viewmodel.AuthViewModel
import com.gpgamelab.justpatience.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Activity for managing user settings and viewing game statistics.
 * The data is observed from the SettingsViewModel.
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    // ViewModels are retrieved via the delegate
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels() // Needed for logout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Initialize View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Toolbar Setup ---
        // Fixed unresolved reference 'toolbarSettings'
        setSupportActionBar(binding.toolbarSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings & Stats"

        // Fixed unresolved reference 'setNavigationOnClickListener'
        binding.toolbarSettings.setNavigationOnClickListener {
            // Navigate back to the main activity
            onBackPressedDispatcher.onBackPressed()
        }

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // Fixed unresolved references for buttons and switches using 'binding.'
        binding.btnSignOut.setOnClickListener { showLogoutDialog() }
        binding.btnResetStats.setOnClickListener { showResetStatsDialog() }

        // Use the OnCheckedChangeListener for the switches and call ViewModel action
        binding.switchSfx.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.toggleSound()
        }

        binding.switchHints.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.toggleHints()
        }
    }

    private fun setupObservers() {
        // The data is collected from the ViewModel's flows
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe User Settings
                settingsViewModel.userSettings.collect { settings ->
                    if (settings != null) {
                        // Fixed unresolved references 'switchSfx' and property names
                        binding.switchSfx.isChecked = settings.soundEnabled
                        binding.switchHints.isChecked = settings.hintsEnabled
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe User Stats
                settingsViewModel.userStats.collect { stats ->
                    if (stats != null) {
                        // Fixed unresolved references 'tvTotalGamesPlayed', 'tvHighScores', and property names
                        binding.tvTotalGamesPlayed.text = "Total Games Played: ${stats.gamesPlayed}"
                        binding.tvHighScores.text = "Top Scores: ${stats.highScores.joinToString(", ")}"
                    } else {
                        binding.tvTotalGamesPlayed.text = "Loading stats..."
                        binding.tvHighScores.text = "Loading scores..."
                    }
                }
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                // Use AuthViewModel to clear the token
                authViewModel.logout()

                // Navigate back to LoginActivity and clear the back stack
                val intent = Intent(this, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetStatsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Reset Stats")
            .setMessage("Are you sure you want to reset all game statistics (scores, games played)? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                settingsViewModel.resetStats()
                Toast.makeText(this, "Game statistics have been reset.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}