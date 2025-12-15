package com.gpgamelab.justpatience.assets

import android.graphics.Bitmap

interface AssetResolver {

    /**
     * Resolve an imagePath into a Bitmap.
     *
     * @throws AssetResolutionException if the asset cannot be found or loaded
     */
//    fun resolve(imagePath: String): Bitmap
    fun resolve(imagePath: String, targetW: Int, targetH: Int): Bitmap
}
