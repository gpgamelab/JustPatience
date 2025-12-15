package com.gpgamelab.justpatience

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.gpgamelab.justpatience.api.UserLoginRequest
import com.gpgamelab.justpatience.api.UserRegistrationRequest
import com.gpgamelab.justpatience.databinding.ActivityLoginBinding
import com.gpgamelab.justpatience.viewmodel.AuthViewModel

/**
 * The entry point for the application, handling user login and registration.
 * This Activity observes the AuthViewModel for state changes (loading, error, token).
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // Late-initialized property for ViewBinding
    private lateinit var binding: ActivityLoginBinding

    // Initialize the ViewModel using the standard Android viewModels delegate
    private val authViewModel: AuthViewModel by viewModels()

    // Private property to track the current mode
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Initialize ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if the user is already authenticated (e.g., has a saved token)
        // This is handled by observing the token flow below.

        // --- Event Listeners (UI Interaction) ---

        // Setup UI listeners
        // Log in button listener
        binding.btnAuthenticate.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                if (isLoginMode) {
                    authViewModel.login(UserLoginRequest(username, password))
                } else {
                    // You may want to add logic for an optional 'email' field here
                    authViewModel.register(UserRegistrationRequest(username, password))
                }
            } else {
                Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // The Switch Mode Button/TextView: Toggles the UI state only.
        binding.tvSwitchMode.setOnClickListener {
            // Toggle the mode state
            isLoginMode = !isLoginMode
            // Update the UI to reflect the new mode
            updateUiForMode(isLoginMode)
            // Clear any previous error/status messages
            binding.tvErrorMessage.visibility = View.GONE
            binding.tvAuthStatus.visibility = View.GONE
        }

        // Set initial UI state (default to Login)
        setAuthMode(isLoginMode) // Call the new function

        binding.tvSwitchMode.setOnClickListener {
            isLoginMode = !isLoginMode
            setAuthMode(isLoginMode)
            binding.tvErrorMessage.visibility = View.GONE // Clear errors
            binding.tvAuthStatus.visibility = View.GONE   // Clear status
        }

        // Example: Navigate to settings (for testing purposes, assuming settings is accessible even when logged out)
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // --- Data Observers (ViewModel State) ---

        // Observer 1: Loading Status
        authViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnAuthenticate.isEnabled = !isLoading
            binding.tvSwitchMode.isEnabled = !isLoading
        }

        // Observer 2: Error Messages
        authViewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, "Authentication Error: $errorMessage", Toast.LENGTH_LONG).show()
                Log.e("Auth", "Error: $errorMessage")
                // Clear the error after showing it
                // authViewModel._error.value = null // Not accessible, rely on next network call to clear
            }
        }

        // Observer 3: Successful Auth Result (Registration or Login)
        authViewModel.authResult.observe(this) { authResponse ->
            if (authResponse != null) {
                // Hide the loading indicator
                binding.progressBar.visibility = View.GONE

                // In a real app, you would navigate to the main game screen here.
                // For now, we'll just show a Toast and navigate to a placeholder screen (Home or Settings).
                Toast.makeText(this, "Welcome, ${authResponse.username}! Proceeding to game...", Toast.LENGTH_LONG).show()
                Log.i("Auth", "Login Successful. Token: ${authResponse.authToken}")

                // Example navigation to Home/Main Game Activity
                val intent = Intent(this, SettingsActivity::class.java) // Placeholder for now
                startActivity(intent)
                finish() // Prevent going back to login
            }
        }

        // --- Observer 4: Global Token Status (Flow) ---
        // Best way to watch for the token being saved by the repository/manager
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authToken.collect { token ->
                    if (token != null) {
                        Log.d("Auth", "Token flow detected: User is authenticated. Token present.")
                        // If token is present, navigate directly to the main activity/home screen
                        // This handles auto-login on app start.
                        val intent = Intent(this@LoginActivity, SettingsActivity::class.java) // Placeholder for now
                        startActivity(intent)
                        finish() // Prevent going back to login
                    } else {
                        Log.d("Auth", "Token flow detected: User is unauthenticated. Token missing.")
                        binding.tvTitle.text = "Just Patience Login"
                    }
                }
            }
        }
    }

    /**
     * Updates the UI to reflect the current authentication mode (Login or Register).
     */
    private fun updateUiForMode(isLoginMode: Boolean) {
        if (isLoginMode) {
            binding.tvTitle.text = getString(R.string.login_title_text)
            binding.btnAuthenticate.text = getString(R.string.login_title_text)
            binding.tvSwitchMode.text = getString(R.string.switch_to_register)
            // Set the hint for the password field if you want to
            binding.tilPassword.hint = getString(R.string.password_hint)
        } else {
            binding.tvTitle.text = getString(R.string.register_title)
            binding.btnAuthenticate.text = getString(R.string.register)
            binding.tvSwitchMode.text = getString(R.string.switch_to_login)
            // Optional: Change the password hint for registration to be more specific
            binding.tilPassword.hint = getString(R.string.password_min_length_hint)
        }
    }

//    private fun setAuthMode(isLogin: Boolean) {
//        // Fix for 'login_title' (using the new resource)
//        binding.tvTitle.text = getString(
//            if (isLogin) R.string.login_title_text else R.string.register_title_text
//        )
//
//        // Fix for 'login' (using R.string.login_button_text)
//        binding.btnAuthenticate.text = getString(
//            if (isLogin) R.string.login_title_text else R.string.register_title //
//        )
//
//        // Use the existing switch strings
//        binding.tvSwitchMode.text = getString(
//            if (isLogin) R.string.switch_to_register else R.string.switch_to_login //
//        )
//
//        // Show/Hide the email field for registration
//        binding.tilEmail.visibility = if (isLogin) View.GONE else View.VISIBLE
//
//        // Fix for 'password' (using R.string.password_hint)
//        binding.tilPassword.hint = getString(R.string.password_hint)
//
//        // Clear email on switch
//        binding.etEmail.text?.clear()
//    }

    private fun setAuthMode(isLogin: Boolean) {
        // Fix for 'login_title' (using the new resource)
        binding.tvTitle.text = getString(
            if (isLogin) R.string.login_title_text else R.string.register_title_text
        )

        // FIX: The authenticate button text should use dedicated action strings
        // instead of the title strings. Assuming R.string.login_button_text and
        // R.string.register_button_text exist.
        binding.btnAuthenticate.text = getString(
            if (isLogin) R.string.login_button_text else R.string.register_button_text
        )

        // Use the existing switch strings
        binding.tvSwitchMode.text = getString(
            if (isLogin) R.string.switch_to_register else R.string.switch_to_login //
        )

        // Show/Hide the email field for registration
        binding.tilEmail.visibility = if (isLogin) View.GONE else View.VISIBLE

        // Fix for 'password' (using R.string.password_hint)
        binding.tilPassword.hint = getString(R.string.password_hint)

        this.isLoginMode = isLogin
    }
}