// SetupWizard.kt
package com.heckpet.androeasy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizard(onComplete: (Mode) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedMode by remember { mutableStateOf<Mode?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Добро пожаловать в AndroEasy!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        when (step) {
            1 -> {
                Text("Выберите режим:", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))

                ModeOption("Med to Easy", "Через Shizuku (без Root)", Mode.SHIZUKU, selectedMode) {
                    selectedMode = it
                    step = 2
                }
                ModeOption("Hard to Easy", "Через Root", Mode.ROOT, selectedMode) {
                    selectedMode = it
                    step = 2
                }
                ModeOption("Easy to Easy", "Дистанционное управление", Mode.REMOTE, selectedMode) {
                    selectedMode = it
                    step = 2
                }
            }

            2 -> {
                Text("Настройка: ${selectedMode?.title}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))

                when (selectedMode) {
                    Mode.SHIZUKU -> ShizukuInfo()
                    Mode.ROOT -> RootInfo()
                    Mode.REMOTE -> RemoteInfo()
                    null -> {}
                }

                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { onComplete(selectedMode!!) },
                    enabled = selectedMode != null
                ) {
                    Text("Готово!")
                }
            }
        }
    }
}

@Composable
fun ModeOption(title: String, desc: String, mode: Mode, selected: Mode?, onSelect: (Mode) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        onClick = { onSelect(mode) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// SetupWizard.kt — ДОБАВЬ В ShizukuInfo()

private fun openPlayStoreOrWeb(context: Context) {
    val playUri = Uri.parse("market://details?id=moe.shizuku.privileged.api")
    val playIntent = Intent(Intent.ACTION_VIEW, playUri)
    playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        context.startActivity(playIntent)
    } catch (e: ActivityNotFoundException) {
        // Фоллбек: веб-версия Play
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // На всякий случай
        Toast.makeText(context, "Не удалось открыть магазин", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ShizukuInfo() {
    val context = LocalContext.current

    Column {
        Text("1. Установите Shizuku:", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // КНОПКА: PLAY STORE
            OutlinedButton(
                onClick = {
                    openPlayStoreOrWeb(context)
                }
            ) {
                Text("Play Store")
            }

            // КНОПКА: GITHUB
            OutlinedButton(
                onClick = {
                    val githubUri = Uri.parse("https://github.com/RikkaApps/Shizuku/releases/latest")
                    context.startActivity(Intent(Intent.ACTION_VIEW, githubUri))
                }
            ) {
                Text("GitHub")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("2. Запустите Shizuku и включите:", style = MaterialTheme.typography.bodySmall)
        Text("   • Через ADB: `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh`")
        Text("   • Или через Wireless Debugging")
        Spacer(Modifier.height(8.dp))
        Text("3. Вернитесь и нажмите 'Готово'")
        Spacer(Modifier.height(8.dp))
        Text("AndroEasy проверит доступ автоматически.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun RootInfo() {
    Text("Убедитесь, что устройство рутировано (Magisk и т.д.).\nAndroEasy проверит доступ автоматически.")
}

@Composable
fun RemoteInfo() {
    Text("Оба устройства должны быть в одной Wi-Fi сети.\nПодключение через HTTPS с само-подписанным сертификатом.")
}