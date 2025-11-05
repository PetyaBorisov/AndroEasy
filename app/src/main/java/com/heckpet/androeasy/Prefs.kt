package com.heckpet.androeasy

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "easy_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_MODE = "app_mode"

    // --- Easy4Unlock --- //
    fun savePaired(context: Context, mac: String, secret: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("paired_$mac", secret).apply()
    }

    fun getSecret(context: Context, mac: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("paired_$mac", null)
    }

    // --- Настройка приложения --- //
    fun isSetupComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun setSetupComplete(context: Context, isComplete: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, isComplete).apply()
    }

    fun setMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode).apply()
    }

    fun getMode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MODE, null)
    }
}