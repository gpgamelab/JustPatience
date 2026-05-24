package com.gpgamelab.justpatience.ads

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.util.Log
import com.gpgamelab.justpatience.R
import com.gpgamelab.justpatience.util.UiScaleUtil
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback

/**
 * AdManager handles initialization and management of Google Mobile Ads.
 * This class centralizes all ad-related functionality.
 */
class AdManager(private val context: Context) {

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null
    private var mRewardedInterstitialAd: RewardedInterstitialAd? = null
    private var showOnLoad = false
    private var adDismissedCallback: (() -> Unit)? = null

    // Banner fallback / retry state
    private val bannerRetryHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var bannerRetryRunnable: Runnable? = null
    private val bannerRetryDelayMs = 5 * 60 * 1000L // 5 minutes
    private val useProductionAds = context.resources.getBoolean(R.bool.use_production_ad_ids)
    private val useFakeTestAds = context.resources.getBoolean(R.bool.use_fake_test_ads)
    private val baseUseFakeAds = !useProductionAds && useFakeTestAds
    private val baseUseTestAds = !useProductionAds && !useFakeTestAds
    private var developerForceTestAds = false
    private var sdkInitialized = false
    private val isDebugBuild: Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun logDebug(message: String) {
        if (isDebugBuild) {
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "AdManager"

        // TODO: Replace with your actual Ad Unit IDs from Google AdMob
        // Test Ad Unit IDs (use these for development/testing)
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

        const val PRODUCTION_BANNER_AD_UNIT_ID = "ca-app-pub-7092037186763886/6653974301"
        const val PRODUCTION_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7092037186763886/9723495353"
        const val PRODUCTION_REWARDED_AD_UNIT_ID = "ca-app-pub-7092037186763886/6817625839"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_UNDO_BTN = "ca-app-pub-7092037186763886/8518224896"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_REDO_BTN = "ca-app-pub-7092037186763886/5983415109"
        const val PRODUCTION_REWARDED_AD_UNIT_ID_RESTART_BTN = "ca-app-pub-7092037186763886/8929288432"
        const val PRODUCTION_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7092037186763886/1488840165"

        // Keep all ad types aligned with the current build unless explicitly overridden.
        private var isTestMode = false
    }

    private fun shouldUseFakeAds(): Boolean {
        return baseUseFakeAds && !developerForceTestAds
    }

    private fun shouldUseTestAds(): Boolean {
        // Developer force mode supersedes fake/production routing while debugging.
        return developerForceTestAds || isTestMode
    }

    init {
        isTestMode = baseUseTestAds
        val mode = when {
            useProductionAds -> "PRODUCTION"
            baseUseFakeAds -> "FAKE_TEST"
            else -> "ADMOB_TEST"
        }
        logDebug("Ad mode initialized: $mode")
    }

    /**
     * Initialize the Google Mobile Ads SDK.
     * Call this once when the app starts (in your Application class or MainActivity).
     */
    fun initializeAds() {
        if (shouldUseFakeAds()) {
            logDebug("Skipping Mobile Ads SDK initialization in fake ad mode")
            return
        }
        if (sdkInitialized) {
            logDebug("Google Mobile Ads SDK already initialized; skipping")
            return
        }
        try {
            MobileAds.initialize(context)
            sdkInitialized = true
            logDebug("Google Mobile Ads SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Google Mobile Ads SDK", e)
        }
    }

    /**
     * Load a banner ad.
     *
     * Ad size is defined in XML via app:adSize and must not be set again in code on the
     * same AdView instance (SDK enforces single assignment).
     */
    fun loadBannerAd(adView: AdView, requestedAdSizes: List<AdSize> = emptyList()) {
        cancelBannerRetry()
        if (shouldUseFakeAds()) {
            showFakeBanner(adView)
            return
        }
        hideFakeBanner(adView)
        if (requestedAdSizes.isNotEmpty()) {
            logDebug("Banner requested sizes=${requestedAdSizes.joinToString { "${it.width}x${it.height}" }} (XML adSize in effect)")
        }
        performBannerLoad(adView, requestedAdSizes, attemptIndex = 0)
    }

    /** Cancel any scheduled banner size retry. Call from onDestroy. */
    fun cancelBannerRetry() {
        bannerRetryRunnable?.let { bannerRetryHandler.removeCallbacks(it) }
        bannerRetryRunnable = null
    }

    private fun performBannerLoad(adView: AdView, sizes: List<AdSize>, attemptIndex: Int) {
        try {
            // setAdSize() can only be called once per AdView (SDK enforced). We keep banner
            // sizes fixed in XML and only retry load attempts on the same view.
            if (attemptIndex > 0) {
                logDebug("Banner retry attempt ${attemptIndex + 1}/${sizes.size} (same size)")
            }

            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val loaded = adView.adSize
                    logDebug("Banner ad loaded: ${loaded?.width}x${loaded?.height}")
                    // If we settled on a fallback, schedule a retry at the primary size.
                    if (attemptIndex > 0 && sizes.isNotEmpty()) {
                        scheduleRetryAtPrimarySize(adView, sizes)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val sizeName = if (sizes.isNotEmpty()) sizes.getOrNull(attemptIndex)
                        ?.let { "${it.width}x${it.height}" } ?: "xml" else "xml"
                    Log.w(TAG, "Banner failed at size $sizeName: code=${adError.code}")
                    if (sizes.isNotEmpty() && attemptIndex < sizes.lastIndex) {
                        performBannerLoad(adView, sizes, attemptIndex + 1)
                    } else {
                        Log.e(TAG, "All banner sizes exhausted. No ad displayed.")
                    }
                }

                override fun onAdOpened()  { logDebug("Banner ad opened") }
                override fun onAdClicked() { logDebug("Banner ad clicked") }
                override fun onAdClosed()  { logDebug("Banner ad closed") }
            }

            if (adView.adUnitId.isEmpty()) {
                Log.w(TAG, "AdView ad unit ID is empty.")
            }

            adView.loadAd(AdRequest.Builder().build())
            val sizeLabel = if (attemptIndex == 0 && sizes.isEmpty()) "xml-declared"
                else sizes.getOrNull(attemptIndex)?.let { "${it.width}x${it.height}" } ?: "xml-declared"
            logDebug("Banner loadAd started: unitId=${adView.adUnitId}, size=$sizeLabel")
        } catch (e: Exception) {
            Log.e(TAG, "Error in performBannerLoad", e)
        }
    }

    private fun scheduleRetryAtPrimarySize(adView: AdView, sizes: List<AdSize>) {
        cancelBannerRetry()
        val primarySize = sizes[0]
        logDebug("Scheduling banner retry at primary size ${primarySize.width}x${primarySize.height} in ${bannerRetryDelayMs / 1000}s")
        val runnable = Runnable {
            logDebug("Retrying banner at primary size ${primarySize.width}x${primarySize.height}")
            // Note: cannot call setAdSize() here (SDK allows only once). Just reload.
            performBannerLoad(adView, sizes, attemptIndex = 0)
        }
        bannerRetryRunnable = runnable
        bannerRetryHandler.postDelayed(runnable, bannerRetryDelayMs)
    }

    /**
     * Load an interstitial ad.
     */
    fun loadInterstitialAd() {
        if (shouldUseFakeAds()) {
            mInterstitialAd = null
            logDebug("Using fake popup for interstitial ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) TEST_INTERSTITIAL_AD_UNIT_ID else PRODUCTION_INTERSTITIAL_AD_UNIT_ID

            InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    logDebug("Interstitial ad loaded successfully")

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
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                completion?.invoke()
            }
        }

        val ad = mInterstitialAd
        if (ad == null) {
            logDebug("Interstitial ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Interstitial ad dismissed")
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
                logDebug("Interstitial ad showed full screen content")
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
        if (shouldUseFakeAds()) {
            mRewardedAd = null
            logDebug("Using fake popup for rewarded ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    logDebug("Rewarded ad loaded successfully")
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
        if (shouldUseFakeAds()) {
            mRewardedAd = null
            logDebug("Using fake popup for rewarded undo ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_UNDO_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    logDebug("Rewarded ad loaded successfully")
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
        if (shouldUseFakeAds()) {
            mRewardedAd = null
            logDebug("Using fake popup for rewarded redo ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_REDO_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    logDebug("Rewarded ad loaded successfully")
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
        if (shouldUseFakeAds()) {
            mRewardedAd = null
            logDebug("Using fake popup for rewarded restart ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) TEST_REWARDED_AD_UNIT_ID else PRODUCTION_REWARDED_AD_UNIT_ID_RESTART_BTN

            RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    logDebug("Rewarded ad loaded successfully")
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
     * Load a rewarded interstitial ad (used in the post-win dialog flow).
     */
    fun loadRewardedInterstitialAd() {
        if (shouldUseFakeAds()) {
            mRewardedInterstitialAd = null
            logDebug("Using fake popup for rewarded interstitial ad")
            return
        }

        try {
            val adRequest = AdRequest.Builder().build()
            val adUnitId = if (shouldUseTestAds()) {
                TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID
            } else {
                PRODUCTION_REWARDED_INTERSTITIAL_AD_UNIT_ID
            }

            RewardedInterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                        mRewardedInterstitialAd = rewardedInterstitialAd
                        logDebug("Rewarded interstitial ad loaded successfully")
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.e(TAG, "Rewarded interstitial ad failed to load: ${loadAdError.message}")
                        mRewardedInterstitialAd = null
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rewarded interstitial ad", e)
        }
    }

    /**
     * Show rewarded ad if available.
     * @return true if ad was shown, false if unavailable.
     */
    fun showRewardedAd(
        onCompleted: (() -> Unit)? = null,
        onUserEarnedReward: (() -> Unit)? = null,
        onFinished: ((Boolean) -> Unit)? = null
    ): Boolean {
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                onUserEarnedReward?.invoke()
                completion?.invoke()
                onFinished?.invoke(true)
            }
        }

        val ad = mRewardedAd
        if (ad == null) {
            logDebug("Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback
        var rewardEarned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded ad dismissed")
                mRewardedAd = null
                loadRewardedAd()
                completion?.invoke()
                onFinished?.invoke(rewardEarned)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                mRewardedAd = null
                loadRewardedAd()
                completion?.invoke()
                onFinished?.invoke(false)
            }

            override fun onAdShowedFullScreenContent() {
                logDebug("Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            rewardEarned = true
            logDebug("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            onUserEarnedReward?.invoke()
        }
        return true
    }
    fun showRewardedAdUndoBtn(onCompleted: (() -> Unit)? = null): Boolean {
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                completion?.invoke()
            }
        }

        val ad = mRewardedAd
        if (ad == null) {
            logDebug("Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded ad dismissed")
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
                logDebug("Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            logDebug("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }
    fun showRewardedAdRedoBtn(onCompleted: (() -> Unit)? = null): Boolean {
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                completion?.invoke()
            }
        }

        val ad = mRewardedAd
        if (ad == null) {
            logDebug("Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded ad dismissed")
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
                logDebug("Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            logDebug("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }
    fun showRewardedAdRestartBtn(onCompleted: (() -> Unit)? = null): Boolean {
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                completion?.invoke()
            }
        }

        val ad = mRewardedAd
        if (ad == null) {
            logDebug("Rewarded ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded ad dismissed")
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
                logDebug("Rewarded ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            logDebug("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
        return true
    }

    /**
     * Show rewarded interstitial ad if available.
     *
     * @param onCompleted Called when ad flow finishes (dismissed or failed to show).
     * @param onUserEarnedReward Called when the SDK reports reward earned.
     * @return true if ad was shown, false if unavailable.
     */
    fun showRewardedInterstitialAd(
        onCompleted: (() -> Unit)? = null,
        onUserEarnedReward: (() -> Unit)? = null
    ): Boolean {
        if (shouldUseFakeAds()) {
            val completion = onCompleted ?: adDismissedCallback
            return showFakeFullScreenAd {
                onUserEarnedReward?.invoke()
                completion?.invoke()
            }
        }

        val ad = mRewardedInterstitialAd
        if (ad == null) {
            logDebug("Rewarded interstitial ad not ready to show")
            return false
        }

        val completion = onCompleted ?: adDismissedCallback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                logDebug("Rewarded interstitial ad dismissed")
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
                completion?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Rewarded interstitial ad failed to show: ${adError.message}")
                mRewardedInterstitialAd = null
                loadRewardedInterstitialAd()
                completion?.invoke()
            }

            override fun onAdShowedFullScreenContent() {
                logDebug("Rewarded interstitial ad showed full screen content")
            }
        }

        ad.show(context as android.app.Activity) { rewardItem ->
            logDebug("User earned rewarded-interstitial reward: ${rewardItem.amount} ${rewardItem.type}")
            onUserEarnedReward?.invoke()
        }
        return true
    }

    fun setDeveloperForceTestAds(forceEnabled: Boolean) {
        if (!isDebugBuild) {
            logDebug("Ignoring developer test-ad force outside debug environment")
            return
        }
        developerForceTestAds = forceEnabled
        isTestMode = baseUseTestAds || forceEnabled
        val mode = when {
            shouldUseFakeAds() -> "FAKE_TEST"
            shouldUseTestAds() -> "ADMOB_TEST"
            else -> "PRODUCTION"
        }
        logDebug("Developer force test ads: $forceEnabled (effective mode=$mode)")
    }

    /**
     * Set whether the app is in test mode (shows test ads)
     * @param testMode true for test ads, false for production ads
     */
    fun setTestMode(testMode: Boolean) {
        if (useProductionAds && !developerForceTestAds) {
            logDebug("Ignoring setTestMode because ad mode is fixed by Gradle properties")
            return
        }
        if (shouldUseFakeAds() && !testMode) {
            logDebug("Ignoring setTestMode(false) while fake mode is active")
            return
        }
        isTestMode = testMode
        logDebug("Test mode set to: $testMode")
    }

    private fun showFakeBanner(adView: AdView) {
        // Keep AdView in the layout flow so bottom-constrained controls stay above banner space.
        adView.visibility = View.INVISIBLE
        (context as? Activity)?.findViewById<View>(R.id.fakeBannerView)?.visibility = View.VISIBLE
    }

    private fun hideFakeBanner(adView: AdView) {
        // Only restore from INVISIBLE (set by showFakeBanner); never override GONE which is
        // managed by resolveActiveBannerAdView to hide non-selected banner views.
        if (adView.visibility == View.INVISIBLE) {
            adView.visibility = View.VISIBLE
        }
        (context as? Activity)?.findViewById<View>(R.id.fakeBannerView)?.visibility = View.GONE
    }

    private fun showFakeFullScreenAd(onContinue: () -> Unit): Boolean {
        val activity = context as? Activity
        if (activity == null || activity.isFinishing) {
            Log.e(TAG, "Unable to show fake full-screen ad: invalid activity context")
            return false
        }

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_fake_fullscreen_ad, null)
        UiScaleUtil.applyBaselineScale(dialogView, activity)
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val metrics = activity.resources.displayMetrics
            dialog.window?.setLayout(
                (metrics.widthPixels * 0.8f).toInt(),
                (metrics.heightPixels * 0.8f).toInt()
            )
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        dialogView.findViewById<Button>(R.id.fakeAdContinueButton).setOnClickListener {
            dialog.dismiss()
            onContinue()
        }

        dialog.show()
        return true
    }

    /**
     * Set whether to show the interstitial ad immediately when it loads.
     * @param show true to show ad on load, false otherwise
     */
    fun setShowOnLoad(show: Boolean) {
        showOnLoad = show
        logDebug("Show on load set to: $show")
    }

    /**
     * Set a callback to be invoked when the interstitial ad is dismissed.
     * @param callback The callback to invoke
     */
    fun setAdDismissedCallback(callback: () -> Unit) {
        adDismissedCallback = callback
    }
}
