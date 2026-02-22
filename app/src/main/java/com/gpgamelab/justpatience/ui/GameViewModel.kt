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

    // Exposed state
    private val _game = MutableStateFlow(Game.newGame())
    val game: StateFlow<Game> = _game

    init {

        startNewGame()

    }

    fun undoLastMove(): Boolean {
        undo()
        return true
    }

    fun startNewGame() {
        viewModelScope.launch {
            _game.value = controller.newGameWithClearHistory()
            saveGame()
        }
    }

    fun drawFromStock() {
        viewModelScope.launch {
            // 1️⃣ Normal draw
            if (!_game.value.stock.isEmpty()) {
                val card: Card =
                    _game.value.stock.pop() ?: throw IllegalStateException("Stock pop failed")
                card.isFaceUp = true
                _game.value.waste.push(card)
            } else {
                // 2️⃣ Recycle waste → stock
                if (!_game.value.waste.isEmpty()) {
                    val recycled =
                        _game.value.waste.take(_game.value.waste.size())
                            ?: throw IllegalStateException("Waste take failed")

                    recycled
                        .reversed()
                        .forEach { card ->
                            card.isFaceUp = false
                            _game.value.stock.push(card)
                        }
                }
            }
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
        val current = _game.value
        val updated = current.moveWasteToTableau(index)
        _game.value = updated
    }

    fun tryMoveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Boolean {
        return _game.value.moveTableauToTableau(fromIndex, cardIndex, toIndex)
    }

    fun tryAutoMoveWasteToFoundation(): Boolean {
        val game = _game.value

        for (i in game.foundations.indices) {
            val moved = game.moveWasteToFoundation(i)
            if (moved) {
                updateAfterMove(game)
                return true
            }
        }
        return false
    }

    fun tryAutoMoveTableauTopToFoundation(tableauIndex: Int): Boolean {
        val game = _game.value
        val pile = game.tableau[tableauIndex]
        val topIndex = pile.size() - 1

        if (topIndex < 0) return false

        for (i in game.foundations.indices) {
            val moved = game.moveTableauToFoundation(
                tableauIndex,
                topIndex,
                i
            )
            if (moved) {
                updateAfterMove(game)
                return true
            }
        }

        return false
    }

    fun tryMoveWasteToFoundation(index: Int): Boolean {
        val game = _game.value
        val moved = game.moveWasteToFoundation(index)

        if (moved) {
            updateAfterMove(game)
        }

        return moved
    }

    fun tryMoveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Boolean {

        val game = _game.value
        val moved = game.moveTableauToFoundation(
            tableauIndex,
            cardIndex,
            foundationIndex
        )

        if (moved) {
            updateAfterMove(game)
        }

        return moved
    }

    private fun updateAfterMove(game: Game) {
        if (game.isWinCondition()) {
            _game.value = game.copy(status = GameStatus.WON)
        } else {
            _game.value = game
        }
    }

    fun undo() {
        viewModelScope.launch {
            try {
                _game.value = controller.undo(_game.value)
                saveGameIfInProgress()
            } catch (t: Throwable) {
                Log.w("GameViewModel", "undo failed: ${t.message}")
            }
        }
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

//    fun restart() {
//        viewModelScope.launch {
//            _game.value = controller.newGameWithClearHistory()
//            saveGame()
//        }
//    }

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
        val current = _game.value
        val recycled = current.recycleWasteToStock()

        if (recycled) {
            _game.value = current
        }

        return recycled
    }
}
