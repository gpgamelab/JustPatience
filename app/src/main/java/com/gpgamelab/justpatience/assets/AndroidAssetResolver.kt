package com.gpgamelab.justpatience.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat

private data class CacheKey(
    val setId: String,
    val path: String,
    val width: Int,
    val height: Int
)

class AndroidAssetResolver(
    private val context: Context
) : AssetResolver {

    companion object {
        private const val TAG = "AndroidAssetResolver"
    }

    private val cache = mutableMapOf<CacheKey, Bitmap>()

    override fun resolve(
        setId: String,
        path: String,
        width: Int,
        height: Int
    ): Bitmap {

        val key = CacheKey(setId, path, width, height)

        cache[key]?.let { return it }

        val resourceId = resolveResourceId(setId, path)

        val resolvedResourceId = if (resourceId != 0) {
            resourceId
        } else {
            // Keep gameplay alive for legacy/unknown saved image paths.
            // Use type-aware fallback so missing face cards do not look face-down.
            val fallbackPath = if (looksLikeBackPath(path)) {
                "drawable:b_0001"
            } else {
                "drawable:ic_spades_ace"
            }
            val fallback = resolveResourceId(setId, fallbackPath)
            if (fallback == 0) {
                throw AssetResolutionException("Drawable not found for $path")
            }
            Log.w(TAG, "Drawable not found for $path, using fallback $fallbackPath")
            fallback
        }

        val drawable = ResourcesCompat.getDrawable(
            context.resources,
            resolvedResourceId,
            context.theme
        ) ?: throw AssetResolutionException("Unable to load drawable resource: $path")

        val scaled = renderDrawableToBitmap(drawable, width, height)

        cache[key] = scaled

        return scaled
    }

    private fun resolveResourceId(setId: String, path: String): Int {

        val (type, name) = when {
            path.startsWith("drawable:") -> {
                "drawable" to path.removePrefix("drawable:")
            }

            // Backward compatibility: accept bare drawable names.
            path.matches(BARE_DRAWABLE_NAME_REGEX) -> {
                "drawable" to path
            }

            else -> {
                throw AssetResolutionException("Unknown imagePath scheme: $path")
            }
        }

        // 🔮 Future-proofing:
        // For now we DO NOT modify the name.
        // Later you can enable prefixing like:
        // val finalName = "${setId}_$name"
        val candidateNames = buildCandidateNames(name)

        for (candidate in candidateNames) {
            val id = context.resources.getIdentifier(
                candidate,
                type,
                context.packageName
            )
            if (id != 0) return id
        }

        return 0
    }

    private fun buildCandidateNames(name: String): List<String> {
        val normalized = name.lowercase()
        val candidates = linkedSetOf(name, normalized)

        // Legacy short format compatibility (e.g., s_02, h_a, d_q).
        legacyShortNameToCurrent(normalized)?.let { candidates.add(it) }

        return candidates.toList()
    }

    private fun looksLikeBackPath(path: String): Boolean {
        val normalized = path.lowercase()
        val bare = normalized.removePrefix("drawable:")
        return bare.startsWith("b_") || "back" in bare
    }

    private fun legacyShortNameToCurrent(name: String): String? {
        val match = LEGACY_CARD_NAME_REGEX.matchEntire(name) ?: return null
        val suitCode = match.groupValues[1]
        val rankCode = match.groupValues[2]

        val suitName = when (suitCode) {
            "s" -> "spades"
            "h" -> "hearts"
            "d" -> "diamonds"
            "c" -> "clubs"
            else -> return null
        }

        val rankName = when (rankCode) {
            "a" -> "ace"
            "j" -> "jack"
            "q" -> "queen"
            "k" -> "king"
            else -> {
                val numeric = rankCode.toIntOrNull() ?: return null
                when (numeric) {
                    1 -> "ace"
                    11 -> "jack"
                    12 -> "queen"
                    13 -> "king"
                    else -> numeric.toString()
                }
            }
        }

        return "ic_${suitName}_${rankName}"
    }

    private val LEGACY_CARD_NAME_REGEX = Regex("^([shdc])_([0-9]{1,2}|[ajqk])$")
    private val BARE_DRAWABLE_NAME_REGEX = Regex("^[a-z0-9_]+$")

    override fun purge(setId: String) {
        val keysToRemove = cache.keys.filter { it.setId == setId }

        keysToRemove.forEach { key ->
            cache[key]?.recycle()
            cache.remove(key)
        }
    }

    override fun purge(setId: String, path: String) {
        val keysToRemove = cache.keys.filter {
            it.setId == setId && it.path == path
        }

        keysToRemove.forEach { key ->
            cache[key]?.recycle()
            cache.remove(key)
        }
    }

    private fun renderDrawableToBitmap(
        drawable: Drawable,
        width: Int,
        height: Int
    ): Bitmap {
        val targetWidth = width.coerceAtLeast(1)
        val targetHeight = height.coerceAtLeast(1)

        return Bitmap.createBitmap(
            targetWidth,
            targetHeight,
            Bitmap.Config.ARGB_8888
        ).also { bitmap ->
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, targetWidth, targetHeight)
            drawable.draw(canvas)
        }
    }
}
