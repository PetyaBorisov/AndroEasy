package com.heckpet.androeasy

import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RemoteClient {

    // КЛИЕНТ, ДОВЕРЯЮЩИЙ ВСЕМ СЕРТИФИКАТАМ
    // Это нужно для работы с само-подписанными сертификатами нашего HttpsServer
    private val unsafeOkHttpClient: OkHttpClient by lazy {
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
                .hostnameVerifier { _, _ -> true }.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // ПРОВЕРКА СОЕДИНЕНИЯ
    suspend fun checkConnection(ip: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://$ip:8443/status").build()
        return@withContext try {
            val response = unsafeOkHttpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // ВЫПОЛНЕНИЕ КОМАНДЫ
    suspend fun executeCommand(ip: String, cmd: String): String = withContext(Dispatchers.IO) {
        val url = "https://$ip:8443/exec?cmd=${java.net.URLEncoder.encode(cmd, "UTF-8")}"
        val request = Request.Builder().url(url).build()
        return@withContext try {
            val response = unsafeOkHttpClient.newCall(request).execute()
            response.body?.string() ?: "Ошибка: Пустой ответ от сервера"
        } catch (e: Exception) {
            e.message ?: "Неизвестная сетевая ошибка"
        }
    }
}