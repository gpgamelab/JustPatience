package com.gpgamelab.justpatience.ads

/**
 * AdConfig contains all ad-related configuration and constants.
 * This file should be updated with your actual AdMob Ad Unit IDs.
 */
object AdConfig {
    
    /**
     * Your AdMob App ID (get this from AdMob console)
     * Format: ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyyyyy
     */
    const val ADMOB_APP_ID = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyyyyy"
    
    /**
     * Banner Ad Unit ID - Used for displaying banner ads
     * 
     * TEST UNIT ID (for development): ca-app-pub-3940256099942544/6300978111
     * PRODUCTION UNIT ID: You'll get this from Google AdMob console after setting up your app
     * 
     * To use your production ID:
     * 1. Sign up at https://admob.google.com
     * 2. Add your app and create a banner ad unit
     * 3. Replace the test ID below with your production ID
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    
    /**
     * Full-screen (Interstitial) Ad Unit ID - For future implementation
     * TEST UNIT ID (for development): ca-app-pub-3940256099942544/1033173712
     */
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    
    /**
     * Rewarded Ad Unit ID - For future implementation
     * TEST UNIT ID (for development): ca-app-pub-3940256099942544/5224354917
     */
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    
    /**
     * Whether to use test ads during development
     * Set to false only when you're ready to go live with production ads
     */
    const val USE_TEST_ADS = true
}

