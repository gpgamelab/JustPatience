package com.gpgamelab.justpatience.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

class AndroidAssetResolver(
    private val context: Context
) : AssetResolver {

    private val bitmapCache = mutableMapOf<String, Bitmap>()

//    override fun resolve(imagePath: String): Bitmap {
//        bitmapCache[imagePath]?.let { return it }
//
//        val bitmap = when {
//            imagePath.startsWith("drawable:") -> {
//                loadFromDrawable(imagePath.removePrefix("drawable:"))
//            }
//            imagePath.startsWith("file:") -> {
//                loadFromFile(imagePath.removePrefix("file:"))
//            }
//            else -> {
//                throw AssetResolutionException(
//                    "Unknown imagePath scheme: $imagePath"
//                )
//            }
//        }
//
//        bitmapCache[imagePath] = bitmap
//        return bitmap
//    }
    override fun resolve(imagePath: String, targetW: Int, targetH: Int): Bitmap {
        val resId = when {
            imagePath.startsWith("drawable:") -> {
                val name = imagePath.removePrefix("drawable:")
                context.resources.getIdentifier(name, "drawable", context.packageName)
            }
            else -> throw AssetResolutionException("Unknown imagePath scheme: $imagePath")
        }

        if (resId == 0) {
            throw AssetResolutionException("Drawable not found for $imagePath")
        }

        // 1️⃣ Decode bounds only
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeResource(context.resources, resId, bounds)

        // 2️⃣ Calculate sampling
        val sampleSize = calculateInSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            targetW,
            targetH
        )

        // 3️⃣ Decode scaled bitmap
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        return BitmapFactory.decodeResource(context.resources, resId, opts)
            ?: throw AssetResolutionException("Failed to decode bitmap: $imagePath")
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
