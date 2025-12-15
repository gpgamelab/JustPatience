package com.gpgamelab.justpatience.model

sealed interface CardRank {
    val displayName: String
    val abbreviation: String
    val sortOrder: Int
}

object Joker : CardRank {
    override val displayName = "Joker"
    override val abbreviation = "JK"
    override val sortOrder = 0
}

enum class StandardRank(
    override val displayName: String,
    override val abbreviation: String,
    override val sortOrder: Int
) : CardRank {
    ACE("Ace", "A", 1),
    TWO("Two", "2", 2),
    THREE("Three", "3", 3),
    FOUR("Four", "4", 4),
    FIVE("Five", "5", 5),
    SIX("Six", "6", 6),
    SEVEN("Seven", "7", 7),
    EIGHT("Eight", "8", 8),
    NINE("Nine", "9", 9),
    TEN("Ten", "10", 10),
    JACK("Jack", "J", 11),
    QUEEN("Queen", "Q", 12),
    KING("King", "K", 13);
}
