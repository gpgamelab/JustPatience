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
    val windowsScore: Int = score,
    val vegasScore: Int = 0,
    val vegasCumulativeBase: Int = 0,
    val vegasCumulativeScore: Int = 0,
    val completionPercentage: Int = 0,
    val moves: Int,
    val savedGameTime: Long,
    val recycleCountUsed: Int = 0,
    val extraTableauUnlocked: Boolean = false,
    val deckCount: Int = DEFAULT_DECK_COUNT
) : Serializable {

    companion object {
        const val DEFAULT_DECK_COUNT = 1
        const val MIN_DECK_COUNT = 1
        const val MAX_DECK_COUNT = 2

        const val LOCKED_TABLEAU_INDEX = 0
        const val TOTAL_TABLEAU_PILES_1_DECK = 8
        const val DEALT_TABLEAU_COUNT_1_DECK = 7
        const val FOUNDATION_COUNT_1_DECK = 4

        const val TOTAL_TABLEAU_PILES_2_DECK = 10
        const val DEALT_TABLEAU_COUNT_2_DECK = 9
        const val FOUNDATION_COUNT_2_DECK = 8

        // Fixed suit order for reserved foundation slots (index order).
        // 1-deck: Spades, Hearts, Diamonds, Clubs
        // 2-deck: Spades, Hearts, Diamonds, Clubs, Spades, Hearts, Diamonds, Clubs
        private val FOUNDATION_RESERVED_SUIT_SEQUENCE = listOf(
            CardSuit.SPADES,
            CardSuit.HEARTS,
            CardSuit.DIAMONDS,
            CardSuit.CLUBS
        )

        fun normalizeDeckCount(rawDeckCount: Int): Int {
            return if (rawDeckCount == 2) 2 else 1
        }

        fun totalTableauPilesFor(deckCount: Int): Int {
            return if (normalizeDeckCount(deckCount) == 2) TOTAL_TABLEAU_PILES_2_DECK else TOTAL_TABLEAU_PILES_1_DECK
        }

        fun dealtTableauCountFor(deckCount: Int): Int {
            return if (normalizeDeckCount(deckCount) == 2) DEALT_TABLEAU_COUNT_2_DECK else DEALT_TABLEAU_COUNT_1_DECK
        }

        fun foundationCountFor(deckCount: Int): Int {
            return if (normalizeDeckCount(deckCount) == 2) FOUNDATION_COUNT_2_DECK else FOUNDATION_COUNT_1_DECK
        }

        fun reservedFoundationSuitForIndex(index: Int): CardSuit {
            val safeIndex = index.coerceAtLeast(0)
            return FOUNDATION_RESERVED_SUIT_SEQUENCE[safeIndex % FOUNDATION_RESERVED_SUIT_SEQUENCE.size]
        }

        fun newFoundationPilesForDeckCount(deckCount: Int): List<FoundationPile> {
            val count = foundationCountFor(deckCount)
            return List(count) { index ->
                FoundationPile(reservedSuit = reservedFoundationSuitForIndex(index))
            }
        }

        /**
         * Creates and deals a brand-new game of Solitaire using 1 deck, no Jokers.
         */
        fun newGame(deckCount: Int = DEFAULT_DECK_COUNT): Game {
            val normalizedDeckCount = normalizeDeckCount(deckCount)
            val fullDeck = FullDeck(deckCount = normalizedDeckCount, includeJokers = false)
            fullDeck.shuffle()

            val totalTableauPiles = totalTableauPilesFor(normalizedDeckCount)
            val dealtTableauCount = dealtTableauCountFor(normalizedDeckCount)
            val foundationCount = foundationCountFor(normalizedDeckCount)

            // Tableau: index 0 starts locked/empty; deal into indices 1..dealtTableauCount.
            val tableauPiles = List(totalTableauPiles) { TableauPile().apply { setDealInProgress() } }

            var index = 0
            for (dealtOffset in 0 until dealtTableauCount) {
                val pile = dealtOffset + 1
                for (cardIndex in 0..dealtOffset) {
                    val card = fullDeck.cards[index++]
                    tableauPiles[pile].push(card.copy(isFaceUp = (cardIndex == dealtOffset)))
                }
            }
            for (pile in 0 until totalTableauPiles) {
                tableauPiles[pile].clearDealInProgress()
            }

            // Remaining cards go to stock (face-down)
            val stockCards = fullDeck.cards.drop(index).toMutableList()
            val stock = Stock(stockCards)

            return Game(
                stock = stock,
                waste = Waste(),
                tableau = tableauPiles,
                foundations = newFoundationPilesForDeckCount(normalizedDeckCount),
                status = GameStatus.IN_PROGRESS,
                score = 0,
                windowsScore = 0,
                vegasScore = -52 * normalizedDeckCount,
                vegasCumulativeBase = 0,
                vegasCumulativeScore = -52 * normalizedDeckCount,
                completionPercentage = 0,
                moves = 0,
                savedGameTime = System.currentTimeMillis(),
                recycleCountUsed = 0,
                extraTableauUnlocked = false,
                deckCount = normalizedDeckCount
            )
        }
    }

    private fun isLockedTableau(index: Int): Boolean {
        return !extraTableauUnlocked && index == LOCKED_TABLEAU_INDEX
    }

    fun moveWasteToTableau(tableauIndex: Int): Game? {
        if (isLockedTableau(tableauIndex)) return null
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
        if (isLockedTableau(fromIndex) || isLockedTableau(toIndex)) return null

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
        if (isLockedTableau(tableauIndex)) return null
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

    fun moveFoundationToTableau(
        foundationIndex: Int,
        tableauIndex: Int
    ): Game? {
        if (isLockedTableau(tableauIndex)) return null
        val fromPile = foundations.getOrNull(foundationIndex) ?: return null
        val toPile = tableau.getOrNull(tableauIndex) ?: return null

        val card = fromPile.peek() ?: return null
        if (!toPile.canPush(card)) return null

        val (newFoundationPile, removedCard) = fromPile.withCardRemoved()
        if (removedCard == null) return null

        val newFoundations = foundations.toMutableList()
        newFoundations[foundationIndex] = newFoundationPile

        val newTableau = tableau.toMutableList()
        newTableau[tableauIndex] = toPile.withCardsAdded(listOf(removedCard))

        return this.copy(
            foundations = newFoundations,
            tableau = newTableau
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

    fun foundationCardsCount(): Int = foundations.sumOf { it.size() }

    fun scoreForMethod(scoreMethod: String): Int {
        return when (ScoreMethod.normalize(scoreMethod)) {
            ScoreMethod.VEGAS -> vegasScore
            ScoreMethod.VEGAS_CUMULATIVE -> vegasCumulativeScore
            ScoreMethod.COMPLETION -> completionPercentage
            else -> windowsScore
        }
    }

    private fun faceImagePath(rank: CardRank, suit: CardSuit?): String {
        if (rank == Joker) {
            return "j_${if (Math.random() < 0.5) "da" else "li"}"
        }

        val suitChar = when (suit) {
            CardSuit.HEARTS -> "hearts"
            CardSuit.DIAMONDS -> "diamonds"
            CardSuit.SPADES -> "spades"
            CardSuit.CLUBS -> "clubs"
            else -> error("Invalid suit")
        }

        val rankCode = when (rank) {
            StandardRank.ACE -> "ace"
            StandardRank.JACK -> "jack"
            StandardRank.QUEEN -> "queen"
            StandardRank.KING -> "king"
            else -> rank.sortOrder.toString()
        }

        return "ic_${suitChar}_${rankCode}"
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
            windowsScore = windowsScore,
            vegasScore = vegasScore,
            vegasCumulativeBase = vegasCumulativeBase,
            vegasCumulativeScore = vegasCumulativeScore,
            completionPercentage = completionPercentage,
            moves = moves,
            savedGameTime = savedGameTime,
            recycleCountUsed = recycleCountUsed,
            extraTableauUnlocked = extraTableauUnlocked,
            deckCount = deckCount
        )
    }
}
