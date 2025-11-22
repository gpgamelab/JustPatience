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
import com.gpgamelab.justpatience.SettingsActivity
import com.gpgamelab.justpatience.HomeActivity // <-- FIX: Import HomeActivity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check for existing token and navigate if authenticated (to prevent flashing login screen)
        if (authViewModel.authToken.value != null) {
            navigateToHome()
            return
        }

        setupListeners()
        setupObservers()
    }

    private fun setupListeners() {
        // All view references fixed to use 'binding.'
        binding.btnRegister.setOnClickListener {
            // Clear any prior errors
            binding.tvError.visibility = View.GONE
            binding.tvError.text = ""

            // 1. Get user input
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // 2. Simple validation
            if (email.isBlank() || password.isBlank()) {
                binding.tvError.text = "Email and Password cannot be empty."
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // 3. Call ViewModel
            authViewModel.register(UserRegistrationRequest(email, password, "username"))
        }

        // All view references fixed to use 'binding.'
        binding.btnLogin.setOnClickListener {
            // Clear any prior errors
            binding.tvError.visibility = View.GONE
            binding.tvError.text = ""

            // 1. Get user input
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            // 2. Simple validation
            if (email.isBlank() || password.isBlank()) {
                binding.tvError.text = "Email and Password cannot be empty."
                binding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // 3. Call ViewModel
            authViewModel.login(UserLoginRequest(email, password))
        }

        // All view references fixed to use 'binding.'
        binding.btnGuestLogin.setOnClickListener {
            // In a real app, this would perform anonymous or temporary authentication
            Toast.makeText(this, "Logged in as Guest.", Toast.LENGTH_SHORT).show()
            navigateToHome()
        }
    }

    private fun setupObservers() {
        // --- Observer 1: Loading State ---
        authViewModel.isLoading.observe(this) { isLoading ->
            // Use View Binding references
            binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.btnRegister.isEnabled = !isLoading
            binding.btnGuestLogin.isEnabled = !isLoading
        }

        // --- Observer 2: Error State ---
        authViewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                // Use View Binding references
                binding.tvError.text = "Error: $errorMessage"
                binding.tvError.visibility = View.VISIBLE
                Log.e("Auth", "Authentication Error: $errorMessage")
            } else {
                binding.tvError.visibility = View.GONE
                binding.tvError.text = ""
            }
        }

        // --- Observer 3: Authentication Result (One-time event after login/register) ---
        authViewModel.authResult.observe(this) { authResponse ->
            if (authResponse != null) {
                // Hide error message if successful
                binding.tvError.visibility = View.GONE

                Toast.makeText(this, "Welcome, ${authResponse.username}!", Toast.LENGTH_LONG).show()
                Log.i("Auth", "Login/Registration Successful. Token is saved.")

                // Navigate to the main activity upon success
                navigateToHome()
            }
        }

        // --- Observer 4: Global Token Status (Flow) ---
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authToken.collect { token ->
                    if (token != null) {
                        Log.d("Auth", "Token flow detected: User is authenticated.")
                        // Navigate if we didn't already and the Activity is not finishing
                        if (!isFinishing) {
                            navigateToHome()
                        }
                    } else {
                        Log.d("Auth", "Token flow detected: User is unauthenticated (Logged out).")
                        binding.tvTitle.text = "Just Patience Login"
                    }
                }
            }
        }
    }

    /**
     * Helper function to navigate to the HomeActivity and clear the back stack.
     * Fixed the 'flags' and type inference errors.
     */
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}