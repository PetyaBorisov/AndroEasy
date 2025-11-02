package com.heckpet.androeasy

import android.content.Context
import android.content.SharedPreferences

object VipManager {
    private const val PREFS_NAME = "vip_prefs"
    private const val KEY_IS_VIP = "is_vip"
    private const val KEY_IS_GOD = "is_god"
    private const val KEY_CODE = "vip_code"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun activateCode(context: Context, code: String, onResult: (Boolean, String) -> Unit) {
        when {
            code.matches(Regex("VIP-[A-Z0-9]{4}-[A-Z0-9]{4}")) -> {
                save(context, code, false)
                onResult(true, "AndroVIP активирован!")
            }
            code.matches(Regex("GOD-[A-Z0-9]{10}")) -> {
                save(context, code, true)
                onResult(true, "ANDROGOD активирован!")
            }
            else -> onResult(false, "Неверный формат кода")
        }
    }

    private fun save(context: Context, code: String, isGod: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_VIP, true)
            .putBoolean(KEY_IS_GOD, isGod)
            .putString(KEY_CODE, code)
            .apply()
    }

    fun isVip(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_VIP, false)
    fun isGod(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_GOD, false)
}