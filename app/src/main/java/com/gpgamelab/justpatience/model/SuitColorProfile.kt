package com.gpgamelab.justpatience.model

data class SuitColorProfile(
    val heartsColor: CardSuit.CardColor = CardSuit.CardColor.LIGHT,
    val diamondsColor: CardSuit.CardColor = CardSuit.CardColor.LIGHT,
    val clubsColor: CardSuit.CardColor = CardSuit.CardColor.DARK,
    val spadesColor: CardSuit.CardColor = CardSuit.CardColor.DARK,
    val jokerColor: CardSuit.CardColor = CardSuit.CardColor.LIGHT
) {
    fun colorFor(suit: CardSuit?): CardSuit.CardColor =
        when (suit) {
            CardSuit.HEARTS -> heartsColor
            CardSuit.DIAMONDS -> diamondsColor
            CardSuit.CLUBS -> clubsColor
            CardSuit.SPADES -> spadesColor
            null -> jokerColor
        }
}
