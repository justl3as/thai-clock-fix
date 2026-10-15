package com.thaiclockfix.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Owns both pipes and the process, including cancellation and partial failures. */
internal class BoundedProcessRunner(
    private val startProcess: (String) -> Process,
    private val timeoutMs: Long = 10_000,
    private val outputLimitBytes: Int = 64 * 1024,
) {
    init {
        require(timeoutMs > 0)
        require(outputLimitBytes > 0)
    }

    suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        ensureActive()
        val process = startProcess(command)
        var stdout: InputStream? = null
        var stderr: InputStream? = null
        try {
            stdout = process.inputStream
            stderr = process.errorStream
            val output = stdout
            val errors = stderr
            withTimeoutOrNull(timeoutMs) {
                coroutineScope {
                    val out = async(Dispatchers.IO) { readLimited(output) }
                    val err = async(Dispatchers.IO) { readLimited(errors) }
                    try {
                        // Shizuku's waitFor is a Binder call and cannot reliably be interrupted.
                        // Poll exitValue instead so cancellation never waits for remote completion.
                        var exit: Int?
                        do {
                            ensureActive()
                            exit =
                                try {
                                    process.exitValue()
                                } catch (_: IllegalThreadStateException) {
                                    null
                                } catch (_: IllegalArgumentException) {
                                    null
                                } catch (_: IllegalStateException) {
                                    null
                                }
                            if (exit == null) delay(20)
                        } while (exit == null)
                        ShellResult(exit == 0, exit, out.await(), err.await())
                    } finally {
                        // Close before coroutineScope joins readers: cancellation must unblock read().
                        runCatching {
                            process.destroy()
                            process.destroyForcibly()
                        }
                        runCatching { output.close() }
                        runCatching { errors.close() }
                    }
                }
            } ?: throw IOException("คำสั่งใช้เวลาเกินกำหนด ($timeoutMs ms)")
        } finally {
            runCatching {
                process.destroy()
                process.destroyForcibly()
            }
            runCatching { stdout?.close() }
            runCatching { stderr?.close() }
            runCatching { process.outputStream.close() }
        }
    }

    private suspend fun readLimited(stream: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val read =
                try {
                    stream.read(buffer)
                } catch (exception: IOException) {
                    // Closing process pipes is how execute() unblocks readers during cancellation.
                    // Preserve the coroutine cancellation instead of leaking the resulting
                    // "stream closed" IOException as an unrelated command failure.
                    currentCoroutineContext().ensureActive()
                    throw exception
                }
            if (read < 0) break
            if (read > outputLimitBytes - output.size()) {
                throw IOException("ผลลัพธ์คำสั่งเกินขนาดที่อนุญาต ($outputLimitBytes bytes ต่อ stream)")
            }
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name()).trim()
    }
}
