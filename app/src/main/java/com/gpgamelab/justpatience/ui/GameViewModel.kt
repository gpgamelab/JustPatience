package com.gpgamelab.justpatience.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.gpgamelab.justpatience.data.GameStatsManager
import com.gpgamelab.justpatience.data.SettingsManager
import com.gpgamelab.justpatience.data.TokenManager
import com.gpgamelab.justpatience.game.GameController
import com.gpgamelab.justpatience.model.Card
import com.gpgamelab.justpatience.model.Game
import com.gpgamelab.justpatience.model.GameStatus
import com.gpgamelab.justpatience.model.StackType
import com.gpgamelab.justpatience.repository.GameRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel that holds the current Game state and provides actions for UI.
 * Calls the GameController (rules engine) for all moves.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application.applicationContext)
    private val tokenManager = TokenManager(application.applicationContext)
    private val repository = GameRepository(settingsManager, tokenManager)
    private val statsManager = GameStatsManager(application.applicationContext)
    private val gson = Gson()
    private val controller = GameController()
    val hasSavedGame = repository.getCurrentGameState()
        .map { !it.isNullOrEmpty() }
    val gameTime = MutableStateFlow(0L)

    // Current draw size setting (supports any positive integer, <=0 defaults to 1)
    private var currentDrawSize = 1
    private var currentRecycleLimit = 3
    private var currentInfiniteRecycles = false

    private val _drawCountForDisplay = MutableStateFlow(1)
    val drawCountForDisplay: StateFlow<Int> = _drawCountForDisplay

    private val _recycleLimitForDisplay = MutableStateFlow(3)
    val recycleLimitForDisplay: StateFlow<Int> = _recycleLimitForDisplay

    private val _isInfiniteRecycles = MutableStateFlow(false)
    val isInfiniteRecycles: StateFlow<Boolean> = _isInfiniteRecycles

    private val _showGameTimer = MutableStateFlow(true)
    val showGameTimer: StateFlow<Boolean> = _showGameTimer

    private val _showWinAnimation = MutableStateFlow(true)
    val showWinAnimation: StateFlow<Boolean> = _showWinAnimation

    private var timerJob: Job? = null
    private var isTimerRunning = false
    private var hasRegisteredFirstMove = false

    private val undoStack = ArrayDeque<Game>()
    private val redoStack = ArrayDeque<Game>()
    private var initialGameState: Game? = null  // Store the game state at the start of the hand
    private var currentHandRecorded = false

    // Exposed state
    private val _game = MutableStateFlow(Game.newGame())

    val game: StateFlow<Game> = _game

    init {
        // Detect whether the process was killed while a game was in progress in the background.
        // isGameSessionActive() is a brief blocking DataStore read – safe from the main thread
        // because DataStore dispatches I/O independently and won't deadlock here.
        val sessionWasInterrupted = settingsManager.isGameSessionActive()
        if (sessionWasInterrupted) {
            // Process was killed mid-session. Record the abandoned game as a loss, then
            // start fresh. We do NOT call startNewGame() here because that would call
            // finalizeCurrentGameIfNeeded() on the empty in-memory game (0 moves), not
            // on the saved game we need to inspect.
            viewModelScope.launch {
                checkAndRecordAbandonedGame()
                startNewGameInternal()
            }
        } else {
            startNewGame()
        }
        observeDrawSizeSetting()
    }

    /**
     * Called on first init when the session-active flag was still set, indicating the process
     * was killed while a game was in progress in the background.
     *
     * Loads the persisted game JSON, and if it represents an IN_PROGRESS game with enough
     * moves to be meaningful (>= 5), records it as an abandoned loss.
     * Always clears the session-active flag when done so the next cold start is clean.
     */
    private suspend fun checkAndRecordAbandonedGame() {
        try {
            val savedJson = repository.getCurrentGameState().firstOrNull()
            if (!savedJson.isNullOrEmpty()) {
                val abandonedGame = gson.fromJson(savedJson, Game::class.java)
                if (abandonedGame.status == GameStatus.IN_PROGRESS) {
                    Log.d("GameViewModel",
                        "Abandoned session detected (moves=${abandonedGame.moves}); recording as loss.")
                    recordGameCompletionInternal(abandonedGame, isWin = false)
                }
                // If status == WON the game was already recorded; nothing to do.
            }
        } catch (t: Throwable) {
            Log.w("GameViewModel", "checkAndRecordAbandonedGame failed: ${t.message}")
        } finally {
            // Always clear the flag so we don't double-record on the next cold start.
            settingsManager.setGameSessionActive(false)
        }
    }

    /**
     * Core new-game setup used when init already handled the abandoned-game check.
     * Skips finalizeCurrentGameIfNeeded() to avoid double-recording.
     */
    private suspend fun startNewGameInternal() {
        undoStack.clear()
        redoStack.clear()
        currentHandRecorded = false
        val newGame = controller.newGameWithClearHistory()
        initialGameState = newGame
        _game.value = newGame
        resetTimerForNewHand()
        saveGame()
    }

    private fun observeDrawSizeSetting() {
        viewModelScope.launch {
            settingsManager.gamePlaySettingsFlow.collect { settings ->
                currentDrawSize = settings.drawSize
                currentRecycleLimit = settings.recycleCount.coerceAtLeast(0)
                currentInfiniteRecycles = settings.infiniteRecycles

                _drawCountForDisplay.value = normalizeDrawCount(currentDrawSize)
                _recycleLimitForDisplay.value = currentRecycleLimit
                _isInfiniteRecycles.value = currentInfiniteRecycles
                _showGameTimer.value = settings.showGameTimer
                _showWinAnimation.value = settings.showWinAnimation
            }
        }
    }

    private fun normalizeDrawCount(rawDrawCount: Int): Int {
        return if (rawDrawCount <= 0) 1 else rawDrawCount
    }

    fun getDrawCountLabelValue(): Int = _drawCountForDisplay.value

    fun getRemainingRecycleCount(): Int? {
        if (currentInfiniteRecycles) return null
        return (currentRecycleLimit - _game.value.recycleCountUsed).coerceAtLeast(0)
    }

    private fun canRecycleWaste(game: Game): Boolean {
        if (currentInfiniteRecycles) return true
        return game.recycleCountUsed < currentRecycleLimit
    }

    private fun startTimerIfNeeded() {
        if (isTimerRunning) return
        isTimerRunning = true
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                gameTime.value += 1
            }
        }
    }

    private fun startTimerOnFirstSuccessfulMoveIfNeeded() {
        if (!hasRegisteredFirstMove) {
            hasRegisteredFirstMove = true
        }
        startTimerIfNeeded()
    }

    private fun resetTimerForNewHand() {
        stopTimer()
        gameTime.value = 0
        hasRegisteredFirstMove = false
    }

    fun stopTimer() {
        isTimerRunning = false
        timerJob?.cancel()
    }

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undoLastMove(): Boolean {
        undo()
        return true
    }
    fun redoLastMove(): Boolean {
        redo()
        return true
    }

    fun startNewGame() {
        finalizeCurrentGameIfNeeded()
        undoStack.clear()
        redoStack.clear()
        resetTimerForNewHand()
        currentHandRecorded = false
        viewModelScope.launch {
            val newGame = controller.newGameWithClearHistory()
            initialGameState = newGame
            _game.value = newGame
            saveGame()
        }
    }

    fun restartGame() {
        // Restart counts as abandoning the current hand.
        finalizeCurrentGameIfNeeded()

        val initial = initialGameState ?: return
        undoStack.clear()
        redoStack.clear()
        _game.value = initial
        resetTimerForNewHand()
        currentHandRecorded = false
        updateUndoRedoState()
        saveGame()
    }

    fun drawFromStock() {
        val game = _game.value
        var moved = false
        var newGame = game

        // Determine how many cards to draw (<=0 defaults to 1)
        val drawCount = normalizeDrawCount(currentDrawSize)

        // 1️⃣ Normal draw - draw multiple cards if configured
        if (!game.stock.isEmpty()) {
            var currentStock = game.stock
            var currentWaste = game.waste
            var cardsDrawn = 0

            repeat(drawCount) {
                if (currentStock.isEmpty()) return@repeat
                val (newStock, card) = currentStock.withCardPopped()
                currentStock = newStock
                if (card != null) {
                    currentWaste = currentWaste.withCardAdded(card.copy(isFaceUp = true))
                    cardsDrawn++
                }
            }

            if (cardsDrawn > 0) {
                newGame = game.copy(stock = currentStock, waste = currentWaste)
                moved = true
            }
        } else {
            // Recycle waste -> stock only if recycle limit allows it.
            if (!game.waste.isEmpty() && canRecycleWaste(game)) {
                val recycled = game.recycleWasteToStock()
                if (recycled != null) {
                    newGame = recycled.copy(recycleCountUsed = game.recycleCountUsed + 1)
                    moved = true
                }
            }
        }

        if (moved) {
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(newGame)  // scoreDelta = 0 for simple scoring
            _game.value = updatedWithScore
            updateUndoRedoState()
        }
    }

    fun attemptMove(
        sourceType: StackType,
        sourceIndex: Int,
        cardIndexInSource: Int,
        targetType: StackType,
        targetIndex: Int
    ) {
        viewModelScope.launch {
            try {
                val game = _game.value
                val (theUpdatedGame, success) = controller.attemptMove(
                    game,
                    sourceType,
                    sourceIndex,
                    cardIndexInSource,
                    targetType,
                    targetIndex
                )
                if (success) {
                    undoStack.addLast(game)
                    redoStack.clear()
                    // Score is already applied by controller, just update move count
                    val updatedWithMoves = theUpdatedGame.copy(moves = theUpdatedGame.moves + 1)
                    startTimerOnFirstSuccessfulMoveIfNeeded()
                    _game.value = updatedWithMoves
                    saveGameIfInProgress()
                    updateUndoRedoState()
                }
            } catch (t: Throwable) {
                Log.w("GameViewModel", "attemptMove failed: ${t.message}")
            }
        }
    }

    fun tryMoveWasteToTableau(index: Int) {
        val game = _game.value
        val updated = game.moveWasteToTableau(index)

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 5)   // simple scoring
            _game.value = updatedWithScore
            updateUndoRedoState()
        }
    }

    fun tryMoveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Boolean {
        val game = _game.value
        val updated = game.moveTableauToTableau(fromIndex, cardIndex, toIndex)

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated)   // scoreDelta = 0 for simple scoring
            _game.value = updatedWithScore
            updateUndoRedoState()
            return true
        }

        return false
    }

    fun tryAutoMoveWasteToFoundation(): Boolean {
        val game = _game.value

        for (i in game.foundations.indices) {
            val updated = game.moveWasteToFoundation(i)
            if (updated != null) {
                undoStack.addLast(game)
                redoStack.clear()
                val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
                _game.value = updatedWithScore
                updateUndoRedoState()
                return true
            }
        }
        return false
    }

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int): Boolean {
        val game = _game.value
        val pile = game.tableau.getOrNull(tableauIndex) ?: return false

        if (pile.isEmpty()) return false

        val topIndex = pile.size() - 1
        val topCard = pile.peek()

        // 🚨 IMPORTANT
        if (topCard?.isFaceUp != true) return false

        for (i in game.foundations.indices) {
            val updated = game.moveTableauToFoundation(
                tableauIndex,
                topIndex,
                i
            )
            if (updated != null) {
                undoStack.addLast(game)
                redoStack.clear()
                val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
                _game.value = updatedWithScore
                updateUndoRedoState()
                return true
            }
        }

        return false
    }

    /**
     * Auto-move all possible cards to foundations.
     * Priority: tableau to foundation, then waste to foundation.
     * Returns the number of moves made.
     */
    fun performAutoMove(): Int {
        var moveCount = 0
        var madeMoveThisPass = true

        // Keep trying until no more moves can be made
        while (madeMoveThisPass) {
            madeMoveThisPass = false

            // First try all tableau piles
            for (tableauIndex in _game.value.tableau.indices) {
                if (tryAutoMoveTableauTopToFoundation(tableauIndex)) {
                    moveCount++
                    madeMoveThisPass = true
                    break // Restart the check from the beginning
                }
            }

            // If no tableau moves, try waste
            if (!madeMoveThisPass && tryAutoMoveWasteToFoundation()) {
                moveCount++
                madeMoveThisPass = true
            }
        }

        return moveCount
    }

    fun tryMoveWasteToFoundation(index: Int): Boolean {
        val game = _game.value
        val updated = game.moveWasteToFoundation(index)

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
            _game.value = updatedWithScore
            return true
        }

        return false
    }

    fun tryMoveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Boolean {
        val game = _game.value
        val updated = game.moveTableauToFoundation(
            tableauIndex,
            cardIndex,
            foundationIndex
        )

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            val updatedWithScore = updateAfterMove(updated, scoreDelta = 10)
            _game.value = updatedWithScore
            updateUndoRedoState()
            return true
        }

        return false
    }

    private fun updateAfterMove(game: Game, scoreDelta: Int = 0): Game {
        startTimerOnFirstSuccessfulMoveIfNeeded()
        return if (game.isWinCondition()) {
            stopTimer()
            val updatedGame = game.copy(
                status = GameStatus.WON,
                score = game.score + scoreDelta,
                moves = game.moves + 1
            )
            // Record the win in stats
            recordGameCompletion(updatedGame, isWin = true)
            updatedGame
        } else {
            game.copy(score = game.score + scoreDelta, moves = game.moves + 1)
        }
    }

    private suspend fun recordGameCompletionInternal(game: Game, isWin: Boolean) {
        // Only record if game hasn't been recorded yet and player made at least 5 moves
        if (currentHandRecorded || game.moves < 5) return

        currentHandRecorded = true

        try {
            val settings = settingsManager.gamePlaySettingsFlow.firstOrNull()
            val playerName = settings?.playerDisplayName
            val cardsDraw = settings?.drawSize
            statsManager.recordGame(
                score = game.score,
                moves = game.moves,
                timeMs = gameTime.value * 1000,  // Convert seconds to milliseconds
                isWin = isWin,
                playerName = playerName,
                cardsDraw = cardsDraw
            )
        } catch (e: Exception) {
            Log.e("GameViewModel", "Failed to record game completion", e)
            currentHandRecorded = false
        }
    }

    /**
     * Record a completed game in the database.
     * Only records if moves >= 5 and game hasn't been recorded yet.
     * Called when a game is won, or when the game is abandoned with enough moves.
     */
    private fun recordGameCompletion(game: Game, isWin: Boolean) {
        viewModelScope.launch {
            recordGameCompletionInternal(game, isWin)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _game.value
            redoStack.addLast(current)
            _game.value = undoStack.removeLast()
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val current = _game.value
        undoStack.addLast(current)
        _game.value = redoStack.removeLast()
        updateUndoRedoState()
    }

    private fun saveGameIfInProgress() {
        if (_game.value.status == GameStatus.IN_PROGRESS) saveGame()
    }

    // Record an in-progress game as a loss before replacing it with a new hand.
    private fun finalizeCurrentGameIfNeeded() {
        val current = _game.value
        if (current.status == GameStatus.IN_PROGRESS) {
            recordGameCompletion(current, isWin = false)
        }
    }

    fun saveGame() {
        viewModelScope.launch {
            try {
                val gameToSave = _game.value
                val json = gson.toJson(gameToSave)
                repository.saveCurrentGameState(json)
                // Mirror session state: true while an in-progress game exists in storage,
                // false once the game has reached a terminal state.
                settingsManager.setGameSessionActive(gameToSave.status == GameStatus.IN_PROGRESS)
            } catch (t: Throwable) {
                Log.w("GameViewModel", "saveGame failed: ${t.message}")
            }
        }
    }

    suspend fun loadSavedGame(): Boolean {
        return try {
            val saved = repository.getCurrentGameState().firstOrNull()
            if (!saved.isNullOrEmpty()) {
                _game.value = gson.fromJson(saved, Game::class.java)
                currentHandRecorded = false
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }

    fun stopGame() {
        val current = _game.value

        // runBlocking ensures the record + save + flag-clear all complete before the ViewModel
        // is torn down when the Activity finishes (back-press / explicit exit).
        runBlocking {
            if (current.status == GameStatus.IN_PROGRESS) {
                recordGameCompletionInternal(current, isWin = false)
            }
            // Persist final state and clear the session flag in the same block so there is
            // no window where the process could die between the two operations.
            try {
                val json = gson.toJson(current)
                repository.saveCurrentGameState(json)
            } catch (t: Throwable) {
                Log.w("GameViewModel", "stopGame save failed: ${t.message}")
            }
            settingsManager.setGameSessionActive(false)
        }
    }

    /**
     * Convenience: locate the stack that contains a specific card instance.
     * Used by CardStackUIManager if needed.
     */
    fun findStackContainingCard(card: com.gpgamelab.justpatience.model.Card)
            : Pair<StackType, Int>? {
        val g = _game.value
        if (g.waste.peek() == card) return Pair(StackType.WASTE, 0)
        if (g.stock.peek() == card) return Pair(StackType.STOCK, 0)
        g.foundations.forEachIndexed { idx, pile ->
            if (pile.peek() == card) return Pair(StackType.FOUNDATION, idx)
            if (pile.asList().contains(card)) return Pair(StackType.FOUNDATION, idx)
        }
        g.tableau.forEachIndexed { idx, pile ->
            if (pile.asList().contains(card)) return Pair(StackType.TABLEAU, idx)
        }
        return null
    }

    fun tryRecycleWasteToStock(): Boolean {
        val game = _game.value

        if (!canRecycleWaste(game)) return false

        val updated = game.recycleWasteToStock()

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            startTimerOnFirstSuccessfulMoveIfNeeded()
            _game.value = updated.copy(recycleCountUsed = game.recycleCountUsed + 1)
            updateUndoRedoState()
            return true
        }

        return false
    }
}
