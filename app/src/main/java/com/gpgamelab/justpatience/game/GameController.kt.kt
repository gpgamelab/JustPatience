// TODO: deprecated – replaced by Game + GameViewModel
//       either delete this file or refactor other code to be here instead.
//       need to analize and decide this later.

package com.gpgamelab.justpatience.game

import com.gpgamelab.justpatience.model.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pure game logic / rules engine.
 *
 * Operates on a Game instance and returns a new Game instance when state changes.
 * Designed to be called from a ViewModel (single-threaded/coroutine context).
 */
class GameController {

    // Mutex for thread-safety if called from multiple coroutines
    private val mutex = Mutex()

    suspend fun newGameWithClearHistory(): Game = mutex.withLock {
        val g = Game.newGame()
        g
    }

    /**
     * Attempt moving cards from a source stack to a target stack.
     *
     * @param game current game
     * @param sourceType stack type of source
     * @param sourceIndex stack index (tableau: 0..6, foundation: 0..3, others ignored)
     * @param cardIndexInSource index of the card inside that stack to start moving (for tableau sequences)
     * @param targetType destination stack type
     * @param targetIndex destination index (for tableau/foundation)
     * @return Pair(newGame, success)
     */
    suspend fun attemptMove(
        game: Game,
        sourceType: StackType,
        sourceIndex: Int,
        cardIndexInSource: Int,
        targetType: StackType,
        targetIndex: Int
    ): Pair<Game, Boolean> = mutex.withLock {

        // Helper getters
        fun getStack(type: StackType, index: Int): CardStack? =
            when (type) {
                StackType.STOCK -> game.stock
                StackType.WASTE -> game.waste
                StackType.TABLEAU -> game.tableau.getOrNull(index)
                StackType.FOUNDATION -> game.foundations.getOrNull(index)
            }

        val source = getStack(sourceType, sourceIndex) ?: return Pair(game, false)
        val target = getStack(targetType, targetIndex) ?: return Pair(game, false)

        // Build list of cards to move
        val cardsToMove: List<Card> = when (sourceType) {
            StackType.TABLEAU -> {
                val srcList = source.asList().toList()
                if (cardIndexInSource < 0 || cardIndexInSource >= srcList.size) return Pair(
                    game,
                    false
                )
                srcList.subList(cardIndexInSource, srcList.size)
            }

            else -> {
                // only top card
                val top = source.peek() ?: return Pair(game, false)
                listOf(top)
            }
        }

        // Validate move using rules
        if (!isValidMove(cardsToMove, target)) return Pair(game, false)

        // Execute move using immutable Game methods
        var updatedGame = game
        var scoreDelta = 0

        when {
            // WASTE -> TABLEAU
            sourceType == StackType.WASTE && targetType == StackType.TABLEAU -> {
                val result = game.moveWasteToTableau(targetIndex)
                if (result == null) return Pair(game, false)
                updatedGame = result
                scoreDelta = 5
            }

            // WASTE -> FOUNDATION
            sourceType == StackType.WASTE && targetType == StackType.FOUNDATION -> {
                val result = game.moveWasteToFoundation(targetIndex)
                if (result == null) return Pair(game, false)
                updatedGame = result
                scoreDelta = 10
            }

            // TABLEAU -> TABLEAU
            sourceType == StackType.TABLEAU && targetType == StackType.TABLEAU -> {
                val result = game.moveTableauToTableau(sourceIndex, cardIndexInSource, targetIndex)
                if (result == null) return Pair(game, false)
                updatedGame = result
                scoreDelta = 0
            }

            // TABLEAU -> FOUNDATION
            sourceType == StackType.TABLEAU && targetType == StackType.FOUNDATION -> {
                val result = game.moveTableauToFoundation(sourceIndex, cardIndexInSource, targetIndex)
                if (result == null) return Pair(game, false)
                updatedGame = result
                scoreDelta = 10
            }

            else -> {
                return Pair(game, false)
            }
        }

        // Apply score update
        var finalGame = updatedGame.copy(
            score = maxOf(0, updatedGame.score + scoreDelta)
        )

        // If win condition met, mark won
        if (finalGame.isWinCondition()) {
            finalGame = finalGame.copy(status = GameStatus.WON)
        }

        return Pair(finalGame, true)
    }

    private fun checkWinCondition(game: Game): Boolean {
        // Win when all foundation piles have 13 cards
        return game.foundations.all { it.size() == 13 }
    }

    private fun isValidMove(cards: List<Card>, target: CardStack): Boolean {
        val topToMove = cards.first()
        val targetTop = target.peek()

        return when (target.type) {
            StackType.FOUNDATION -> {
                // Only single card moves
                if (cards.size > 1) return false
                when (targetTop) {
                    null -> topToMove.recto.rank is StandardRank && (topToMove.recto.rank as StandardRank) == StandardRank.ACE
                    else -> {
                        // same suit and one higher
                        topToMove.recto.suit == targetTop.recto.suit &&
                                topToMove.recto.rank.sortOrder == (targetTop.recto.rank.sortOrder + 1)
                    }
                }
            }

            StackType.TABLEAU -> {
                when (targetTop) {
                    null -> topToMove.recto.rank is StandardRank && topToMove.recto.rank == StandardRank.KING
                    else -> {
                        // alternating color and one lower
                        topToMove.recto.hasOppositeColor(targetTop.recto) &&
                                topToMove.recto.isOneLowerRank(targetTop.recto)
                    }
                }
            }

            else -> false
        }
    }

    private fun scoreForMove(src: StackType, dst: StackType): Int = when {
        src == StackType.WASTE && dst == StackType.TABLEAU -> 5
        src == StackType.WASTE && dst == StackType.FOUNDATION -> 10
        src == StackType.TABLEAU && dst == StackType.FOUNDATION -> 10
        src == StackType.FOUNDATION && dst == StackType.TABLEAU -> -15
        else -> 0
    }

    // --- Minimal move representations used for undo/history ---
    private sealed class Move {
        data class Draw(val cards: List<Card>) : Move()
        data class StockReset(val wasteCards: List<Card>) : Move()
        data class CardMove(
            val cards: List<Card>,
            val sourceType: StackType,
            val sourceIndex: Int,
            val targetType: StackType,
            val targetIndex: Int,
            val flipped: Boolean,
            val scoreDelta: Int
        ) : Move()
    }
}
