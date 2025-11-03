package com.heckpet.androeasy

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun VipDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AndroVIP — 199₽") },
        text = { Text("Безлимит + Grok + Плагины") },
        confirmButton = { Button(onClick = onDismiss) { Text("Оплатить 199₽") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun GodDialog(onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("1000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AndroGOD — 1000₽+") },
        text = {
            Column {
                Text("• ВСЁ НАВСЕГДА\n• Личный дизайн\n• Имя в титрах")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if ((it.toIntOrNull() ?: 0) >= 1000) amount = it },
                    label = { Text("Сумма") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Оплатить $amount₽") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun CodeDialog(context: MainActivity, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Активация по коду") },
        text = {
            Column {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("VIP-XXXX-XXXX") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(message, color = if (message.contains("активирован")) Color.Green else Color.Red)
            }
        },
        confirmButton = {
            Button(onClick = {
                SubscriptionManager.activateCode(context, code) { success, msg ->
                    message = msg
                    if (success) {
                        code = ""
                        onDismiss()
                    }
                }
            }) { Text("Активировать") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Отмена") } }
    )
}