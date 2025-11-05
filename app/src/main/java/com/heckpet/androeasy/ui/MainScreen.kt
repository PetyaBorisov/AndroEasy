package com.heckpet.androeasy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.heckpet.androeasy.*
import com.heckpet.androeasy.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    drawerState: DrawerState,
    outputText: String,
    commands: List<Command>,
    pairedDevices: List<PairedDeviceInfo>,
    showDeviceDialog: Boolean,
    onShowPairedDevices: () -> Unit,
    onPair: (String) -> Unit,
    onUnlock: (String) -> Unit,
    updateOutput: (String) -> Unit,
    onDismissDialog: () -> Unit,
    onNavigateToRemote: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("AndroEasy", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                Divider()
                NavigationDrawerItem(
                    label = { Text("Главный экран") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    label = { Text("Удаленное управление") },
                    selected = false,
                    onClick = onNavigateToRemote
                )
                NavigationDrawerItem(
                    label = { Text("Сообщество") },
                    selected = false,
                    onClick = { /* TODO: Firebase Community Screen */ }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AndroEasy ${BuildConfig.VERSION_NAME}") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { paddingValues ->
            MainContent(
                modifier = Modifier.padding(paddingValues),
                outputText = outputText,
                commands = commands,
                pairedDevices = pairedDevices,
                showDeviceDialog = showDeviceDialog,
                onShowPairedDevices = onShowPairedDevices,
                onPair = onPair,
                onUnlock = onUnlock,
                updateOutput = updateOutput,
                onDismissDialog = onDismissDialog
            )
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    outputText: String,
    commands: List<Command>,
    pairedDevices: List<PairedDeviceInfo>,
    showDeviceDialog: Boolean,
    onShowPairedDevices: () -> Unit,
    onPair: (String) -> Unit,
    onUnlock: (String) -> Unit,
    updateOutput: (String) -> Unit,
    onDismissDialog: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var directCommand by remember { mutableStateOf("") }
    var httpsServer by remember { mutableStateOf<HttpsServer?>(null) }
    var unlockTargetMac by remember { mutableStateOf<String?>(null) } // ← УЛУЧШЕНИЕ

    if (showDeviceDialog) {
        DeviceListDialog(
            devices = pairedDevices,
            onDismiss = onDismissDialog,
            onDeviceSelected = {
                // УЛУЧШЕНИЕ: Сохраняем MAC в отдельную переменную, не трогая directCommand
                unlockTargetMac = it
                onPair(it)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // === EASY4UNLOCK ===
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Easy4Unlock", style = MaterialTheme.typography.titleLarge)
                // УЛУЧШЕНИЕ: Отображаем выбранное устройство
                Text("Цель: ${unlockTargetMac ?: "Не выбрана"}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onShowPairedDevices) { Text("Выбрать цель") } // ← УЛУЧШЕНИЕ
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = { onUnlock(unlockTargetMac ?: "") }) { Text("Открыть") } // ← УЛУЧШЕНИЕ
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
            label = { Text("Прямая команда") }, // ← УЛУЧШЕНИЕ
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
                if (httpsServer == null) {
                    try {
                        val server = HttpsServer(context).apply { start() }
                        httpsServer = server
                        updateOutput("HTTPS сервер запущен на https://${getLocalIp()}:8443")
                    } catch (e: Exception) {
                        updateOutput("Ошибка сервера: ${e.message}")
                    }
                } else {
                    httpsServer?.stop()
                    httpsServer = null
                    updateOutput("HTTPS сервер остановлен.")
                }
            }
        }) {
            Text(if (httpsServer == null) "Запустить HTTPS сервер" else "Остановить HTTPS сервер")
        }

        Spacer(Modifier.height(24.dp))

        // === ТОП КОМАНД ===
        Text("Топ команд", style = MaterialTheme.typography.titleLarge)
        LazyColumn(
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
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

        Spacer(Modifier.height(16.dp))

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
fun DeviceListDialog(
    devices: List<PairedDeviceInfo>,
    onDismiss: () -> Unit,
    onDeviceSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text("Выберите устройство для привязки", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(devices) { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device.address) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(device.name, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Text(device.address, style = MaterialTheme.typography.bodyMedium)
                        }
                        Divider()
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(8.dp)) {
                    Text("ОТМЕНА")
                }
            }
        }
    }
}

@Composable
fun StatusBar() {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("${BuildConfig.VERSION_NAME} • RuStore Ready", style = MaterialTheme.typography.titleMedium)
        Text("Подписка: ${SubscriptionManager.getLevel(context).uppercase()}")
        Text("Бот: @AndroEasyBot")
    }
}