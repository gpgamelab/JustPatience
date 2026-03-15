package com.gpgamelab.justpatience.ads

import android.content.Context
import android.util.Log
import com.gpgamelab.justpatience.R
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback

/**
 * AdManager handles initialization and management of Google Mobile Ads.
 * This class centralizes all ad-related functionality.
 */
class AdManager(private val context: Context) {

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var showOnLoad = false
    private var adDismissedCallback: (() -> Unit)? = null

    companion object {
        private const val TAG = "AdManager"

        // TODO: Replace with your actual Ad Unit IDs from Google AdMob
        // Test Ad Unit IDs (use these for development/testing)
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

        const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-7092037186763886/6653974301"
        const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7092037186763886/9723495353"
        const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-7092037186763886/6817625839"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_UNDO_BTN = "ca-app-pub-7092037186763886/8518224896"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_REDO_BTN = "ca-app-pub-7092037186763886/5983415109"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_RESTART_BTN = "ca-app-pub-7092037186763886/8929288432"

        // Keep all ad types aligned with the current build unless explicitly overridden.
        private var isTestMode = false
    }

    init {
        isTestMode = context.resources.getBoolean(R.bool.use_test_ad_ids)
        Log.d(TAG, "Ad mode initialized: ${if (isTestMode) "TEST" else "PRODUCTION"}")
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
     * @param adView The XML AdView to load
     */
    fun loadBannerAd(adView: AdView) {
        try {
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${adError.message}")
                }

                override fun onAdOpened() {
                    Log.d(TAG, "Banner ad opened")
                }

                override fun onAdClicked() {
                    Log.d(TAG, "Banner ad clicked")
                }

                override fun onAdClosed() {
                    Log.d(TAG, "Banner ad closed")
                }
            }

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            Log.d(TAG, "Banner ad loading started with XML unit ID: ${adView.adUnitId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load banner ad", e)
        }
    }

    /**
     * Load an interstitial ad.
     */
    fun loadInterstitialAd() {
        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (isTestMode) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

            InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    Log.d(TAG, "Interstitial ad loaded successfully")

                    if (showOnLoad) {
                        showInterstitialAd()
                        showOnLoad = false
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message}")
                    mInterstitialAd = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load interstitial ad", e)
        }
    }

    /**
     * Show the interstitial ad if loaded.
     * @return true if an ad was shown, false if no ad was ready
     */
    fun showInterstitialAd(onCompleted: (() -> Unit)? = null): Boolean {
        val ad = mInterstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                mInterstitialAd = null
                loadInterstitialAd()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                mInterstitialAd = null
                loadInterstitialAd()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity)
        return true
    }

    fun showInterstitialAd() {
        showInterstitialAd(null)
    }

    /**
     * Load a rewarded ad.
     */
    fun loadRewardedAd() {
        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (isTestMode) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    mRewardedAd = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded ad", e)
        }
    }
    fun loadRewardedAdUndoBtn() {
        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (isTestMode) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_UNDO_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    mRewardedAd = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded ad", e)
        }
    }
    fun loadRewardedAdRedoBtn() {
        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (isTestMode) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_REDO_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    mRewardedAd = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded ad", e)
        }
    }
    fun loadRewardedAdRestartBtn() {
        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (isTestMode) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_RESTART_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    Log.d(TAG, "Rewarded ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    mRewardedAd = null
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded ad", e)
        }
    }

    /**
     * Show rewarded ad if available.
     * @return true if ad was shown, false if unavailable.
     */
    fun showRewardedAd(onCompleted: (() -> Unit)? = null): Boolean {
        val ad = mRewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                mRewardedAd = null
                loadRewardedAd()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                mRewardedAd = null
                loadRewardedAd()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }
    fun showRewardedAdUndoBtn(onCompleted: (() -> Unit)? = null): Boolean {
        val ad = mRewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                mRewardedAd = null
                loadRewardedAdUndoBtn()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                mRewardedAd = null
                loadRewardedAdUndoBtn()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }
    fun showRewardedAdRedoBtn(onCompleted: (() -> Unit)? = null): Boolean {
        val ad = mRewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                mRewardedAd = null
                loadRewardedAdRedoBtn()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                mRewardedAd = null
                loadRewardedAdRedoBtn()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }
    fun showRewardedAdRestartBtn(onCompleted: (() -> Unit)? = null): Boolean {
        val ad = mRewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                mRewardedAd = null
                loadRewardedAdRestartBtn()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                mRewardedAd = null
                loadRewardedAdRestartBtn()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }

    /**
     * Set whether the app is in test mode (shows test ads)
     * @param testMode true for test ads, false for production ads
     */
    fun setTestMode(testMode: Boolean) {
        isTestMode = testMode
        Log.d(TAG, "Test mode set to: $testMode")
    }

    /**
     * Set whether to show the interstitial ad immediately when it loads.
     * @param show true to show ad on load, false otherwise
     */
    fun setShowOnLoad(show: Boolean) {
        showOnLoad = show
        Log.d(TAG, "Show on load set to: $show")
    }

    /**
     * Set a callback to be invoked when the interstitial ad is dismissed.
     * @param callback The callback to invoke
     */
    fun setAdDismissedCallback(callback: () -> Unit) {
        adDismissedCallback = callback
    }
}
