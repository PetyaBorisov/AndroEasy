package com.heckpet.androeasy.du

import okhttp3.*
import okhttp3.CertificatePinner
import android.content.Context
import java.util.concurrent.TimeUnit

object ClientHelper {
    fun createClient(host: String, expectedFingerprint: String): OkHttpClient {
        val pinner = CertificatePinner.Builder()
            .add(host, "sha256/$expectedFingerprint")
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(pinner)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun connectAndVerify(url: String, fingerprint: String): Boolean {
        val host = getHostFromUrl(url)
        val client = createClient(host, fingerprint)
        return try {
            val request = Request.Builder().url("$url/get-cert").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private fun getHostFromUrl(url: String): String {
        return url.substringAfter("https://").substringBefore(":").substringBefore("/")
    }
}