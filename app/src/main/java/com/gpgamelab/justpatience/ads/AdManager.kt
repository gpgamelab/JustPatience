package com.gpgamelab.justpatience.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

/**
 * AdManager handles initialization and management of Google Mobile Ads.
 * This class centralizes all ad-related functionality.
 */
class AdManager(private val context: Context) {

    companion object {
        private const val TAG = "AdManager"

        // TODO: Replace with your actual Ad Unit IDs from Google AdMob
        // Test Ad Unit IDs (use these for development/testing)
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx"

        // For now, we'll use test ads
        private var isTestMode = true
    }

    /**
     * Initialize the Google Mobile Ads SDK.
     * Call this once when the app starts (in your Application class or MainActivity).
     */
    fun initializeAds() {
        try {
            MobileAds.initialize(context)
            Log.d(TAG, "Google Mobile Ads SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Mobile Ads SDK", e)
        }
    }

    /**
     * Load a banner ad into the provided AdView.
     * @param adView The AdView to load the ad into
     */
    fun loadBannerAd(adView: AdView) {
        try {
            val adRequest = AdRequest.Builder().build()

            val adUnitId = if (isTestMode) {
                TEST_BANNER_AD_UNIT_ID
            } else {
                PRODUCTION_BANNER_AD_UNIT_ID
            }

            adView.adUnitId = adUnitId
            adView.loadAd(adRequest)

            Log.d(TAG, "Banner ad loading started (Test Mode: $isTestMode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load banner ad", e)
        }
    }

    /**
     * Set whether the app is in test mode (shows test ads)
     * @param testMode true for test ads, false for production ads
     */
    fun setTestMode(testMode: Boolean) {
        isTestMode = testMode
        Log.d(TAG, "Test mode set to: $testMode")
    }
}

