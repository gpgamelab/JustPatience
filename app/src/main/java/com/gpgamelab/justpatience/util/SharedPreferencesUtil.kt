package com.gpgamelab.justpatience.util

import android.content.Context
import android.content.SharedPreferences

// This utility class handles settings state for SFX and Music
class SharedPreferencesUtil(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "game_settings"
        private const val KEY_SFX_ENABLED = "sfx_enabled"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val DEFAULT_ENABLED = true
    }

    fun isSfxEnabled(): Boolean {
        return prefs.getBoolean(KEY_SFX_ENABLED, DEFAULT_ENABLED)
    }

    fun setSfxEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_SFX_ENABLED, isEnabled).apply()
    }

    fun isMusicEnabled(): Boolean {
        return prefs.getBoolean(KEY_MUSIC_ENABLED, DEFAULT_ENABLED)
    }

    fun setMusicEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, isEnabled).apply()
    }
}