package com.gpgamelab.justpatience.model

import com.google.gson.annotations.SerializedName
import java.util.Collections

/**
 * A data class representing the complete state of a Klondike Solitaire game.
 * This class holds all the card stacks and the move history, making it the
 * primary object to be serialized/deserialized for saving and loading games.
 *
 * @property stock The draw pile.
 * @property waste The discard pile.
 * @property foundationPiles The four piles where cards are built A-K by suit.
 * @property tableauPiles The seven main playing columns.
 * @property moveHistory A list of all moves made, used for the 'Undo' feature.
 * @property score The current score of the game.
 * @property timeSeconds The time elapsed since the game started.
 */
data class Game(
    // Stacks
    @SerializedName("stk") val stock: Stock = Stock(),
    @SerializedName("wst") val waste: Waste = Waste(),
    @SerializedName("fnd") val foundationPiles: List<FoundationPile> = List(4) { FoundationPile() },
    @SerializedName("tab") val tableauPiles: List<TableauPile> = List(7) { TableauPile() },

    // Game Metrics
    @SerializedName("scr") var score: Int = 0,
    @SerializedName("tim") var timeSeconds: Long = 0,

    // Undo/Redo mechanism
    @SerializedName("mvs") val moveHistory: MutableList<Move> = mutableListOf()
) {
    companion object {
        /**
         * Initializes a new game of Klondike Solitaire.
         * Creates a shuffled deck and deals the cards into the Tableau piles.
         *
         * @return A new Game instance ready to play.
         */
        fun newGame(): Game {
            val fullDeck = Card.fullDeck().toMutableList()
            // Shuffle the deck 7 times for good measure
            repeat(7) { Collections.shuffle(fullDeck) }

            val game = Game()

            // 1. Deal cards to Tableau Piles
            for (i in 0..6) {
                val tableau = game.tableauPiles[i]
                for (j in 0..i) {
                    val card = fullDeck.removeAt(0)
                    // Only the last card in each column is face up
                    if (j == i) {
                        card.isFaceUp = true
                    }
                    tableau.cards.add(card)
                }
            }

            // 2. Put remaining cards into the Stock
            game.stock.cards.addAll(fullDeck)

            return game
        }
    }

    /**
     * Checks if the game is currently won (all cards in the Foundation piles).
     */
    fun isWon(): Boolean {
        // A win state occurs when the sum of cards in the foundation piles is 52.
        return foundationPiles.sumOf { it.cards.size } == 52
    }
}