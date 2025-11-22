package com.gpgamelab.justpatience.model

import com.google.gson.annotations.SerializedName

/**
 * Enumeration of the different types of card stacks on the Solitaire board.
 * This helps the game logic identify where a card is coming from or going to.
 */
enum class StackType {
    @SerializedName("stk") STOCK,
    @SerializedName("wst") WASTE,
    @SerializedName("tab") TABLEAU,
    @SerializedName("fnd") FOUNDATION
}

/**
 * Abstract base class for all card stacks in the game.
 * All concrete piles (Stock, Waste, Tableau, Foundation) must implement this.
 *
 * @property type The identifier for the type of stack.
 * @property cards The mutable list of cards in this stack.
 */
sealed class CardStack(
    @SerializedName("t") open val type: StackType,
    @SerializedName("c") open val cards: MutableList<Card> = mutableListOf()
) {
    /**
     * Attempts to add a card to the top of the stack.
     * Specific validation logic for adding cards is handled in the concrete subclasses.
     * @return True if the card was added, false otherwise.
     */
    open fun addCard(card: Card): Boolean {
        cards.add(card)
        return true
    }

    /**
     * Removes and returns the last (top) card from the stack.
     * @return The top Card, or null if the stack is empty.
     */
    open fun removeCard(): Card? {
        return if (cards.isNotEmpty()) cards.removeAt(cards.lastIndex) else null
    }

    /**
     * Checks if the stack is currently empty.
     */
    fun isEmpty(): Boolean = cards.isEmpty()

    /**
     * Returns the top card of the stack without removing it.
     */
    fun peek(): Card? = cards.lastOrNull()
}

/**
 * Represents the Stock pile (draw pile).
 */
data class Stock(
    @SerializedName("c") override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.STOCK, cards)

/**
 * Represents the Waste pile (cards drawn from the Stock).
 */
data class Waste(
    @SerializedName("c") override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.WASTE, cards) {
    /**
     * Only the top card of the waste pile is face up and available to move.
     * In Solitaire, cards can only be added to the waste during an "undo" operation.
     */
    override fun addCard(card: Card): Boolean {
        if (!card.isFaceUp) card.isFaceUp = true
        return super.addCard(card)
    }
}

/**
 * Represents one of the seven Tableau piles (the main playing area).
 * Tableau piles can contain both face-down and face-up cards.
 */
data class TableauPile(
    @SerializedName("c") override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.TABLEAU, cards) {

    /**
     * Tableau piles allow adding if the card's rank is one less than the top card
     * AND the suit color is opposite. (Logic check in GamePresenter, not here).
     */
    override fun addCard(card: Card): Boolean {
        if (!card.isFaceUp) card.isFaceUp = true
        return super.addCard(card)
    }
}

/**
 * Represents one of the four Foundation piles (Ace-to-King in suit).
 * Foundation piles only accept cards of the same suit in ascending order.
 */
data class FoundationPile(
    @SerializedName("c") override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.FOUNDATION, cards) {

    /**
     * Foundation piles only allow adding the next card in sequence of the same suit.
     * (Logic check in GamePresenter, not here).
     */
    override fun addCard(card: Card): Boolean {
        if (!card.isFaceUp) card.isFaceUp = true
        return super.addCard(card)
    }
}

// --- Move History (Required for Undo) ---

/**
 * Represents a single action taken by the player that modifies the game state.
 * Used to store the state change for the 'Undo' functionality.
 */
sealed class Move {
    /**
     * Move representing a set of cards moving from one stack to another.
     * @param cards The cards that were moved.
     * @param source The stack the cards moved from.
     * @param target The stack the cards moved to.
     * @param cardFlipped True if a face-down card was flipped face-up at the source.
     */
    data class CardMovement(
        @SerializedName("c") val cards: List<Card>,
        @SerializedName("s") val source: CardStack,
        @SerializedName("t") val target: CardStack,
        @SerializedName("f") val cardFlipped: Boolean = false
    ) : Move()

    /**
     * Move representing drawing cards from the Stock to the Waste.
     * @param cardsDrawn The cards moved from Stock to Waste.
     */
    data class DrawCard(
        @SerializedName("d") val cardsDrawn: List<Card>
    ) : Move()

    /**
     * Move representing the Stock being reset (moving all Waste cards back to Stock).
     * @param wasteCards The cards moved from Waste back to Stock.
     */
    data class StockReset(
        @SerializedName("w") val wasteCards: List<Card>
    ) : Move()
}
