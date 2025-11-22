package com.gpgamelab.justpatience.presenter

import android.util.Log
import com.gpgamelab.justpatience.model.*
import com.gpgamelab.justpatience.model.Rank.*
import com.gpgamelab.justpatience.model.Suit.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log

/**
 * The GamePresenter is responsible for all game logic, rule validation,
 * state management (Game object), move history (Undo), and scoring.
 *
 * @param gson An instance of Gson for serialization/deserialization of the Game state.
 */
class GamePresenter(private val gson: Gson) {

    // --- State Management ---

    private val _gameState = MutableStateFlow(Game.newGame())
    val gameState: StateFlow<Game> = _gameState.asStateFlow()

    /**
     * Exposes the current Game object. Should be used for reading the current state.
     */
    val currentGame: Game
        get() = _gameState.value

    /**
     * Helper list to quickly access all stacks by their type and index.
     * This makes finding stacks by reference easier in move methods.
     */
    private val allStacks: List<CardStack>
        get() = listOf(currentGame.stock, currentGame.waste) +
                currentGame.tableauPiles +
                currentGame.foundationPiles

    // --- Game Initialization and Loading ---

    /**
     * Initializes a brand new game state and resets all history.
     */
    fun startNewGame() {
        Log.i("GamePresenter", "Starting a new game.")
        _gameState.value = Game.newGame()
    }

    /**
     * Loads a game state from a serialized JSON string.
     * @param jsonString The serialized Game state.
     * @return True if loading was successful, false otherwise.
     */
    fun loadGame(jsonString: String): Boolean {
        return try {
            val loadedGame = gson.fromJson(jsonString, Game::class.java)
            _gameState.value = loadedGame
            Log.i("GamePresenter", "Game loaded successfully.")
            true
        } catch (e: Exception) {
            Log.e("GamePresenter", "Error loading game state: ${e.message}", e)
            false
        }
    }

    /**
     * Serializes the current game state into a JSON string for saving.
     */
    fun serializeGame(): String {
        return gson.toJson(currentGame)
    }

    // --- Core Game Actions ---

    /**
     * Draws cards from the Stock pile to the Waste pile.
     * Handles the logic for resetting the Stock when empty.
     */
    fun drawFromStock() {
        val game = currentGame
        val stock = game.stock
        val waste = game.waste

        if (stock.isEmpty()) {
            // Case 1: Stock is empty, reset from Waste
            if (waste.isNotEmpty()) {
                val wasteCards = waste.cards.toMutableList()
                waste.cards.clear()
                stock.cards.addAll(wasteCards.reversed().onEach { it.isFaceUp = false })

                // Record the move: Stock Reset
                recordMove(Move.StockReset(wasteCards))

                // Score: 100 points for resetting the stock (optional rule, but good for scoring)
                updateScore(-100) // Penalty for resetting the stock

                // Update UI state
                updateState()
            }
        } else {
            // Case 2: Draw 1 card from Stock to Waste
            val drawnCard = stock.removeCard()
            if (drawnCard != null) {
                drawnCard.isFaceUp = true
                waste.addCard(drawnCard)

                // Record the move: Draw Card
                recordMove(Move.DrawCard(listOf(drawnCard)))

                // Score: 5 points for moving from Stock to Waste
                updateScore(5)

                // Update UI state
                updateState()
            }
        }
    }

    /**
     * Attempts to move a card (or group of cards) from a source stack to a target stack.
     * This is the central logic for all card movements on the board.
     *
     * @param card The top-most card of the group being moved.
     * @param sourceStack The stack the card is coming from.
     * @param targetStack The stack the card is going to.
     * @param cardIndexInSource The index of the card being moved in the source stack.
     * @return True if the move was successful and the state was updated, false otherwise.
     */
    fun moveCard(card: Card, sourceStack: CardStack, targetStack: CardStack, cardIndexInSource: Int): Boolean {
        // 1. Get the sublist of cards to move
        val cardsToMove: List<Card> = when (sourceStack.type) {
            StackType.TABLEAU -> sourceStack.cards.subList(cardIndexInSource, sourceStack.cards.size)
            else -> listOf(card) // For Stock, Waste, Foundation, always move just the top card
        }

        // 2. Validate the move based on the target stack type
        if (!isValidMove(cardsToMove, targetStack)) {
            Log.d("GamePresenter", "Move validation failed for ${cardsToMove.size} cards.")
            return false
        }

        // 3. Execute the move
        val sourceSizeBeforeMove = sourceStack.cards.size
        var cardFlipped = false

        // a. Remove cards from source
        when (sourceStack.type) {
            StackType.TABLEAU -> {
                // Remove the sublist from the tableau pile
                sourceStack.cards.subList(cardIndexInSource, sourceStack.cards.size).clear()
            }
            StackType.WASTE -> {
                // Remove the top card from waste
                sourceStack.removeCard()
            }
            StackType.FOUNDATION -> {
                // Remove the top card from foundation (only happens on Undo, but we allow it for logic consistency)
                sourceStack.removeCard()
            }
            else -> {
                // Stock is never a source for direct drag moves
                Log.e("GamePresenter", "Invalid source stack for move: ${sourceStack.type}")
                return false
            }
        }

        // b. Check and flip the new top card in the source Tableau pile
        if (sourceStack.type == StackType.TABLEAU && sourceStack.cards.isNotEmpty()) {
            val newTopCard = sourceStack.peek()
            if (newTopCard != null && !newTopCard.isFaceUp) {
                newTopCard.isFaceUp = true
                cardFlipped = true
                // Score: 5 points for flipping a card in the tableau
                updateScore(5)
            }
        }

        // c. Add cards to target
        cardsToMove.forEach { targetStack.addCard(it) }

        // 4. Record the move
        val move = Move.CardMovement(cardsToMove, sourceStack, targetStack, cardFlipped)
        recordMove(move)

        // 5. Update score based on move destination
        updateScoreForMove(sourceStack.type, targetStack.type)

        // 6. Update the UI state
        updateState()

        return true
    }

    /**
     * Validates if a list of cards can be legally placed on the target stack.
     */
    private fun isValidMove(cards: List<Card>, target: CardStack): Boolean {
        val topCardToMove = cards.first()
        val targetCard = target.peek()

        return when (target.type) {
            StackType.FOUNDATION -> {
                // Must move only a single card to Foundation
                if (cards.size > 1) return false

                when (targetCard) {
                    null -> topCardToMove.rank == ACE // Foundation accepts only an Ace on an empty pile
                    else -> topCardToMove.suit == targetCard.suit && topCardToMove.rank.ordinal == targetCard.rank.ordinal + 1
                }
            }
            StackType.TABLEAU -> {
                when (targetCard) {
                    null -> topCardToMove.rank == KING // Tableau accepts only a King on an empty column
                    else -> topCardToMove.isOppositeColor(targetCard) && topCardToMove.rank.ordinal == targetCard.rank.ordinal - 1
                }
            }
            // Waste and Stock are not valid drag/drop targets in Klondike Solitaire
            else -> false
        }
    }

    /**
     * Executes the 'Undo' operation, reverting the last recorded move.
     */
    fun undoLastMove(): Boolean {
        val lastMove = currentGame.moveHistory.removeLastOrNull()
        if (lastMove == null) {
            Log.d("GamePresenter", "No moves to undo.")
            return false
        }

        Log.d("GamePresenter", "Undoing move: $lastMove")

        when (lastMove) {
            is Move.CardMovement -> {
                // Reverse the card movement
                val source = lastMove.source // The stack the cards CAME FROM originally (now TARGET of undo)
                val target = lastMove.target // The stack the cards WENT TO (now SOURCE of undo)
                val cards = lastMove.cards
                val cardFlipped = lastMove.cardFlipped

                // 1. Remove cards from the current target
                // Since cards were added as a block, we can remove them as a block
                for (i in 0 until cards.size) {
                    if (target.cards.isNotEmpty()) {
                        target.cards.removeLast()
                    }
                }

                // 2. Place cards back on the source
                source.cards.addAll(cards)

                // 3. Reverse the card flip if one occurred (only applies to Tableau source)
                if (cardFlipped && source is TableauPile) {
                    val newTopCard = source.peek()
                    if (newTopCard != null) {
                        newTopCard.isFaceUp = false // Flip back down
                        // Score: Undo the flip score change
                        updateScore(-5)
                    }
                }

                // 4. Reverse the move score change
                reverseScoreForMove(lastMove.source.type, lastMove.target.type)
            }

            is Move.DrawCard -> {
                // Reverse the draw (move card(s) from Waste back to Stock)
                lastMove.cardsDrawn.forEach {
                    if (currentGame.waste.isNotEmpty()) {
                        currentGame.waste.removeCard()
                        it.isFaceUp = false
                        currentGame.stock.addCard(it)
                    }
                }
                // Reverse the draw score change
                updateScore(-5)
            }

            is Move.StockReset -> {
                // Reverse the stock reset (move cards back from Stock to Waste)
                val cardsToMove = lastMove.wasteCards.toMutableList().reversed() // They were reversed when put in Stock
                currentGame.stock.cards.clear()
                currentGame.waste.cards.addAll(cardsToMove.onEach { it.isFaceUp = true })

                // Reverse the score change
                updateScore(100) // Un-penalty for resetting the stock
            }
        }

        // 7. Update the UI state
        updateState()
        return true
    }

    // --- Private Helper Methods ---

    /**
     * Records a move to the history list and enforces a maximum history size (optional for memory).
     */
    private fun recordMove(move: Move) {
        val history = currentGame.moveHistory
        history.add(move)
        // Optionally cap history size:
        // while (history.size > 50) history.removeFirst()
    }

    /**
     * Manages the score update based on the move type.
     */
    private fun updateScoreForMove(sourceType: StackType, targetType: StackType) {
        val points = when {
            // Waste to Tableau: 5 points
            sourceType == StackType.WASTE && targetType == StackType.TABLEAU -> 5
            // Waste to Foundation: 10 points
            sourceType == StackType.WASTE && targetType == StackType.FOUNDATION -> 10
            // Tableau to Foundation: 10 points
            sourceType == StackType.TABLEAU && targetType == StackType.FOUNDATION -> 10
            // Foundation to Tableau: -15 points (penalty)
            sourceType == StackType.FOUNDATION && targetType == StackType.TABLEAU -> -15
            else -> 0
        }
        updateScore(points)
    }

    /**
     * Reverses the score change during an undo operation.
     */
    private fun reverseScoreForMove(sourceType: StackType, targetType: StackType) {
        val points = when {
            sourceType == StackType.WASTE && targetType == StackType.TABLEAU -> -5
            sourceType == StackType.WASTE && targetType == StackType.FOUNDATION -> -10
            sourceType == StackType.TABLEAU && targetType == StackType.FOUNDATION -> -10
            sourceType == StackType.FOUNDATION && targetType == StackType.TABLEAU -> 15
            else -> 0
        }
        updateScore(points)
    }

    /**
     * Atomically updates the score of the game and publishes the new state.
     */
    private fun updateScore(points: Int) {
        currentGame.score += points
        // Ensure score doesn't drop below 0 (optional rule)
        if (currentGame.score < 0) currentGame.score = 0
    }

    /**
     * Forces the StateFlow to emit the current Game state, updating the UI.
     * This is crucial after any mutable change within the Game object.
     */
    private fun updateState() {
        _gameState.value = currentGame.copy(
            score = currentGame.score,
            timeSeconds = currentGame.timeSeconds
        ) // Simple copy to force StateFlow emission
    }

    // --- Public Utility Methods for UI/Interactions ---

    /**
     * Returns the stack at a given board position, useful for drag-and-drop UI logic.
     * @param type The type of stack (Tableau, Foundation, etc.)
     * @param index The index of the stack (0-3 for Foundation, 0-6 for Tableau, -1 for others)
     * @return The CardStack or null if the index is out of bounds.
     */
    fun getStack(type: StackType, index: Int = -1): CardStack? {
        return when (type) {
            StackType.STOCK -> currentGame.stock
            StackType.WASTE -> currentGame.waste
            StackType.TABLEAU -> currentGame.tableauPiles.getOrNull(index)
            StackType.FOUNDATION -> currentGame.foundationPiles.getOrNull(index)
        }
    }

    /**
     * Finds the stack containing a specific card instance.
     * This is useful for knowing the source of a drag event.
     * @param card The Card instance to locate.
     * @return The CardStack where the card currently resides, or null.
     */
    fun findStackContainingCard(card: Card): CardStack? {
        return allStacks.firstOrNull { it.cards.contains(card) }
    }
}