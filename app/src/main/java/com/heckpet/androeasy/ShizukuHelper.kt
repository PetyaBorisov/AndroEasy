package com.heckpet.androeasy

import android.content.Context
import rikka.shizuku.Shizuku
import java.io.DataOutputStream

object ShizukuHelper {
    fun isAvailable(): Boolean = try { Shizuku.pingBinder(); true } catch (e: Exception) { false }

    fun requestPermission(context: Context, onGranted: () -> Unit) {
        if (Shizuku.checkSelfPermission() == 0) {
            onGranted()
            return
        }
        Shizuku.addRequestPermissionResultListener(object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (grantResult == 0) onGranted()
                Shizuku.removeRequestPermissionResultListener(this)
            }
        })
        Shizuku.requestPermission(100)
    }

    fun execute(command: String, onResult: (String) -> Unit) {
        Thread {
            try {
                val process = Runtime.getRuntime().exec("sh")
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("export PATH=/system/bin:\$PATH\n")
                os.writeBytes("$command\n")
                os.writeBytes("exit\n")
                os.flush()
                os.close()
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                process.waitFor()
                onResult(if (error.isEmpty()) output else "Ошибка: $error")
            } catch (e: Exception) {
                onResult("Ошибка: ${e.message}")
            }
        }.start()
    }
}