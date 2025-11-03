package com.heckpet.androeasy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.heckpet.androeasy.*
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    outputText: String,
    commands: List<Command>,
    onPair: () -> Unit,
    onUnlock: () -> Unit,
    updateOutput: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var directCommand by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Text("AndroEasy RRO1", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        // === EASY4UNLOCK ===
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Easy4Unlock", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onPair) { Text("Привязать") }
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = onUnlock) { Text("Открыть") }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // === ИИ + ПОИСК ===
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Что сделать?") },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            Button(onClick = {
                scope.launch {
                    updateOutput("ИИ думает...")
                    val result = CommandExecutor.executeWithAI(context, query)
                    updateOutput(result)
                }
            }) {
                Text("Спросить ИИ")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                scope.launch {
                    val db = CommandDatabase.getInstance(context)
                    val results = db.commandDao().search(query)
                    updateOutput("Найдено: ${results.size}")
                }
            }) {
                Text("Поиск в БД")
            }
        }

        Spacer(Modifier.height(16.dp))

        // === ПРЯМАЯ КОМАНДА ===
        OutlinedTextField(
            value = directCommand,
            onValueChange = { directCommand = it },
            label = { Text("Прямая команда") },
            placeholder = { Text("pm list packages") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch {
                val result = RootShell.exec(directCommand)
                updateOutput(result)
            }
        }) {
            Text("Выполнить")
        }

        Spacer(Modifier.height(16.dp))

        // === HTTPS СЕРВЕР ===
        Button(onClick = {
            scope.launch {
                try {
                    HttpsServer(context)
                    updateOutput("HTTPS сервер запущен на https://${getLocalIp()}:8443")
                } catch (e: Exception) {
                    updateOutput("Ошибка сервера: ${e.message}")
                }
            }
        }) {
            Text("Запустить HTTPS сервер")
        }

        Spacer(Modifier.height(24.dp))

        // === ТОП КОМАНД ===
        Text("Топ команд", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(commands) { cmd ->
                Card(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cmd.title, style = MaterialTheme.typography.titleMedium)
                            Text(cmd.command, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val result = RootShell.exec(cmd.command)
                                updateOutput(result)
                            }
                        }) {
                            Text("Run")
                        }
                    }
                }
            }
        }

        // === ВЫВОД ===
        if (outputText.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = outputText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        StatusBar()
    }
}

@Composable
fun StatusBar() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("RRO1 • RuStore Ready", style = MaterialTheme.typography.titleMedium)
        Text("Подписка: ${SubscriptionManager.getLevel(context).uppercase()}")
        Text("Бот: @AndroEasyBot")
    }
}