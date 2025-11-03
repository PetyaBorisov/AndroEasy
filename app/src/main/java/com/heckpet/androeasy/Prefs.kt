package com.heckpet.androeasy

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "easy_prefs"

    fun savePaired(context: Context, mac: String, secret: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("paired_$mac", secret).apply()
    }

    fun getSecret(context: Context, mac: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("paired_$mac", null)
    }
}