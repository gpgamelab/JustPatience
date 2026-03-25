package com.gpgamelab.justpatience.model

/**
 * Represents a single legal card move that can be shown as a hint to the player.
 *
 * @param sourceStackType  Stack type the moving card comes from.
 * @param sourceStackIndex Pile index within that stack type (0 for WASTE).
 * @param sourceCardIndex  Card's index within the pile (-1 means top / whole-pile for WASTE).
 * @param destStackType    Stack type the card should be moved to.
 * @param destStackIndex   Pile index within the destination stack type.
 */
data class HintMove(
    val sourceStackType: StackType,
    val sourceStackIndex: Int,
    val sourceCardIndex: Int,
    val destStackType: StackType,
    val destStackIndex: Int
)

/**
 * Three-phase glow sequence per hint move:
 *
 *  SOURCE_ONLY    – source card glows (1 s)
 *  SOURCE_AND_DEST – source + destination glow simultaneously (1 s)
 *  DEST_ONLY      – only destination glows (1 s)
 *
 * After DEST_ONLY the next move in the list begins.
 */
enum class HintPhase {
    SOURCE_ONLY,
    SOURCE_AND_DEST,
    DEST_ONLY
}

/** Current hint animation state consumed by GameBoardView to draw glow rings. */
data class HintDisplayState(
    val move: HintMove,
    val phase: HintPhase
)

