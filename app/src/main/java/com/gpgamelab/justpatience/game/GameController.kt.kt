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

    // Move history for undo (stores minimal info to reverse a move)
//    private val moveHistory = ArrayDeque<Move>()

    // Mutex for thread-safety if called from multiple coroutines
    private val mutex = Mutex()

    suspend fun newGameWithClearHistory(): Game = mutex.withLock {
        val g = Game.newGame()
//        moveHistory.clear()
        g
    }

//    suspend fun drawFromStock(game: Game): Game = mutex.withLock {
//        // Work on copies where needed (Game is a data class)
//        val stock = game.stock
//        val waste = game.waste
//
//        // If stock has cards, draw one to waste
//        val drawn = stock.draw(1)
//        if (drawn != null && drawn.isNotEmpty()) {
//            drawn.forEach { waste.push(it.copy(isFaceUp = true)) }
//
////            moveHistory.addLast(Move.Draw(drawn))
//            return game.copy(
//                stock = stock,
//                waste = waste,
//                moves = game.moves + 1,
//                score = game.score + 5
//            )
//        } else {
//            // Reset: move all waste back to stock (face-down), preserving order
//            val wasteCards = waste.asList().toList()
//            if (wasteCards.isEmpty()) return game // nothing to do
//
//            // clear waste, put reversed into stock face down
//            wasteCards.reversed().forEach {
//                stock.push(it.copy(isFaceUp = false))
//            }
//            waste.take(wasteCards.size) // remove them from waste (take returns, but also removes)
//
////            moveHistory.addLast(Move.StockReset(wasteCards))
//            // optional scoring rule: penalty for reset
//            return game.copy(
//                stock = stock,
//                waste = waste,
//                moves = game.moves + 1,
//                score = maxOf(0, game.score - 100)
//            )
//        }
//    }

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
                // need to use asList() and compute sublist by index
//                val srcList = source.asList()
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

//    suspend fun undo(game: Game): Game = mutex.withLock {
//        val last = moveHistory.removeLastOrNull() ?: return game
//        when (last) {
//            is Move.Draw -> {
//                // Move drawn cards back from waste to stock (face-down)
//                val drawn = last.cards
//                drawn.forEach {
//                    // remove from waste (topmost)
//                    if (game.waste.peek() == it) {
//                        game.waste.pop()
//                    }
//                    it.isFaceUp = false
//                    game.stock.push(it)
//                }
//                return game.copy(
//                    stock = game.stock,
//                    waste = game.waste,
//                    moves = game.moves + 1,
//                    score = maxOf(0, game.score - 5)
//                )
//            }
//
//            is Move.StockReset -> {
//                // Move the cards from stock back to waste (they were moved reversed)
//                last.wasteCards.forEach {
//                    // remove from stock (top order) and push face-up to waste
//                    if (game.stock.peek() == it) {
//                        game.stock.pop()
//                    }
//                    it.isFaceUp = true
//                    game.waste.push(it)
//                }
//                return game.copy(
//                    stock = game.stock,
//                    waste = game.waste,
//                    moves = game.moves + 1,
//                    score = game.score + 100
//                )
//            }
//
//            is Move.CardMove -> {
//                // Reverse card move: remove from target, add back to source
//                val targetStack = when (last.targetType) {
//                    StackType.STOCK -> game.stock
//                    StackType.WASTE -> game.waste
//                    StackType.TABLEAU -> game.tableau.getOrNull(last.targetIndex)
//                    StackType.FOUNDATION -> game.foundations.getOrNull(last.targetIndex)
//                } ?: return game
//
//                val sourceStack = when (last.sourceType) {
//                    StackType.STOCK -> game.stock
//                    StackType.WASTE -> game.waste
//                    StackType.TABLEAU -> game.tableau.getOrNull(last.sourceIndex)
//                    StackType.FOUNDATION -> game.foundations.getOrNull(last.sourceIndex)
//                } ?: return game
//
//                // remove last.cards.size from target (assumes they are at end)
//                if (last.cards.size == 1) {
//                    targetStack.pop()
//                    sourceStack.push(last.cards.first())
//                } else {
//                    // For sequence moves, pop N from target and push back in order
//                    val taken = (targetStack as? TableauPile)?.take(last.cards.size) ?: run {
//                        // fallback: remove one-by-one if needed
//                        last.cards.forEach { targetStack.pop() }
//                        null
//                    }
//
//                    if (taken != null) {
//                        // push back onto source
//                        taken.forEach { sourceStack.push(it) }
//                    } else {
//                        // best-effort: push the recorded cards back
//                        last.cards.forEach { sourceStack.push(it) }
//                    }
//                }
//
//                // If flip was done during the original move, flip it back
//                if (last.flipped && sourceStack is TableauPile) {
//                    sourceStack.peek()?.isFaceUp = false
//                }
//
//                return game.copy(
//                    moves = game.moves + 1,
//                    score = maxOf(0, game.score - last.scoreDelta)
//                )
//            }
//        }
//    }

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
