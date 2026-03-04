package com.gpgamelab.justpatience.model

import android.util.Log
import java.io.Serializable

/**
 * The core data model representing the entire state of a game of Solitaire (Patience).
 * This model is pure data — no UI. Serialized and suitable for saving/loading.
 *
 * @property stock The stock pile.
 * @property waste The waste pile.
 * @property tableau The 7 main columns of cards.
 * @property foundations The 4 piles where cards are built up by cardSuit.
 * @property status The current state of the game.
 * @property score The current score.
 * @property moves The number of moves made.
 * @property savedGameTime TBD.
 */
data class Game(
    val stock: Stock,
    val waste: Waste,
    val tableau: List<TableauPile>,
    val foundations: List<FoundationPile>,
    val status: GameStatus,
    val score: Int,
    val moves: Int,
    val savedGameTime: Long
) : Serializable {

    companion object {

        /**
         * Creates and deals a brand-new game of Solitaire using 1 deck, no Jokers.
         */
        fun newGame(): Game {
            val fullDeck = FullDeck(deckCount = 1, includeJokers = false)
            fullDeck.shuffle()

            // Tableau: 7 piles with increasing number of cards.
            val tableauPiles = List(7) { TableauPile().apply { setDealInProgress() } }

            var index = 0
            for (pile in 0 until 7) {
                for (cardIndex in 0..pile) {
                    val card = fullDeck.cards[index++]
                    card.isFaceUp = (cardIndex == pile) // Last card face-up
                    tableauPiles[pile].push(card)
                }
            }
            for (pile in 0 until 7) {
                tableauPiles[pile].clearDealInProgress()
            }

            // Remaining cards go to stock (face-down)
            val stockCards = fullDeck.cards.drop(index).toMutableList()
            val stock = Stock(stockCards)

            return Game(
                stock = stock,
                waste = Waste(),
                tableau = tableauPiles,
                foundations = List(4) { FoundationPile() },
                status = GameStatus.IN_PROGRESS,
                score = 0,
                moves = 0,
                savedGameTime = System.currentTimeMillis()
            )
        }
    }

    fun moveWasteToTableau(tableauIndex: Int): Game {
        val waste = this.waste
        val tableauPile = tableau.getOrNull(tableauIndex) ?: return this

        val card = waste.peek() ?: return this

        // Let TableauPile decide if the move is legal
        if (!tableauPile.canPush(card)) {
            return this
        }

        // Mutate via CardStack API ONLY
        val moved = waste.pop() ?: return this
        tableauPile.push(moved)

        return this
    }

    fun moveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Boolean {
        if (fromIndex == toIndex) return false

        val fromPile = tableau[fromIndex]
        val toPile = tableau[toIndex]

        val count = fromPile.size() - cardIndex
        if (count <= 0) return false

        // 🔒 PEEK FIRST — NO MUTATION
        val seq = fromPile.asList().takeLast(count)

        if (!fromPile.isValidSequence(seq)) return false
        if (!toPile.canPush(seq)) return false

        // ✅ NOW MUTATE (SAFE)
        fromPile.take(count)
        toPile.push(seq)

        // Auto-flip
        fromPile.peek()?.let {
            if (!it.isFaceUp) it.isFaceUp = true
        }

        return true
    }

    fun moveWasteToFoundation(foundationIndex: Int): Boolean {
        val foundationPile = foundations.getOrNull(foundationIndex) ?: return false
        val card = waste.peek() ?: return false

        // push() validates suit + rank progression
        if (!foundationPile.push(card)) {
            return false
        }

        waste.pop()
        return true
    }

    fun moveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Boolean {

        val fromPile = tableau[tableauIndex]
        val toPile = foundations[foundationIndex]

        // MUST be top card
        if (cardIndex != fromPile.size() - 1) return false

        val card = fromPile.peekAt(cardIndex) ?: return false

        if (!toPile.canPush(card)) return false

        fromPile.popFrom(cardIndex)
        toPile.push(card)

        // Auto-flip
        fromPile.peek()?.let {
            if (!it.isFaceUp) it.isFaceUp = true
        }

        return true
    }

    /**
     * Convenience: checks whether all foundation piles are complete.
     */
    fun isWinCondition(): Boolean {
        return foundations.all { pile ->
            pile.size() == 13
        }
    }

    private fun faceImagePath(rank: CardRank, suit: CardSuit?): String {
        if (rank == Joker) {
            return "j_${if (Math.random() < 0.5) "da" else "li"}"
        }

        val suitChar = when (suit) {
            CardSuit.HEARTS -> "h"
            CardSuit.DIAMONDS -> "d"
            CardSuit.SPADES -> "s"
            CardSuit.CLUBS -> "c"
            else -> error("Invalid suit")
        }

        val rankCode = when (rank) {
            StandardRank.ACE -> "ac"
            StandardRank.JACK -> "ja"
            StandardRank.QUEEN -> "qu"
            StandardRank.KING -> "ki"
            else -> rank.sortOrder.toString().padStart(2, '0')
        }

        return "${suitChar}_${rankCode}"
    }

    fun recycleWasteToStock(): Boolean {
        if (!stock.isEmpty() || waste.isEmpty()) return false

        val recycled = waste.take(waste.size()) ?: return false

        // Reverse order (top waste card becomes last stock card)
        recycled.reversed().forEach { card ->
            card.isFaceUp = false
            stock.push(card)
        }

        return true
    }

    fun deepCopy(): Game {

        val newTableau = tableau.map { it.deepCopy() }.toMutableList()
        val newFoundations = foundations.map { it.deepCopy() }.toMutableList()

        return Game(
            stock = stock.deepCopy(),
            waste = waste.deepCopy(),
            tableau = newTableau,
            foundations = newFoundations,
            status = status,
            score = score,
            moves = moves,
            savedGameTime = savedGameTime
        )
    }
}

