// GigaChatApi.kt
package com.heckpet.androeasy

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import com.heckpet.androeasy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object GigaChatApi {
    private const val TAG = "GigaChatApi"
    private const val AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private const val CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private const val SCOPE = "GIGACHAT_API_PERS"  // Для физлиц; для бизнеса — GIGACHAT_API_CORP

    private val CLIENT_ID = BuildConfig.GIGACHAT_ID
    private val CLIENT_SECRET = BuildConfig.GIGACHAT_SECRET

    // УЛУЧШЕНИЕ: Создаем клиент, доверяющий всем сертификатам (включая Минцифры)
    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(@SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

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
                tokenExpiry = System.currentTimeMillis() + (json.optLong("expires_in", 3600) * 1000 - 60000)
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

    suspend fun generateCommand(query: String): String = withContext(Dispatchers.IO) {
        if (CLIENT_ID.isBlank() || CLIENT_SECRET.isBlank()) {
            Log.e(TAG, "GigaChat ID/Secret не заданы в local.properties!")
            return@withContext "Ошибка: Ключи GigaChat не настроены. Добавьте их в local.properties и пересоберите проект."
        }

        val token = getAccessToken() ?: return@withContext "Ошибка: Не удалось авторизоваться в GigaChat"

        val prompt = """
            Ты эксперт по Android, ADB и Root. Пользователь попросил: "$query".
            Ответь ТОЛЬКО одной командой shell (bash/adb), без объяснений.
            Если это ADB-команда, используй 'adb shell'.
            Пример: для "перезагрузка" → "reboot".
        """.trimIndent()

        val requestBody = JSONObject().apply {
            put("model", "GigaChat:latest")
            put("messages", arrayOf(
                JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }
            ))
            put("temperature", 0.3)
            put("max_tokens", 50)
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