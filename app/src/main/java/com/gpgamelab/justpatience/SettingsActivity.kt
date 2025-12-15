package com.gpgamelab.justpatience

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.ViewModelProvider // Needed for ViewModel setup
import com.gpgamelab.justpatience.databinding.ActivitySettingsBinding
import com.gpgamelab.justpatience.viewmodel.AuthViewModel
import com.gpgamelab.justpatience.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/**
 * Activity for managing game settings, user statistics, and authentication actions (logout).
 * This activity uses ViewBinding and observes the SettingsViewModel for data updates.
 */
class SettingsActivity : AppCompatActivity() {

//    // Initialize View Binding
//    private lateinit var binding: ActivitySettingsBinding
//
//    // Initialize ViewModels using delegates
//    private val settingsViewModel: SettingsViewModel by viewModels()
//    private val authViewModel: AuthViewModel by viewModels()


//    private lateinit var authViewModel: AuthViewModel
    private val authViewModel: AuthViewModel by viewModels()

    //    private lateinit var settingsViewModel: SettingsViewModel
    private val settingsViewModel: SettingsViewModel by viewModels()

    private lateinit var binding: ActivitySettingsBinding


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Use View Binding to inflate the layout
//        binding = ActivitySettingsBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Setup the Toolbar
//        val toolbar: Toolbar = binding.toolbar
//        setSupportActionBar(toolbar)
//        supportActionBar?.title = getString(R.string.title_settings)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        toolbar.setNavigationOnClickListener { finish() }
//
//        // --- Event Listeners ---
//
//        // Toggling SFX Switch
//        binding.switchSfx.setOnCheckedChangeListener { _, isChecked ->
//            // Only call ViewModel if the change is from the user, not the initial observation update
//            Log.d("SettingsActivity", "SFX Switch changed to $isChecked")
//            settingsViewModel.toggleSound() // ViewModel handles the logic and persistence
//        }
//
//        // Toggling Hints Switch
//        binding.switchHints.setOnCheckedChangeListener { _, isChecked ->
//            Log.d("SettingsActivity", "Hints Switch changed to $isChecked")
//            settingsViewModel.toggleHints()
//        }
//
//        // Listener for Logout button
//        binding.btnSignOut.setOnClickListener { showLogoutDialog() }
//
//        // Listener for Reset Stats button
//        binding.btnResetStats.setOnClickListener { showResetStatsDialog() }
//
//        // --- Data Observers ---
//
//        // Observe User Settings and update the UI controls
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                settingsViewModel.userSettings.collect { settings ->
//                    settings?.let {
//                        // Crucial: setChecked without triggering the listener on load/update
//                        binding.switchSfx.isChecked = it.isSoundEnabled
//                        binding.switchHints.isChecked = it.isHintsEnabled
//                        Log.d("SettingsActivity", "UI updated: Sound=${it.isSoundEnabled}, Hints=${it.isHintsEnabled}")
//                    }
//                }
//            }
//        }
//
//        // Observe User Stats and update the TextViews
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                settingsViewModel.userStats.collect { stats ->
//                    stats?.let {
//                        binding.tvGamesPlayedValue.text = it.gamesPlayed.toString()
//                        binding.tvGamesWonValue.text = it.gamesWon.toString()
//                        binding.tvHighScoreValue.text = it.highScore.toString()
//
//                        val winRate = if (it.gamesPlayed > 0) (it.gamesWon.toDouble() / it.gamesPlayed * 100) else 0.0
//                        binding.tvWinRateValue.text = getString(R.string.win_rate_format, String.format("%.1f", winRate))
//                    }
//                }
//            }
//        }
//
//        // Observe User Data (for login status)
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                settingsViewModel.userData.collect { userData ->
//                    val statusText = if (userData?.isLoggedIn == true) {
//                        getString(R.string.status_logged_in)
//                    } else {
//                        getString(R.string.status_guest_user)
//                    }
//                    binding.tvUserStatus.text = statusText
//                    // Update logout button text/visibility if needed, based on isLoggedIn
//                }
//            }
//        }
//
//        // Observe Logout Result from AuthViewModel
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                authViewModel.logoutStatus.collect { isLoggedOut ->
//                    if (isLoggedOut) {
//                        Toast.makeText(this@SettingsActivity, "Logged out successfully.", Toast.LENGTH_SHORT).show()
//                        // Navigate back to LoginActivity
//                        val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
//                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                        }
//                        startActivity(intent)
//                        finish()
//                    }
//                }
//            }
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2. Initialize ViewModels (Placeholder - adapt to your DI framework)
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
                Toast.makeText(this, getString(R.string.stats_reset_success), Toast.LENGTH_SHORT).show()
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





}