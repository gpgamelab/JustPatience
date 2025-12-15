package com.gpgamelab.justpatience.model

import com.gpgamelab.justpatience.model.CardSuit.CardColor

enum class StackType {
    STOCK,
    WASTE,
    TABLEAU,
    FOUNDATION
}

/**
 * Represents one or more full 52-card (or 54-card with Jokers) decks.
 */
data class FullDeck(
    val deckCount: Int = 1,
    val includeJokers: Boolean = false,
    val cards: MutableList<Card> = buildDeck(deckCount, includeJokers)
) {
    companion object {
        const val MIN_DECKS = 1
        const val MAX_DECKS = 8
    }

    init {
        require(deckCount in MIN_DECKS..MAX_DECKS) {
            "deckCount must be between $MIN_DECKS and $MAX_DECKS (was $deckCount)"
        }
    }

    /** Randomly shuffles the deck multiple times based on deck size. */
    fun shuffle() {
        val deckSize = cards.size
        val requiredShuffles = maxOf(3, kotlin.math.ceil(kotlin.math.sqrt(deckSize.toDouble())).toInt())

        repeat(requiredShuffles) {
            cards.shuffle()
        }
    }

    /** Removes the top card and returns it, or null if empty. */
    fun drawCard(): Card? = cards.removeLastOrNull()

}

// Helper function for building the FullDeck
private fun buildDeck(
    deckCount: Int,
    includeJokers: Boolean
): MutableList<Card> {

    val result = mutableListOf<Card>()

    repeat(deckCount) {
        // Build the standard 52 cards
        for (suit in CardSuit.entries) {
            for (rank in StandardRank.entries) {
                result.add(Card(rank, suit, Recto(rank, suit, faceImagePath(rank, suit), suit.defaultColor), Verso(defaultBackImagePath())))
            }
        }

        // Optionally add Jokers
        if (includeJokers) {
            result.add(Card(Joker, null, Recto(Joker, null, "drawable:j_li", CardColor.LIGHT), Verso(defaultBackImagePath())))
            result.add(Card(Joker, null, Recto(Joker, null, "drawable:j_da", CardColor.DARK), Verso(defaultBackImagePath())))
        }
    }

    return result
}

private fun faceImagePath(rank: CardRank, suit: CardSuit?): String {
    val suitCode = when (suit) {
        CardSuit.HEARTS -> "h"
        CardSuit.DIAMONDS -> "d"
        CardSuit.CLUBS -> "c"
        CardSuit.SPADES -> "s"
        else -> error("Non-joker card must have suit")
    }

    val rankCode = when (rank) {
        StandardRank.ACE -> "ac"
        StandardRank.JACK -> "ja"
        StandardRank.QUEEN -> "qu"
        StandardRank.KING -> "ki"
        else -> rank.sortOrder.toString().padStart(2, '0')
    }

    return "drawable:${suitCode}_${rankCode}"
}
private fun defaultBackImagePath(): String =
    "drawable:b_0001"

/**
 * Base class for all board piles.
 */
sealed class CardStack(
    open val type: StackType,
    protected open val cards: MutableList<Card> = mutableListOf()
) {
    open fun canPush(card: Card): Boolean = true
    open fun canPush(cards: List<Card>): Boolean = cards.size == 1 && canPush(cards.first())
    open fun canPop(): Boolean = cards.isNotEmpty()

    open fun push(card: Card): Boolean {
        return if (canPush(card)) {
            cards.add(card)
            true
        } else false
    }

    open fun push(cards: List<Card>): Boolean {
        return if (canPush(cards)) {
            this.cards.addAll(cards)
            true
        } else false
    }

    open fun pop(): Card? =
        if (canPop()) cards.removeAt(cards.lastIndex) else null

    /** Removes N cards from the end (tableau sequences) */
    open fun take(count: Int): List<Card>? =
        if (count in 1..cards.size) {
            val taken = cards.takeLast(count)
            repeat(count) { cards.removeAt(cards.lastIndex) }
            taken
        } else null

    fun peek(): Card? = cards.lastOrNull()
    fun isEmpty(): Boolean = cards.isEmpty()
    fun size(): Int = cards.size
    fun asList(): List<Card> = cards.toList()
}

class Stock(cards: MutableList<Card>) : CardStack(StackType.STOCK, cards) {

    override fun canPush(card: Card): Boolean = false       // No pushing during play
    override fun canPush(cards: List<Card>): Boolean = false
    override fun canPop(): Boolean = cards.isNotEmpty()

    /** Draws one card (or more if your rules allow it) */
    fun draw(count: Int = 1): List<Card>? =
        take(count)
}

class Waste : CardStack(StackType.WASTE) {

    override fun canPush(card: Card): Boolean = true
    override fun canPush(cards: List<Card>): Boolean = cards.size == 1

    override fun canPop(): Boolean = cards.isNotEmpty()

    override fun push(card: Card): Boolean {
        card.isFaceUp = true
        return super.push(card)
    }
}

class FoundationPile(
    override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.FOUNDATION, cards) {

    override fun push(card: Card): Boolean {
        card.isFaceUp = true
        return super.push(card)
    }
}

class TableauPile(
    override val cards: MutableList<Card> = mutableListOf()
) : CardStack(StackType.TABLEAU, cards) {

    override fun canPush(card: Card): Boolean {
        val top = peek()

        return when (top) {
            null -> card.recto.rank == StandardRank.KING        // Only Kings on empty tableau
            else ->
                card.recto.hasOppositeColor(top.recto)  &&
                        card.recto.isOneLowerRank(top.recto)
        }
    }

    override fun canPush(cards: List<Card>): Boolean {
        if (cards.isEmpty()) return false
        return canPush(cards.first())            // Validate top card of moving sequence
    }

    /** Validates that the moving sequence is internally correct (descending + alternating) */
    fun isValidSequence(seq: List<Card>): Boolean {
        if (seq.size < 2) return true
        for (i in 0 until seq.size - 1) {
            val upper = seq[i]
            val lower = seq[i + 1]

            if (upper.recto.hasSameColor(lower.recto)) return false
            if (upper.recto.isOneHigherRank(lower.recto)) return false
        }
        return true
    }

    override fun push(card: Card): Boolean {
        card.isFaceUp = false
        return super.push(card)
    }

    override fun take(count: Int): List<Card>? {
        val seq = super.take(count) ?: return null
        return if (isValidSequence(seq)) seq else null
    }
}
