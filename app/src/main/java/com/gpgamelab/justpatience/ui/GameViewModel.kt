package com.gpgamelab.justpatience.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
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
    private val gson = Gson()
    private val controller = GameController()
    val hasSavedGame = repository.getCurrentGameState()
        .map { !it.isNullOrEmpty() }
    val gameTime = MutableStateFlow(0L)

    private var timerJob: Job? = null

    private val undoStack = ArrayDeque<Game>()
    private val redoStack = ArrayDeque<Game>()

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
        undoStack.clear()
        redoStack.clear()
        startTimer()
        viewModelScope.launch {
            _game.value = controller.newGameWithClearHistory()
            saveGame()
        }
    }

    fun drawFromStock() {
        val snapshot = _game.value.deepCopy()
        val game = _game.value
        var moved = false

        // 1️⃣ Normal draw
        if (!game.stock.isEmpty()) {
            val card = game.stock.pop()
                ?: throw IllegalStateException("Stock pop failed")
            game.waste.push(card.copy(isFaceUp = true))
            moved = true

        } else {
            // 2️⃣ Recycle waste → stock
            if (!game.waste.isEmpty()) {

                val recycled = game.waste.take(game.waste.size())
                    ?: throw IllegalStateException("Waste take failed")

                recycled
                    .reversed()
                    .forEach { card ->
                        game.stock.push(card.copy(isFaceUp = false))
                    }

                moved = true
            }
        }

        if (moved) {
            undoStack.addLast(snapshot)
            redoStack.clear()
            updateAfterMove(game)  // scoreDelta = 0 for simple scoring
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
                val (theUpdatedGame, success) = controller.attemptMove(
                    _game.value,
                    sourceType,
                    sourceIndex,
                    cardIndexInSource,
                    targetType,
                    targetIndex
                )
                if (success) {
                    _game.value = theUpdatedGame
                    saveGameIfInProgress()
                }
            } catch (t: Throwable) {
                Log.w("GameViewModel", "attemptMove failed: ${t.message}")
            }
        }
    }

    fun tryMoveWasteToTableau(index: Int) {
        val snapshot = _game.value.deepCopy()
        val game = _game.value
        val updated = game.moveWasteToTableau(index)

        if (updated != game) {
            undoStack.addLast(snapshot)
            redoStack.clear()
            updateAfterMove(game, scoreDelta = 5)   // simple scoring
        }

        _game.value = updated
    }

    fun tryMoveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Boolean {
        val snapshot = _game.value.deepCopy()
        val game = _game.value
        val moved = game.moveTableauToTableau(fromIndex, cardIndex, toIndex)

        if (moved) {
            undoStack.addLast(snapshot)
            redoStack.clear()
            updateAfterMove(game)   // scoreDelta = 0 for simple scoring
        }

        return moved
    }

    fun tryAutoMoveWasteToFoundation(): Boolean {
        val snapshot = _game.value.deepCopy()
        val game = _game.value

        for (i in game.foundations.indices) {
            val moved = game.moveWasteToFoundation(i)
            if (moved) {
                undoStack.addLast(snapshot)
                redoStack.clear()
                updateAfterMove(game, scoreDelta = 10)
                return true
            }
        }
        return false
    }

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int): Boolean {
        val snapshot = _game.value.deepCopy()
        val game = _game.value
        val pile = game.tableau[tableauIndex]

        if (pile.isEmpty()) return false

        val topIndex = pile.size() - 1
        val topCard = pile.peek()

        // 🚨 IMPORTANT
        if (topCard?.isFaceUp != true) return false

        for (i in game.foundations.indices) {
            val moved = game.moveTableauToFoundation(
                tableauIndex,
                topIndex,
                i
            )
            if (moved) {
                undoStack.addLast(snapshot)
                redoStack.clear()
                updateAfterMove(game, scoreDelta = 10)
                return true
            }
        }

        return false
    }

    fun tryMoveWasteToFoundation(index: Int): Boolean {
        val snapshot = _game.value.deepCopy()
        val game = _game.value
        val moved = game.moveWasteToFoundation(index)

        if (moved) {
            undoStack.addLast(snapshot)
            redoStack.clear()
            updateAfterMove(game, scoreDelta = 10)
        }

        return moved
    }

    fun tryMoveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Boolean {
        val currentGame = _game.value
        val newGame = currentGame.deepCopy()

        val moved = newGame.moveTableauToFoundation(
            tableauIndex,
            cardIndex,
            foundationIndex
        )

        if (moved) {
            undoStack.addLast(currentGame.deepCopy())
            redoStack.clear()

            updateAfterMove(newGame, scoreDelta = 10)

            _game.value = newGame
        }

        return moved
    }

    private fun updateAfterMove(game: Game, scoreDelta: Int = 0) {
        if (game.isWinCondition()) {
            stopTimer()
            _game.value = game.copy(
                status = GameStatus.WON,
                score = game.score + scoreDelta,
                moves = game.moves + 1
            )
        } else {
            _game.value = game.copy(score = game.score + scoreDelta, moves = game.moves + 1)
        }
    }

    fun undo() {

        if (undoStack.isNotEmpty()) {
            val current = _game.value.deepCopy()

            redoStack.addLast(current)
            _game.value = undoStack.removeLast().deepCopy()

        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return

        val current = _game.value.deepCopy()

        undoStack.addLast(current)

        _game.value = redoStack.removeLast().deepCopy()
    }

    private fun saveGameIfInProgress() {
        if (_game.value.status == GameStatus.IN_PROGRESS) saveGame()
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
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            false
        }
    }

    fun stopGame() {
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
        val snapshot = _game.value.deepCopy()
        val current = _game.value
        val recycled = current.recycleWasteToStock()

        if (recycled) {
            undoStack.addLast(snapshot)
            redoStack.clear()
            _game.value = current
        }

        return recycled
    }
}
