// SubscriptionManager.kt
package com.heckpet.androeasy

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Calendar

object SubscriptionManager {
    private const val PREFS_NAME = "sub_prefs"
    private const val KEY_LEVEL = "level" // "free", "pro", "deluxe", "smart"
    private const val KEY_LAST_RESET = "last_reset"
    private const val KEY_PROMPTS_USED = "prompts_used"
    private const val KEY_DU_TIME_USED = "du_time_used_sec"
    private const val KEY_ACTIVE_CONNECTIONS = "active_connections"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // === УРОВНИ ===
    fun getLevel(context: Context): String = prefs(context).getString(KEY_LEVEL, "free") ?: "free"
    fun isFree(context: Context) = getLevel(context) == "free"
    fun isPro(context: Context) = getLevel(context) == "pro"
    fun isDeluxe(context: Context) = getLevel(context) == "deluxe"
    fun isSmart(context: Context) = getLevel(context) == "smart"
    fun isPremium(context: Context) = !isFree(context)

    // === АКТИВАЦИЯ ===
    fun activateCode(context: Context, code: String, onResult: (Boolean, String) -> Unit) {
        when {
            code.matches(Regex("PRO-[A-Z0-9]{4}")) -> setLevel(context, "pro", onResult)
            code.matches(Regex("DLX-[A-Z0-9]{6}")) -> setLevel(context, "deluxe", onResult)
            code.matches(Regex("SM-[A-Z0-9]{8}")) -> setLevel(context, "smart", onResult)
            else -> onResult(false, "Неверный код")
        }
    }

    private fun setLevel(context: Context, level: String, onResult: (Boolean, String) -> Unit) {
        prefs(context).edit {
            putString(KEY_LEVEL, level)
            putLong(KEY_LAST_RESET, 0L) // сбросим лимиты при апгрейде
        }
        onResult(true, "Подписка $level активирована!")
    }

    // === ЛИМИТЫ ===
    fun resetIfNewDay(context: Context) {
        val prefs = prefs(context)
        val last = prefs.getLong(KEY_LAST_RESET, 0)
        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val calLast = Calendar.getInstance().apply { timeInMillis = last }

        if (last == 0L || calNow.get(Calendar.DAY_OF_YEAR) != calLast.get(Calendar.DAY_OF_YEAR) ||
            calNow.get(Calendar.YEAR) != calLast.get(Calendar.YEAR)
        ) {
            if (!isSmart(context)) {
                prefs.edit {
                    putInt(KEY_PROMPTS_USED, 0)
                    putInt(KEY_DU_TIME_USED, 0)
                    putLong(KEY_LAST_RESET, now)
                }
            }
        }
    }

    // Промпты
    fun getDailyPrompts(context: Context): Int = when (getLevel(context)) {
        "free" -> 500
        "pro" -> 1_000
        "deluxe" -> 50_000
        "smart" -> Int.MAX_VALUE
        else -> 0
    }

    fun consumePrompt(context: Context): Boolean {
        resetIfNewDay(context)
        if (isSmart(context)) return true
        val used = prefs(context).getInt(KEY_PROMPTS_USED, 0)
        val max = getDailyPrompts(context)
        if (used >= max) return false
        prefs(context).edit { putInt(KEY_PROMPTS_USED, used + 1) }
        return true
    }

    fun getRemainingPrompts(context: Context): Int {
        resetIfNewDay(context)
        val used = prefs(context).getInt(KEY_PROMPTS_USED, 0)
        return (getDailyPrompts(context) - used).coerceAtLeast(0)
    }

    // ДУ: время
    fun getDailyDuSeconds(context: Context): Long = when (getLevel(context)) {
        "free" -> 3600L     // 1 час
        "pro" -> 18000L     // 5 часов
        "deluxe" -> 54000L  // 15 часов
        "smart" -> Long.MAX_VALUE
        else -> 0
    }

    fun addDuTime(context: Context, seconds: Int): Boolean {
        resetIfNewDay(context)
        if (isSmart(context)) return true
        val used = prefs(context).getInt(KEY_DU_TIME_USED, 0)
        val max = getDailyDuSeconds(context)
        if (used + seconds > max) return false
        prefs(context).edit { putInt(KEY_DU_TIME_USED, used + seconds) }
        return true
    }

    fun getRemainingDuTime(context: Context): String {
        resetIfNewDay(context)
        val used = prefs(context).getInt(KEY_DU_TIME_USED, 0)
        val remaining = getDailyDuSeconds(context) - used
        return if (remaining <= 0) "0 мин" else {
            val h = remaining / 3600
            val m = (remaining % 3600) / 60
            "${h}ч ${m}м"
        }
    }

    // ДУ: подключения
    fun getMaxConnections(context: Context): Int = when (getLevel(context)) {
        "free" -> 1
        "pro" -> 3
        "deluxe" -> 10
        "smart" -> Int.MAX_VALUE
        else -> 0
    }

    fun incrementConnection(context: Context): Boolean {
        val current = prefs(context).getInt(KEY_ACTIVE_CONNECTIONS, 0)
        val max = getMaxConnections(context)
        if (current >= max) return false
        prefs(context).edit { putInt(KEY_ACTIVE_CONNECTIONS, current + 1) }
        return true
    }

    fun decrementConnection(context: Context) {
        val current = prefs(context).getInt(KEY_ACTIVE_CONNECTIONS, 0)
        if (current > 0) {
            prefs(context).edit { putInt(KEY_ACTIVE_CONNECTIONS, current - 1) }
        }
    }

    fun getActiveConnections(context: Context): Int = prefs(context).getInt(KEY_ACTIVE_CONNECTIONS, 0)
}