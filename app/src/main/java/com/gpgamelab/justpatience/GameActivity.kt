package com.gpgamelab.justpatience

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.viewmodel.GameViewModel
import com.gpgamelab.justpatience.viewmodel.SettingsViewModel // Required for checking sound settings
import kotlinx.coroutines.launch

/**
 * The main activity where the Solitaire game is played.
 * Manages the game UI, handles user interaction, and observes the GameViewModel.
 */
class GameActivity : AppCompatActivity() {

    // ViewModels for game state and settings
    private val gameViewModel: GameViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // UI elements
    private lateinit var toolbar: Toolbar
    private lateinit var tvScore: TextView
    private lateinit var tvMoves: TextView
    private lateinit var tvTime: TextView
    private lateinit var gameBoardView: GameBoardView // Assuming a custom view for the game board

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Note: You must create R.layout.activity_game in res/layout/
        setContentView(R.layout.activity_game)

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        tvScore = findViewById(R.id.tv_score)
        tvMoves = findViewById(R.id.tv_moves)
        tvTime = findViewById(R.id.tv_time)
        gameBoardView = findViewById(R.id.game_board_view) // Assuming custom view ID

        setupToolbar()
        setupGameView()
        observeGameState()

        // Important: If a saved game was loaded in HomeActivity, this will continue it.
        // If HomeActivity started a new game, this will run the initialization logic.
        if (gameViewModel.gameStatus.value == GameStatus.NOT_STARTED) {
            gameViewModel.startNewGame()
        }
    }

    /**
     * Sets up the custom game board view and provides it with the ViewModel and necessary data.
     */
    private fun setupGameView() {
        // Pass the ViewModel to the custom view so it can handle touch events and call logic
        gameBoardView.setViewModel(gameViewModel)

        // Set up the listener for when the custom view detects a card move
        gameBoardView.setOnMoveMadeListener { from, to, cardIndex ->
            // Pass the move data directly to the ViewModel for processing
            gameViewModel.makeMove(from, to, cardIndex)
        }
    }

    /**
     * Observes the game state flows from the ViewModel and updates the UI elements.
     */
    private fun observeGameState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Score
                launch {
                    gameViewModel.score.collect { score ->
                        tvScore.text = getString(R.string.score_format, score) // Assumes @string/score_format: "Score: %d"
                    }
                }

                // Observe Moves
                launch {
                    gameViewModel.moves.collect { moves ->
                        tvMoves.text = getString(R.string.moves_format, moves) // Assumes @string/moves_format: "Moves: %d"
                    }
                }

                // Observe Game Board State (Piles and Deck)
                launch {
                    gameViewModel.tableauPiles.collect { piles ->
                        // Pass the new pile data to the custom view for drawing
                        gameBoardView.updateTableau(piles)
                    }
                }

                // Observe Game Status (Win/Loss)
                launch {
                    gameViewModel.gameStatus.collect { status ->
                        when (status) {
                            GameStatus.WON -> showGameEndDialog(true)
                            GameStatus.LOST -> showGameEndDialog(false)
                            else -> { /* Game in progress, no action needed */ }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets up the toolbar as the ActionBar and inflates the game menu.
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button
        supportActionBar?.title = getString(R.string.game_title) // Assumes @string/game_title
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_game, menu) // Assumes R.menu.menu_game exists
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button press in the toolbar
                showExitConfirmationDialog()
                true
            }
            R.id.action_undo -> { // Assumes @id/action_undo
                handleUndo()
                true
            }
            R.id.action_hint -> { // Assumes @id/action_hint
                handleHint()
                true
            }
            R.id.action_restart -> { // Assumes @id/action_restart
                showRestartConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Dialog shown when the user attempts to exit the game.
     */
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit_game_title) // Assumes @string/exit_game_title
            .setMessage(R.string.exit_game_message) // Assumes @string/exit_game_message
            .setPositiveButton(R.string.save_and_exit) { _, _ -> // Assumes @string/save_and_exit
                gameViewModel.saveGame()
                finish() // Go back to HomeActivity
            }
            .setNegativeButton(R.string.discard_and_exit) { _, _ -> // Assumes @string/discard_and_exit
                // Do NOT save the game
                finish()
            }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }

    /**
     * Dialog shown to confirm restarting the game.
     */
    private fun showRestartConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_game_title) // Assumes @string/restart_game_title
            .setMessage(R.string.restart_game_message) // Assumes @string/restart_game_message
            .setPositiveButton(R.string.restart) { _, _ ->
                gameViewModel.startNewGame()
                Toast.makeText(this, "Game restarted!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Shows the win or lose screen dialog and handles recording stats.
     */
    private fun showGameEndDialog(isWin: Boolean) {
        val titleId = if (isWin) R.string.game_won_title else R.string.game_lost_title
        val messageId = if (isWin) R.string.game_won_message else R.string.game_lost_message

        // Record the result in the repository
        lifecycleScope.launch {
            if (isWin) {
                gameViewModel.recordGameWin(gameViewModel.score.value)
            } else {
                gameViewModel.recordGameLoss()
            }
            // Clear the saved game state once the game is over
            gameViewModel.clearSavedGame()
        }

        AlertDialog.Builder(this)
            .setTitle(titleId)
            .setMessage(getString(messageId, gameViewModel.score.value, gameViewModel.moves.value))
            .setPositiveButton(R.string.new_game_button_text) { _, _ ->
                gameViewModel.startNewGame()
            }
            .setNegativeButton(R.string.exit_to_home) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // --- Action Handlers ---

    private fun handleUndo() {
        // gameViewModel.undoLastMove() - Logic would go here
        Toast.makeText(this, "Undo feature not yet implemented!", Toast.LENGTH_SHORT).show()
    }

    private fun handleHint() {
        // gameViewModel.getHint() - Logic would go here
        val hintsEnabled = settingsViewModel.userSettings.value?.hintsEnabled ?: false
        if (hintsEnabled) {
            Toast.makeText(this, "Showing next possible move...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Hints are disabled in settings.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Lifecycle Overrides ---

    override fun onPause() {
        super.onPause()
        // Save the game state when the activity is paused (e.g., user switches apps or goes home)
        gameViewModel.saveGame()
        Log.d("GameActivity", "Game state saved onPause.")
    }

    override fun onBackPressed() {
        // Intercept the system back button press to confirm exit/save
        showExitConfirmationDialog()
    }

    /**
     * Placeholder class for a custom game board view (needs to be created).
     * In a real app, this would be a custom View extending View or SurfaceView to draw the game.
     */
    class GameBoardView(context: android.content.Context, attrs: android.util.AttributeSet? = null) :
        android.view.View(context, attrs) {

        private var viewModel: GameViewModel? = null
        private var onMoveMadeListener: ((from: String, to: String, cardIndex: Int) -> Unit)? = null

        fun setViewModel(vm: GameViewModel) {
            viewModel = vm
        }

        fun setOnMoveMadeListener(listener: (from: String, to: String, cardIndex: Int) -> Unit) {
            onMoveMadeListener = listener
        }

        fun updateTableau(piles: List<List<com.gpgamelab.justpatience.model.Card>>) {
            // In a real app, this would trigger a redraw (invalidate())
            Log.v("GameBoardView", "Tableau updated with ${piles.size} piles.")
        }

        // You would override onDraw, onTouchEvent, and onMeasure here
        // to handle the actual drawing and drag-and-drop logic for cards.
    }
}