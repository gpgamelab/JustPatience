package com.gpgamelab.justpatience.model

/**
 * Represents the four suits in a standard deck of cards.
 * The 'color' property is useful for Solitaire move validation (alternating colors).
 */

enum class CardSuit(
    val displayName: String,
    val abbreviation: String,
    val defaultColor: CardColor
) {
    HEARTS("Hearts", "H", CardColor.LIGHT),
    DIAMONDS("Diamonds", "D", CardColor.LIGHT),
    CLUBS("Clubs", "C", CardColor.DARK),
    SPADES("Spades", "S", CardColor.DARK);

    enum class CardColor {
        LIGHT, DARK
    }
}
