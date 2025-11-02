package com.heckpet.androeasy

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter

object RootShell {
    private var process: Process? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    fun exec(command: String): String {
        if (process == null) {
            process = Runtime.getRuntime().exec("su")
            writer = PrintWriter(process!!.outputStream)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
        }

        writer!!.println(command)
        writer!!.flush()

        return reader!!.readLine() ?: ""
    }

    fun close() {
        writer?.close()
        reader?.close()
        process?.destroy()
    }
}