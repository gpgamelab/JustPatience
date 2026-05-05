package com.gpgamelab.justpatience.model

object ScoreMethod {
    const val WINDOWS = "windows"
    const val VEGAS = "vegas"
    const val VEGAS_CUMULATIVE = "vegas_cumulative"
    const val COMPLETION = "completion"

    fun normalize(raw: String?): String {
        return when (raw?.trim()?.lowercase()) {
            VEGAS -> VEGAS
            VEGAS_CUMULATIVE -> VEGAS_CUMULATIVE
            COMPLETION -> COMPLETION
            else -> WINDOWS
        }
    }
}

object ScoreCalculator {
    fun computeVegasScore(game: Game): Int {
        val ante = 52 * Game.normalizeDeckCount(game.deckCount)
        return (game.foundationCardsCount() * 5) - ante
    }

    fun computeCompletionPercentage(game: Game): Int {
        val totalFoundationCards = game.foundations.size * 13
        if (totalFoundationCards <= 0) return 0
        return ((game.foundationCardsCount() * 100.0) / totalFoundationCards)
            .toInt()
            .coerceIn(0, 100)
    }
}

