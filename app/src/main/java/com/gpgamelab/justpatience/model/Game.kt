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
            val tableauPiles = List(7) { TableauPile() }

            var index = 0
            for (pile in 0 until 7) {
                for (cardIndex in 0..pile) {
                    val card = fullDeck.cards[index++]
                    card.isFaceUp = (cardIndex == pile) // Last card face-up
                    tableauPiles[pile].push(card)
                }
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

    /**
     * Draws one card from stock to waste.
     */
    fun drawFromStock(): Game {
        val newStockCards = stock.asList().toMutableList()
        val newWasteCards = waste.asList().toMutableList()

        val drawn = stock.draw(1)
        if (drawn != null) {
            newWasteCards.addAll(drawn)
        } else {
            // Recycle waste back to stock when empty
            newStockCards.addAll(newWasteCards.map { it.copy(isFaceUp = false) })
            newWasteCards.clear()
        }

        return copy(
            stock = Stock(newStockCards),
            waste = Waste().apply { newWasteCards.forEach { push(it) } },
            moves = moves + 1
        )
    }

//    fun moveWasteToTableau(tableauIndex: Int): Game {
//        val wastePile = waste
//        if (wastePile.isEmpty()) return this
//
//        val card = wastePile.peek()
//        val tableauPile = tableau[tableauIndex]
//
//        if (!canMoveToTableau(card, tableauPile)) {
//            return this
//        }
//
//        val newWaste = wastePile.copy().apply { pop() }
//        val newTableau = tableau.toMutableList().apply {
//            this[tableauIndex] = tableauPile.copy().apply { push(card) }
//        }
//
//        return copy(
//            waste = newWaste,
//            tableau = newTableau,
//            moves = moves + 1
//        )
//    }
//fun moveWasteToTableau(tableauIndex: Int): Game {
//    if (waste.isEmpty()) return this
//
//    val card = waste.last()
//    val targetPile = tableau[tableauIndex]
//
//    if (!canMoveToTableau(card, targetPile)) {
//        return this
//    }
//
//    // Mutate game state
//    waste.removeLast()
//    targetPile.cards.add(card)
//
//    return this
//}
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


    //    private fun canMoveToTableau(card: Card, pile: Stack<Card>): Boolean {
//        if (pile.isEmpty()) {
//            return card.rank == StandardRank.KING
//        }
//
//        val top = pile.peek()
//        if (!top.isFaceUp) return false
//
//        return top.rank.sortOrder == card.rank.sortOrder + 1 &&
//                top.color != card.color
//    }
//private fun canMoveToTableau(
//    card: Card,
//    pile: TableauPile
//): Boolean {
//
//    // Empty tableau → only Kings allowed
//    if (pile.cards.isEmpty()) {
//        return card.rank == StandardRank.KING
//    }
//
//    val topCard = pile.cards.last()
//
//    // Must alternate color
//    if (card.color == topCard.color) {
//        return false
//    }
//
//    // Must be one rank lower
//    return card.rank.sortOrder + 1 == topCard.rank.sortOrder
//}

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

}
