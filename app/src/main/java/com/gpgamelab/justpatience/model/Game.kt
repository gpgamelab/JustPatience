package com.gpgamelab.justpatience.model

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
                    tableauPiles[pile].push(card.copy(isFaceUp = (cardIndex == pile)))
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

    fun moveWasteToTableau(tableauIndex: Int): Game? {
        val wasteCard = this.waste.peek() ?: return null
        val tableauPile = tableau.getOrNull(tableauIndex) ?: return null

        // Let TableauPile decide if the move is legal
        if (!tableauPile.canPush(wasteCard)) {
            return null
        }

        // ✅ Create new immutable stacks
        val (newWaste, _) = this.waste.withCardPopped()
        val newTableau = tableau.toMutableList()
        newTableau[tableauIndex] = tableauPile.withCardsAdded(listOf(wasteCard))

        return this.copy(
            waste = newWaste,
            tableau = newTableau
        )
    }

    fun moveTableauToTableau(
        fromIndex: Int,
        cardIndex: Int,
        toIndex: Int
    ): Game? {
        if (fromIndex == toIndex) return null

        val fromPile = tableau.getOrNull(fromIndex) ?: return null
        val toPile = tableau.getOrNull(toIndex) ?: return null

        val count = fromPile.size() - cardIndex
        if (count <= 0) return null

        // 🔒 PEEK FIRST — NO MUTATION
        val seq = fromPile.asList().takeLast(count)

        if (!fromPile.isValidSequence(seq)) return null
        if (!toPile.canPush(seq)) return null

        // ✅ Create new immutable stacks
        val (newFromPile, _) = fromPile.withCardsRemoved(count)
        val newTableau = tableau.toMutableList()
        newTableau[fromIndex] = newFromPile
        newTableau[toIndex] = toPile.withCardsAdded(seq)

        // Auto-flip
        val flippedFromPile = newFromPile.withTopCardFlipped()
        newTableau[fromIndex] = flippedFromPile

        return this.copy(tableau = newTableau)
    }

    fun moveWasteToFoundation(foundationIndex: Int): Game? {
        val foundationPile = foundations.getOrNull(foundationIndex) ?: return null
        val card = waste.peek() ?: return null

        // push() validates suit + rank progression
        if (!foundationPile.canPush(card)) {
            return null
        }

        // ✅ Create new immutable stacks
        val (newWaste, _) = this.waste.withCardPopped()
        val newFoundations = foundations.toMutableList()
        newFoundations[foundationIndex] = foundationPile.withCardAdded(card)

        return this.copy(
            waste = newWaste,
            foundations = newFoundations
        )
    }

    fun moveTableauToFoundation(
        tableauIndex: Int,
        cardIndex: Int,
        foundationIndex: Int
    ): Game? {
        val fromPile = tableau.getOrNull(tableauIndex) ?: return null
        val toPile = foundations.getOrNull(foundationIndex) ?: return null

        // MUST be top card
        if (cardIndex != fromPile.size() - 1) return null

        val card = fromPile.peekAt(cardIndex) ?: return null

        if (!toPile.canPush(card)) return null

        // ✅ Create new immutable stacks
        val (newFromPile, _) = fromPile.withCardsRemoved(1)
        val newFoundations = foundations.toMutableList()
        newFoundations[foundationIndex] = toPile.withCardAdded(card)

        val newTableau = tableau.toMutableList()
        newTableau[tableauIndex] = newFromPile

        // Auto-flip
        val flippedFromPile = newFromPile.withTopCardFlipped()
        newTableau[tableauIndex] = flippedFromPile

        return this.copy(
            tableau = newTableau,
            foundations = newFoundations
        )
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

    fun recycleWasteToStock(): Game? {
        if (!stock.isEmpty() || waste.isEmpty()) return null

        val (newWaste, recycled) = waste.withAllCardsTaken()
        if (recycled == null) return null

        // Reverse order (top waste card becomes last stock card)
        val cardsToStock = recycled.reversed().map { it.copy(isFaceUp = false) }
        val newStock = stock.withCardsAdded(cardsToStock)

        return this.copy(
            stock = newStock,
            waste = newWaste
        )
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

