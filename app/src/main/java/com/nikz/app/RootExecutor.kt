package com.nikz.app

import android.util.Log
import java.io.File

object RootExecutor {

    private const val TAG = "RootExec"

    data class Result(
        val success: Boolean,
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    fun isRootAvailable(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            out.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun run(binary: File, args: String = ""): Result {
        val libDir = binary.parentFile!!.absolutePath
        val command = buildString {
            append("cd $libDir && ")
            append("LD_LIBRARY_PATH=$libDir ")
            append(binary.absolutePath)
            if (args.isNotEmpty()) append(" $args")
        }
        Log.d(TAG, "Running: $command")
        return execRoot(command)
    }

    fun shell(command: String): Result = execRoot(command)

    private fun execRoot(command: String): Result {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(false)
                .start()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (stdout.isNotEmpty()) Log.d(TAG, "STDOUT: $stdout")
            if (stderr.isNotEmpty()) Log.w(TAG, "STDERR: $stderr")
            Result(exit == 0, exit, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            Result(false, -1, "", e.message ?: "unknown error")
        }
    }
}
