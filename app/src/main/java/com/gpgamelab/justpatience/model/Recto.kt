package com.gpgamelab.justpatience.model

import com.gpgamelab.justpatience.model.CardSuit.CardColor

data class Recto(
    var rank: CardRank,
    var suit: CardSuit?, // Null for Joker
    var imagePath: String,
    var colorProfile: CardColor
) {
    // get the suit color; a Joker has no suit color
    val suitColor: CardSuit.CardColor?
        get() = suit?.defaultColor

    fun longLabel(): String =
        when (rank) {
            Joker -> rank.displayName
            else -> "${rank.displayName} of ${suit?.displayName}"
        }

    fun shortLabel(): String =
        when (rank) {
            Joker -> rank.abbreviation
            else -> "${rank.abbreviation}${suit?.abbreviation}"
        }

    fun encodedFileLabel(): String =
        when (rank) {
            Joker -> rank.abbreviation.lowercase()
            else -> "${rank.abbreviation.lowercase()}_${suit?.abbreviation?.lowercase()}"
        }

    /**
     * Checks whether this card's suit color differs from another card's suit color.
     */
    fun hasSameColor(other: Recto): Boolean =
        when (this.rank) {
            Joker -> true
            else -> this.suitColor == other.suitColor
        }

    fun hasOppositeColor(other: Recto): Boolean =
        hasSameColor(other).not()

    fun isOneHigherRank(other: Recto): Boolean =
        when (this.rank) {
            Joker -> true
            else -> this.rank.sortOrder >= 2 && this.rank.sortOrder - 1 == other.rank.sortOrder
        }

    fun isOneLowerRank(other: Recto): Boolean =
        when (this.rank) {
            Joker -> true
            else -> this.rank.sortOrder <= 12 && this.rank.sortOrder + 1 == other.rank.sortOrder
        }
}
