package com.gpgamelab.justpatience.util

/**
 * Classifies a device into one of five aspect-ratio buckets based on the ratio
 * of its shorter side to its longer side (X = min / max, so 0 < X ≤ 1.0).
 *
 *  SLIM_COMPACT – X ≤ 0.44   → ultra-narrow phones (22:9+, ratio ≤ 0.409)
 *  SLIM         – 0.44 < X ≤ 0.50 → tall/narrow phones (20:9 ≈ 0.45, 21:9 ≈ 0.43)
 *  CLASSIC      – 0.50 < X < 0.58 → 16:9 era phones
 *  BROAD        – 0.58 ≤ X < 0.78 → tablets, wide-format devices
 *  SQUARE       – X ≥ 0.78   → foldables and near-square devices
 *
 * Because the ratio is always computed from min/max, the same device returns the
 * same category regardless of its current orientation.
 *
 * SLIM_COMPACT is significant for ad placement: on these devices a 320 dp banner
 * ad consumes 85 %+ of screen width, so controls must be repositioned above the ad.
 */
enum class DeviceAspectCategory {
    SLIM_COMPACT,
    SLIM,
    CLASSIC,
    BROAD,
    SQUARE;

    companion object {
        /** Classify from raw pixel dimensions (orientation-independent). */
        fun classify(widthPx: Int, heightPx: Int): DeviceAspectCategory {
            val w = widthPx.coerceAtLeast(1)
            val h = heightPx.coerceAtLeast(1)
            val ratio = minOf(w, h).toFloat() / maxOf(w, h).toFloat()
            return when {
                ratio <= 0.44f -> SLIM_COMPACT
                ratio <= 0.50f -> SLIM
                ratio < 0.58f  -> CLASSIC
                ratio < 0.78f  -> BROAD
                else           -> SQUARE
            }
        }
    }
}
