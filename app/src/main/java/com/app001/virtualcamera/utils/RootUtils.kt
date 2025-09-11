package com.app001.virtualcamera.utils

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootUtils {
    private const val TAG = "RootUtils"

    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { path ->
            try {
                java.io.File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun checkRootMethod3(): Boolean {
        return try {
            Runtime.getRuntime().exec("su").waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun executeCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = process.inputStream
            val errorStream = process.errorStream

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            process.waitFor()

            val output = readStream(inputStream)
            val error = readStream(errorStream)

            if (error.isNotEmpty()) {
                Log.e(TAG, "Command error: $error")
            }

            output
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${e.message}")
            ""
        }
    }

    private fun readStream(inputStream: java.io.InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }

        return output.toString()
    }
}