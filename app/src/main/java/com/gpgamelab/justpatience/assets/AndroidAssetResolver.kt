package com.gpgamelab.justpatience.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

private data class CacheKey(
    val setId: String,
    val path: String,
    val width: Int,
    val height: Int
)

class AndroidAssetResolver(
    private val context: Context
) : AssetResolver {

//    private val bitmapCache = mutableMapOf<String, Bitmap>()
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

        if (resourceId == 0) {
            throw AssetResolutionException("Drawable not found for $path")
        }

        // Decode efficiently using sampling (your old improvement)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resourceId, bounds)

        val sampleSize = calculateInSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            width,
            height
        )

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = BitmapFactory.decodeResource(context.resources, resourceId, opts)
            ?: throw AssetResolutionException("Unable to decode resource: $path")

        val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)

        cache[key] = scaled

        return scaled
    }

    private fun resolveResourceId(setId: String, path: String): Int {

        val (type, name) = when {
            path.startsWith("drawable:") -> {
                "drawable" to path.removePrefix("drawable:")
            }
            else -> {
                throw AssetResolutionException("Unknown imagePath scheme: $path")
            }
        }

        // 🔮 Future-proofing:
        // For now we DO NOT modify the name.
        // Later you can enable prefixing like:
        // val finalName = "${setId}_$name"
        val finalName = name

        return context.resources.getIdentifier(
            finalName,
            type,
            context.packageName
        )
    }

//    override fun resolve(
//        setId: String,
//        path: String,
//        width: Int,
//        height: Int
//    ): Bitmap {
//
//        val key = CacheKey(setId, path, width, height)
//
//        cache[key]?.let { return it }
//
//        val resourceId = resolveResourceId(setId, path)
//
//        val original = BitmapFactory.decodeResource(context.resources, resourceId)
//            ?: throw AssetResolutionException("Unable to decode resource: $path")
//
//        val scaled = Bitmap.createScaledBitmap(original, width, height, true)
//
//        cache[key] = scaled
//
//        return scaled
//    }
//
//    private fun resolveResourceId(setId: String, path: String): Int {
//        val futureFullName = "${setId}_$path"
//        Log.d("AssetResolver", "ZYZZX Resolving futureFullName: $futureFullName")
//        val fullName = "${path}"
//        Log.d("AssetResolver", "ZYZZX Resolving path: $path")
//        Log.d("AssetResolver", "ZYZZX Resolving fullName: $fullName")
//        return context.resources.getIdentifier(
//            fullName,
//            "drawable",
//            context.packageName
//        )
//    }

    //    override fun resolve(imagePath: String, targetW: Int, targetH: Int): Bitmap {
//        val resId = when {
//            imagePath.startsWith("drawable:") -> {
//                val name = imagePath.removePrefix("drawable:")
//                context.resources.getIdentifier(name, "drawable", context.packageName)
//            }
//            else -> throw AssetResolutionException("Unknown imagePath scheme: $imagePath")
//        }
//
//        if (resId == 0) {
//            throw AssetResolutionException("Drawable not found for $imagePath")
//        }
//
//        // 1️⃣ Decode bounds only
//        val bounds = BitmapFactory.Options().apply {
//            inJustDecodeBounds = true
//        }
//        BitmapFactory.decodeResource(context.resources, resId, bounds)
//
//        // 2️⃣ Calculate sampling
//        val sampleSize = calculateInSampleSize(
//            bounds.outWidth,
//            bounds.outHeight,
//            targetW,
//            targetH
//        )
//
//        // 3️⃣ Decode scaled bitmap
//        val opts = BitmapFactory.Options().apply {
//            inSampleSize = sampleSize
//            inPreferredConfig = Bitmap.Config.ARGB_8888
//        }
//
//        return BitmapFactory.decodeResource(context.resources, resId, opts)
//            ?: throw AssetResolutionException("Failed to decode bitmap: $imagePath")
//    }

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

    private fun calculateInSampleSize(
        srcW: Int,
        srcH: Int,
        dstW: Int,
        dstH: Int
    ): Int {
        var sample = 1

        while (srcW / sample > dstW * 2 || srcH / sample > dstH * 2) {
            sample *= 2
        }
        return sample
    }

    private fun loadFromDrawable(name: String): Bitmap {
        val resId = context.resources.getIdentifier(
            name,
            "drawable",
            context.packageName
        )

        if (resId == 0) {
            throw AssetResolutionException(
                "Drawable resource not found: drawable:$name"
            )
        }

        return BitmapFactory.decodeResource(context.resources, resId)
            ?: throw AssetResolutionException(
                "Failed to decode drawable resource: drawable:$name"
            )
    }

    private fun loadFromFile(path: String): Bitmap {
        val file = File(path)

        if (!file.exists()) {
            throw AssetResolutionException(
                "Image file does not exist: file:$path"
            )
        }

        return BitmapFactory.decodeFile(file.absolutePath)
            ?: throw AssetResolutionException(
                "Failed to decode image file: file:$path"
            )
    }
}
