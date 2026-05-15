package com.gpgamelab.justpatience.util

/**
 * Classifies a device into one of four aspect-ratio buckets based on the ratio
 * of its shorter side to its longer side (X = min / max, so 0 < X ≤ 1.0).
 *
 *  SLIM     – X ≤ 0.50   → modern tall/narrow phones  (20:9, 21:9, …)
 *  CLASSIC  – 0.50 < X < 0.58 → 16:9 era phones
 *  BROAD    – 0.58 ≤ X < 0.78 → tablets, wide-format devices
 *  SQUARE   – X ≥ 0.78   → foldables and near-square devices
 *
 * Because the ratio is always computed from min/max, the same device returns the
 * same category regardless of its current orientation.
 */
enum class DeviceAspectCategory {
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
                ratio <= 0.50f -> SLIM
                ratio < 0.58f  -> CLASSIC
                ratio < 0.78f  -> BROAD
                else           -> SQUARE
            }
        }
    }
}

