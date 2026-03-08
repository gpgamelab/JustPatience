package com.gpgamelab.justpatience

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.gpgamelab.justpatience.databinding.ActivitySettingsBinding
import com.gpgamelab.justpatience.viewmodel.AuthViewModel
import com.gpgamelab.justpatience.viewmodel.SettingsViewModel

/**
 * Activity for managing game settings, user statistics, and authentication actions (logout).
 * This activity uses ViewBinding and observes the SettingsViewModel for data updates.
 */
class SettingsActivity : AppCompatActivity() {

    //    private lateinit var authViewModel: AuthViewModel
    private val authViewModel: AuthViewModel by viewModels()

    //    private lateinit var settingsViewModel: SettingsViewModel
    private val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Setup the Toolbar with back button
        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // 3. Initialize ViewModels (Placeholder - adapt to your DI framework)
        // Since I don't see your DI setup, I'll mock the initialization.
        // **YOU MUST replace this block with your actual ViewModel initialization.**
        // Example for testing/manual setup:
        // val authRepo = AuthRepository(ApiClient.authService, TokenManager(applicationContext))
        // authViewModel = ViewModelProvider(this, AuthViewModelFactory(authRepo, ...)).get(AuthViewModel::class.java)
        // ... (etc)

        // Assuming your Activity has access to the view models (e.g., via Koin's 'getViewModel'):
        // This is a common pattern for injection:
        // authViewModel = getViewModel()
        // settingsViewModel = getViewModel()

        // For demonstration, let's assume they are initialized correctly.
        // For now, let's assume you'll replace this with your proper DI initialization.
        // If your app crashes here, you need to set up the DI/Factory.

        // --- Placeholder for your ViewModel Initialization ---
        // You MUST replace this with proper DI or Factory setup.
        // As a temporary measure for compilation, you might use:
        // authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)
        // settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        // --- END Placeholder ---

        // 3. Bind Actions
        setupListeners()
    }


    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_logout_title)
            .setMessage(R.string.confirm_logout_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                authViewModel.logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showResetStatsDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_reset_stats_title)
            .setMessage(R.string.confirm_reset_stats_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                settingsViewModel.resetStats()
                Toast.makeText(this, getString(R.string.stats_reset_success), Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun setupListeners() {
        // Logout Implementation
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Reset Stats Implementation
        binding.resetStatsButton.setOnClickListener {
            showResetStatsConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(R.string.yes) { _, _ ->
                authViewModel.logout()
                navigateToLogin()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            // Clear all activities in the stack and start a new one for LoginActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Finish SettingsActivity so user can't press back to it
    }

    private fun showResetStatsConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_stats_title)
            .setMessage(R.string.confirm_reset_stats_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                settingsViewModel.resetStats()
                Toast.makeText(this, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate back to home page
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}