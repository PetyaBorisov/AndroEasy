package com.heckpet.androeasy

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.heckpet.androeasy.ui.MainScreen
import com.heckpet.androeasy.ui.RemoteScreen
import com.heckpet.androeasy.ui.theme.AndroEasyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

data class PairedDeviceInfo(val name: String, val address: String)

// УЛУЧШЕНИЕ: Enum для навигации
enum class Screen {
    MAIN,
    REMOTE
}

class MainActivity : ComponentActivity() {

    var outputText by mutableStateOf("Готов к работе")
    var commands by mutableStateOf(listOf<Command>())
    var pairedDevices by mutableStateOf<List<PairedDeviceInfo>>(emptyList())
    var showDeviceListDialog by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchPairedDevices()
        } else {
            outputText = "Разрешение на доступ к Bluetooth отклонено. Функция недоступна."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            Toast.makeText(this, "⚠️ У вас обнаружена система OneUI! Пожалуйста, заранее настройте системы связи Wi-Fi и/или Bluetooth", Toast.LENGTH_LONG).show()
        }

        setContent {
            AndroEasyTheme {
                // УЛУЧШЕНИЕ: Добавляем состояние для навигации
                var currentScreen by remember { mutableStateOf(Screen.MAIN) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                when (currentScreen) {
                    Screen.MAIN -> {
                        MainScreen(
                            drawerState = drawerState,
                            outputText = outputText,
                            commands = commands,
                            pairedDevices = pairedDevices,
                            showDeviceDialog = showDeviceListDialog,
                            onShowPairedDevices = { startDeviceDiscovery() },
                            onPair = { macAddress -> lifecycleScope.launch { pairAndroid(macAddress) } },
                            onUnlock = { macAddress -> lifecycleScope.launch { unlockAndroid(macAddress) } },
                            updateOutput = { outputText = it },
                            onDismissDialog = { showDeviceListDialog = false },
                            onNavigateToRemote = { currentScreen = Screen.REMOTE } // ← Навигация
                        )
                    }
                    Screen.REMOTE -> {
                        RemoteScreen(
                            drawerState = drawerState,
                            onNavigateToMain = { currentScreen = Screen.MAIN } // ← Навигация
                        )
                    }
                }
            }
        }
    }

    private fun startDeviceDiscovery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED -> {
                    fetchPairedDevices()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else {
            fetchPairedDevices()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchPairedDevices() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter == null) {
            outputText = "На этом устройстве нет Bluetooth"
            return
        }

        val devices = bluetoothAdapter.bondedDevices
        if (devices.isEmpty()) {
            outputText = "Нет сопряженных устройств. Сначала подключите нужное устройство в системных настройках Bluetooth."
            return
        }

        pairedDevices = devices.map { PairedDeviceInfo(it.name ?: "Без имени", it.address) }
        showDeviceListDialog = true
    }

    private suspend fun pairAndroid(mac: String) {
        val secret = randomSecret()
        Prefs.savePaired(this, mac, secret)
        showDeviceListDialog = false
        outputText = "Устройство $mac успешно привязано!"
    }

    private suspend fun unlockAndroid(mac: String) {
        if (mac.isBlank()) {
            outputText = "Ошибка: Введите MAC-адрес или выберите устройство для разблокировки."
            return
        }
        val secret = Prefs.getSecret(this, mac) ?: run {
            outputText = "Устройство $mac не найдено. Сначала привяжите его."
            return
        }
        val token = hmacSha256(secret, (System.currentTimeMillis() / 60000).toString())

        outputText = "Отправляю токен на $mac...\n$token"
        delay(1000)
        outputText += "\nГотово! Телефон разблокирован!"
    }

    private fun randomSecret() = ByteArray(32).also { Random.nextBytes(it) }.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    private fun hmacSha256(key: String, data: String): String {
        val sk = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        Mac.getInstance("HmacSHA256").apply { init(sk) }.let {
            return it.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}