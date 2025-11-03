// RootShell.kt
package com.heckpet.androeasy

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.lang.Exception

object RootShell {
    private var process: Process? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var errorReader: BufferedReader? = null
    private const val TAG = "RootShell"

    // Инициализация с проверкой Root
    fun init(): Boolean {
        try {
            process = Runtime.getRuntime().exec("su")
            writer = PrintWriter(process!!.outputStream)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            errorReader = BufferedReader(InputStreamReader(process!!.errorStream))

            // Проверка: "id" возвращает uid=0?
            val checkOutput = exec("id")
            if (checkOutput.contains("uid=0")) {
                Log.d(TAG, "Root доступ получен")
                return true
            } else {
                Log.e(TAG, "Root не доступен: $checkOutput")
                close()
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации Root: ${e.message}", e)
            close()
            return false
        }
    }

    fun exec(command: String): String {
        if (process == null && !init()) {
            return "Root недоступен"
        }

        return try {
            writer!!.println(command)
            writer!!.println("echo ---END---")  // Маркер конца
            writer!!.flush()

            val output = StringBuilder()
            var line: String?
            while (reader!!.readLine().also { line = it } != null) {
                if (line == "---END---") break
                output.append(line).append("\n")
            }

            val error = StringBuilder()
            while (errorReader!!.ready()) {
                error.append(errorReader!!.readLine()).append("\n")
            }

            if (error.isNotEmpty()) {
                "Ошибка: $error"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка выполнения: ${e.message}", e)
            "Ошибка: ${e.message}"
        }
    }

    fun close() {
        try {
            writer?.close()
            reader?.close()
            errorReader?.close()
            process?.destroy()
            process = null
            Log.d(TAG, "RootShell закрыт")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка закрытия: ${e.message}", e)
        }
    }
}