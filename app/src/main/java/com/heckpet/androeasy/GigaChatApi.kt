// GigaChatApi.kt
package com.heckpet.androeasy

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GigaChatApi {
    private const val TAG = "GigaChatApi"
    private const val AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private const val CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private const val SCOPE = "GIGACHAT_API_PERS"  // Для физлиц; для бизнеса — GIGACHAT_API_CORP

    // ← ТВОИ ДАННЫЕ ИЗ STUDIO (храни в SharedPrefs или secrets!)
    private const val CLIENT_ID = BuildConfig.GIGACHAT_ID  // Из Studio
    private const val CLIENT_SECRET = BuildConfig.GIGACHAT_SECRET  // Из Studio

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()  // Для теста; в релизе — убрать логи

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    // Получаем токен (кешируем на 3600 сек)
    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return@withContext accessToken
        }

        val credentials = "$CLIENT_ID:$CLIENT_SECRET"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val requestBody = JSONObject().apply {
            put("scope", SCOPE)
        }.toString().toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(AUTH_URL)
            .post(requestBody)
            .addHeader("Authorization", "Basic $encoded")
            .addHeader("RqUID", java.util.UUID.randomUUID().toString())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                accessToken = json.optString("access_token")
                tokenExpiry = System.currentTimeMillis() + (json.optLong("expires_in", 3600) * 1000 - 60000)  // минус 1 мин на обновление
                Log.d(TAG, "Токен получен: ${accessToken?.take(20)}...")
                accessToken
            } else {
                Log.e(TAG, "Ошибка авторизации: ${response.code} - ${response.body?.string()}")
                null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка сети: ${e.message}")
            null
        }
    }

    // Генерация команды (основной метод)
    suspend fun generateCommand(query: String): String = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext "Ошибка: Не удалось авторизоваться в GigaChat"

        val prompt = """
            Ты эксперт по Android, ADB и Root. Пользователь попросил: "$query".
            Ответь ТОЛЬКО одной командой shell (bash/adb), без объяснений.
            Если это ADB-команда, используй 'adb shell'.
            Пример: для "перезагрузка" → "reboot".
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("model", "GigaChat:latest")  // Или "GigaChat-Pro"
            put("messages", arrayOf(
                JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }
            ))
            put("temperature", 0.3)  // Низкая для точности
            put("max_tokens", 50)    // Короткий ответ
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(CHAT_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("RqUID", java.util.UUID.randomUUID().toString())
            .build()

        return@withContext try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val content = json.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    ?: "Ошибка: Пустой ответ от ИИ"

                Log.d(TAG, "ИИ сгенерировал: $content")
                content
            } else {
                Log.e(TAG, "Ошибка GigaChat: ${response.code} - ${response.body?.string()}")
                "Ошибка ИИ: ${response.code}"
            }
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка сети: ${e.message}")
            "Ошибка сети"
        }
    }
}