package com.gpgamelab.justpatience.model

/**
 * Represents a single destination glow when the player taps a card with multiple valid moves.
 * Shows simultaneously: source card in amber, all valid destinations in cyan.
 */
data class SingleClickGlowState(
    val sourceStackType: StackType,
    val sourceStackIndex: Int,
    val sourceCardIndex: Int,
    val destinations: List<GlowDestination>
)

data class GlowDestination(
    val destStackType: StackType,
    val destStackIndex: Int
)

