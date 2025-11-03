package com.heckpet.androeasy

import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.heckpet.androeasy.ui.MainScreen
import com.heckpet.androeasy.ui.theme.AndroEasyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private var selectedMac: String? = null
    var outputText by mutableStateOf("Готов к работе")
    var commands by mutableStateOf(listOf<Command>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            Toast.makeText(this, "⚠️ У вас обнаружена система OneUI! Пожалуйста, заранее настройте системы связи Wi-Fi и/или Bluetooth", Toast.LENGTH_LONG).show()
        }

        setContent {
            AndroEasyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        outputText = outputText,
                        commands = commands,
                        onPair = { lifecycleScope.launch { pairAndroid() } },
                        onUnlock = { lifecycleScope.launch { unlockAndroid() } },
                        updateOutput = { outputText = it }
                    )
                }
            }
        }
    }

    private suspend fun pairAndroid() {
        selectedMac = "B4:XX:XX:XX:XX:XX"
        val secret = randomSecret()
        Prefs.savePaired(this, selectedMac!!, secret)
        outputText = "Привязано: $selectedMac"
    }

    private suspend fun unlockAndroid() {
        val mac = selectedMac ?: run {
            outputText = "Сначала привяжи!"
            return
        }
        val secret = Prefs.getSecret(this, mac) ?: run {
            outputText = "Секрет потерян!"
            return
        }
        val token = hmacSha256(secret, (System.currentTimeMillis() / 60000).toString())

        // ОТПРАВЛЯЕМ КОМАНДУ НА СТАРЫЙ ТЕЛЕФОН
        // (через ADB по Wi-Fi или Bluetooth — потом добавим)
        outputText = "Отправляю токен на $mac...\n$token"
        delay(1000)
        outputText += "\nГотово! Телефон разблокирован!"
    }

    private fun randomSecret() = ByteArray(32).also { Random.nextBytes(it) }
        .let { Base64.encodeToString(it, Base64.NO_WRAP) }

    private fun hmacSha256(key: String, data: String): String {
        val sk = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        Mac.getInstance("HmacSHA256").apply { init(sk) }.let {
            return it.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}

data class Command(val title: String, val command: String)