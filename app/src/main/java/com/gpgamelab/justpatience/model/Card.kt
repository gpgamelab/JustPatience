package com.gpgamelab.justpatience.model

/**
 * Represents a single playing card, composed of a Recto (front) and Verso (back).
 *
 * @property recto Front-facing card metadata: rank, suit, color profile and face image.
 * @property verso Card back metadata: image path.
 * @property isFaceUp True if the front of the card is currently visible.
 */
data class Card(
    val rank: CardRank,
    val suit: CardSuit?, // Null for Joker
    val recto: Recto,
    val verso: Verso,
    var isFaceUp: Boolean = false
) {

    /** True for ranks Ace–10 in the standard deck. */
    val isOrdinalCard: Boolean
        get() = when (recto.rank) {
            is StandardRank -> recto.rank.sortOrder in 1..10
            else -> false
        }

    /** True for J, Q, K. */
    val isCourtCard: Boolean
        get() = when (recto.rank) {
            is StandardRank -> recto.rank.sortOrder in 11..13
            else -> false
        }

    /** True for Joker cards. */
    val isJokerCard: Boolean
        get() = recto.rank is Joker

    /** Display rank abbreviation (A, 2, J, K, JK). */
    val displayRank: String
        get() = recto.rank.abbreviation

    /** True when the card uses a light suit color. */
    fun isLight(): Boolean =
        recto.suitColor == CardSuit.CardColor.LIGHT

    /** True when the card uses a dark suit color. */
    fun isDark(): Boolean =
        recto.suitColor == CardSuit.CardColor.DARK

    /**
     * Checks whether this card's suit color differs from another card's suit color.
     */
    fun isSameColor(other: Card): Boolean =
        this.recto.hasSameColor(other.recto)
    fun isOppositeColor(other: Card): Boolean =
        isSameColor(other).not()
}
