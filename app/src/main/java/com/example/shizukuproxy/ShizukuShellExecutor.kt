package com.example.shizukuproxy

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.DataOutputStream

/**
 * Handles privileged shell queries via Shizuku binder layer.
 * Specifically aims to locate the softAP interface gateway.
 */
object ShizukuShellExecutor {

    private const val TAG = "ShizukuShellExecutor"

    /**
     * Checks if Shizuku binder is online and permissions are currently approved.
     */
    fun isShizukuAvailable(context: Context): Boolean {
        if (!Shizuku.pingBinder()) {
            return false
        }
        if (Shizuku.isPreV11()) {
            return false
        }
        return Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries routing tables on Android via Shizuku shell.
     * Executes 'ip r' and parses the active gateway matching 'ap0' interface.
     */
    fun getHotspotGatewayIp(onResult: (String?) -> Unit) {
        if (Shizuku.getVersion() < 11) {
            Log.e(TAG, "Shizuku API level 11+ is required")
            onResult(null)
            return
        }

        Thread {
            try {
                // Instantiates a privileged shell process via Shizuku
                val process = Shizuku.newProcess(arrayOf("sh"), null, null)
                val os = DataOutputStream(process.outputStream)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errReader = BufferedReader(InputStreamReader(process.errorStream))

                // Requesting routing rules
                val command = "ip r"
                os.writeBytes(command + "\n")
                os.writeBytes("exit\n")
                os.flush()

                val outputLines = ArrayList<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputLines.add(line!!)
                }
                process.waitFor()

                os.close()
                reader.close()
                errReader.close()

                // Parse the command output streams for ap0 (tethering interfaces)
                val gatewayIp = parseOutputForAp0(outputLines)

                Handler(Looper.getMainLooper()).post {
                    onResult(gatewayIp)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errors invoking Shizuku command execution helper", e)
                Handler(Looper.getMainLooper()).post {
                    onResult(null)
                }
            }
        }.start()
    }

    /**
     * Parse system stdout matches. Equivalent to: grep ap0 | awk '{print $NF}'
     */
    private fun parseOutputForAp0(lines: List<String>): String? {
        for (line in lines) {
            val clean = line.trim()
            // Look for softAP interfaces: "ap0" (standard on modern Snapdragon devices)
            if (clean.contains("ap0") || clean.contains("wlan1") || clean.contains("wlan0")) {
                val parts = clean.split("\\s+".toRegex())
                if (parts.isNotEmpty()) {
                    val srcIndex = parts.indexOf("src")
                    if (srcIndex != -1 && srcIndex + 1 < parts.size) {
                        return parts[srcIndex + 1]
                    }
                    return parts.last() // falls back to trailing awk field
                }
            }
        }
        return null
    }
}
