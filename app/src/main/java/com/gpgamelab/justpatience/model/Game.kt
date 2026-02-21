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

//    /**
//     * Draws one card from stock to waste.
//     */
//    fun drawFromStock(): Game {
//        val newStockCards = stock.asList().toMutableList()
//        val newWasteCards = waste.asList().toMutableList()
//
//        val drawn = stock.draw(1)
//        if (drawn != null) {
//            newWasteCards.addAll(drawn)
//        } else {
//            // Recycle waste back to stock when empty
//            newStockCards.addAll(newWasteCards.map { it.copy(isFaceUp = false) })
//            newWasteCards.clear()
//        }
//
//        return copy(
//            stock = Stock(newStockCards),
//            waste = Waste().apply { newWasteCards.forEach { push(it) } },
//            moves = moves + 1
//        )
//    }

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

    //    fun moveTableauToTableau(
//        fromIndex: Int,
//        cardIndex: Int,
//        toIndex: Int
//    ): Game {
//        if (fromIndex == toIndex) return this
//
//        val fromPile = tableau[fromIndex]
//        val toPile = tableau[toIndex]
//
//        val temp = mutableListOf<Card>()
//
//        // Pop cards until we reach the dragged card
//        while (fromPile.size() > cardIndex) {
//            val c = fromPile.pop() ?: break
//            temp.add(c)
//        }
//
//        if (temp.isEmpty()) {
//            // Nothing moved — restore
//            temp.reversed().forEach { fromPile.push(it) }
//            return this
//        }
//
//        val firstCard = temp.last()
//
//        // Test legality by trying to push
//        if (!toPile.push(firstCard)) {
//            // Illegal — restore source pile
//            temp.reversed().forEach { fromPile.push(it) }
//            return this
//        }
//
//        // Legal — push remaining cards
//        for (i in temp.size - 2 downTo 0) {
//            toPile.push(temp[i])
//        }
//
//        // Auto-flip source pile
//        fromPile.peek()?.let {
//            if (!it.isFaceUp) it.isFaceUp = true
//        }
//
//        return this
//    }
//fun moveTableauToTableau(
//    fromIndex: Int,
//    cardIndex: Int,
//    toIndex: Int
//): Boolean {
//    if (fromIndex == toIndex) return false
//
//    val fromPile = tableau[fromIndex]
//    val toPile = tableau[toIndex]
//
//    val temp = mutableListOf<Card>()
//
//    while (fromPile.size() > cardIndex) {
//        val c = fromPile.pop() ?: break
//        temp.add(c)
//    }
//
//    if (temp.isEmpty()) {
//        temp.reversed().forEach { fromPile.push(it) }
//        return false
//    }
//
//    val firstCard = temp.last()
//
//    if (!toPile.push(firstCard)) {
//        temp.reversed().forEach { fromPile.push(it) }
//        return false
//    }
//
//    for (i in temp.size - 2 downTo 0) {
//        toPile.push(temp[i])
//    }
//
//    fromPile.peek()?.let {
//        if (!it.isFaceUp) it.isFaceUp = true
//    }
//
//    return true
//}
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

    //    fun moveTableauToFoundation(
//        tableauIndex: Int,
//        cardIndex: Int,
//        foundationIndex: Int
//    ): Boolean {
////        val tableauPile = tableau.getOrNull(tableauIndex) ?: return this
////        val foundationPile = foundations.getOrNull(foundationIndex) ?: return this
////
////        val card = tableauPile.peek() ?: return this
////
////        if (!foundationPile.push(card)) {
////            return this
////        }
////
////        tableauPile.pop()
////
////        // Auto-flip tableau
////        tableauPile.peek()?.let {
////            if (!it.isFaceUp) it.isFaceUp = true
////        }
////
////        return this
//        val fromPile = tableau[tableauIndex]
//        val toPile = foundations[foundationIndex]
//
//        // MUST be top card
//        if (cardIndex != fromPile.size() - 1) return false
//
//        val card = fromPile.peekAt(cardIndex) ?: return false
//
//        if (!toPile.canPush(card)) return false
//
//        // Remove exactly ONE card
//        fromPile.popFrom(cardIndex)
//
//        toPile.push(card)
//
//        // Auto-flip source pile
//        fromPile.peek()?.let {
//            if (!it.isFaceUp) it.isFaceUp = true
//        }
//
//        return true
//    }
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

}
