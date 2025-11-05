package com.heckpet.androeasy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckpet.androeasy.RemoteClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    drawerState: DrawerState,
    onNavigateToMain: () -> Unit
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
                    selected = false,
                    onClick = onNavigateToMain
                )
                NavigationDrawerItem(
                    label = { Text("Удаленное управление") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } }
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
                    title = { Text("Удаленное управление") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { paddingValues ->
            RemoteContent(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun RemoteContent(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var ipAddress by remember { mutableStateOf("") }
    var remoteCommand by remember { mutableStateOf("") }
    var remoteOutput by remember { mutableStateOf("Ожидание подключения...") }
    var isConnected by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Блок подключения ---
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP-адрес устройства") },
            placeholder = { Text("192.168.1.10") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConnected
        )
        Button(
            onClick = {
                if (isConnected) {
                    isConnected = false
                    remoteOutput = "Отключено."
                } else {
                    scope.launch {
                        remoteOutput = "Подключение..."
                        if (RemoteClient.checkConnection(ipAddress)) {
                            isConnected = true
                            remoteOutput = "Успешно подключено к $ipAddress"
                        } else {
                            remoteOutput = "Не удалось подключиться к $ipAddress. Убедитесь, что на удаленном устройстве запущен HTTPS сервер AndroEasy."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isConnected) "Отключиться" else "Подключиться")
        }

        Spacer(Modifier.height(24.dp))

        // --- Блок управления (виден только после подключения) ---
        if (isConnected) {
            OutlinedTextField(
                value = remoteCommand,
                onValueChange = { remoteCommand = it },
                label = { Text("Команда для удаленного устройства") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        remoteOutput = "Выполнение..."
                        val result = RemoteClient.executeCommand(ipAddress, remoteCommand)
                        remoteOutput = result
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выполнить удаленно")
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Вывод ---
        Text("Вывод:", style = MaterialTheme.typography.titleMedium)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp)
        ) {
            Text(
                text = remoteOutput,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}