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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private var timerJob: Job? = null

    private val undoStack = ArrayDeque<Game>()
    private val redoStack = ArrayDeque<Game>()
    private var initialGameState: Game? = null  // Store the game state at the start of the hand
    private var currentHandRecorded = false

    // Exposed state
    private val _game = MutableStateFlow(Game.newGame())

    val game: StateFlow<Game> = _game

    init {

        startNewGame()

    }

    private fun startTimer() {
        stopTimer()
        gameTime.value = 0


        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                gameTime.value += 1
            }
        }
    }

    fun stopTimer() {
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
        startTimer()
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
        currentHandRecorded = false
        updateUndoRedoState()
        saveGame()
    }

    fun drawFromStock() {
        val game = _game.value
        var moved = false
        var newGame = game

        // 1️⃣ Normal draw
        if (!game.stock.isEmpty()) {
            val (newStock, card) = game.stock.withCardPopped()
            if (card != null) {
                val newWaste = game.waste.withCardAdded(card.copy(isFaceUp = true))
                newGame = game.copy(stock = newStock, waste = newWaste)
                moved = true
            }
        } else {
            // 2️⃣ Recycle waste → stock
            if (!game.waste.isEmpty()) {
                val recycled = game.recycleWasteToStock()
                if (recycled != null) {
                    newGame = recycled
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

    /**
     * Record a completed game in the database.
     * Only records if moves >= 5 and game hasn't been recorded yet.
     * Called when a game is won, or when the game is abandoned with enough moves.
     */
    private fun recordGameCompletion(game: Game, isWin: Boolean) {
        // Only record if game hasn't been recorded yet and player made at least 5 moves
        if (currentHandRecorded || game.moves < 5) {
            return
        }

        currentHandRecorded = true

        viewModelScope.launch {
            try {
                statsManager.recordGame(
                    score = game.score,
                    moves = game.moves,
                    timeMs = gameTime.value * 1000,  // Convert seconds to milliseconds
                    isWin = isWin
                )
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to record game completion", e)
            }
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
                val json = gson.toJson(_game.value)
                repository.saveCurrentGameState(json)
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
        // Record only if this hand is still in progress and eligible.
        finalizeCurrentGameIfNeeded()
        saveGame()
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
        val updated = game.recycleWasteToStock()

        if (updated != null) {
            undoStack.addLast(game)
            redoStack.clear()
            _game.value = updated
            updateUndoRedoState()
            return true
        }

        return false
    }
}
