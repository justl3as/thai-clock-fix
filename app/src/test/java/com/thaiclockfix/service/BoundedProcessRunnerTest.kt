package com.thaiclockfix.service

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BoundedProcessRunnerTest {
    private class FixtureProcess(
        private val stdout: InputStream = ByteArrayInputStream(byteArrayOf()),
        private val stderr: InputStream = ByteArrayInputStream(byteArrayOf()),
    ) : Process() {
        var destroyed = false
        override fun getInputStream() = stdout
        override fun getErrorStream() = stderr
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun waitFor() = 0
        override fun exitValue() = 0
        override fun destroy() {
            destroyed = true
            stdout.close()
            stderr.close()
        }
    }

    private class CloseFailureInputStream : InputStream() {
        val entered = CountDownLatch(1)
        private val closed = CountDownLatch(1)

        override fun read(): Int {
            entered.countDown()
            if (!closed.await(2, TimeUnit.SECONDS)) {
                throw IOException("stream was not closed")
            }
            throw IOException("stream closed")
        }

        override fun read(
            bytes: ByteArray,
            offset: Int,
            length: Int,
        ): Int = read()

        override fun close() {
            closed.countDown()
        }
    }

    private class HangingFixtureProcess(
        private val stdout: CloseFailureInputStream,
        private val stderr: CloseFailureInputStream,
    ) : Process() {
        @Volatile var destroyed = false

        override fun getInputStream() = stdout
        override fun getErrorStream() = stderr
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun waitFor(): Int = throw UnsupportedOperationException("waitFor must not be used")
        override fun exitValue(): Int = if (destroyed) 143 else throw IllegalThreadStateException("still running")

        override fun destroy() {
            destroyed = true
            stdout.close()
            stderr.close()
        }

        override fun destroyForcibly(): Process {
            destroy()
            return this
        }
    }

    @Test fun bothStreamsAreDrainedConcurrently() = runBlocking {
        val entered = CountDownLatch(2)
        fun stream(value: String) = object : ByteArrayInputStream(value.toByteArray()) {
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                entered.countDown()
                if (!entered.await(1, TimeUnit.SECONDS)) throw IOException("The other pipe was not drained")
                return super.read(bytes, offset, length)
            }
        }
        val result = BoundedProcessRunner({ FixtureProcess(stream("out"), stream("err")) }).execute("fixture")
        assertEquals("out", result.stdout)
        assertEquals("err", result.stderr)
    }

    @Test fun outputOverflowFailsAndDestroysProcess() = runBlocking {
        val process = FixtureProcess(ByteArrayInputStream(ByteArray(65) { 97 }))
        try {
            BoundedProcessRunner({ process }, outputLimitBytes = 64).execute("fixture")
            fail("Output above the cap must fail")
        } catch (_: IOException) { }
        assertTrue(process.destroyed)
    }

    @Test fun hungProcessTimesOutAndIsDestroyed() = runBlocking {
        val process = ProcessBuilder("/bin/sh", "-c", "exec sleep 1").start()
        try {
            BoundedProcessRunner({ process }, timeoutMs = 100).execute("fixture")
            fail("Command must time out")
        } catch (_: IOException) {
            withTimeout(1_000) { while (process.isAlive) delay(10) }
            assertFalse(process.isAlive)
        } finally {
            process.destroy()
        }
        withTimeout(1_000) { while (process.isAlive) delay(10) }
        assertFalse(process.isAlive)
    }

    @Test fun cancellationDestroysProcessPromptly() = runBlocking {
        // The process would naturally exit after 10s; both waits together (join + death)
        // stay far below that, so passing still proves cancellation killed it early.
        val process = ProcessBuilder("/bin/sh", "-c", "exec sleep 10").start()
        val started = CompletableDeferred<Unit>()
        val job = launch(Dispatchers.IO) {
            BoundedProcessRunner({
                started.complete(Unit)
                process
            }).execute("fixture")
        }
        started.await()
        try {
            job.cancel()
            withTimeout(2_000) { job.join() }
            // destroy() sends SIGTERM; delivery is asynchronous, so poll for the exit.
            withTimeout(2_000) { while (process.isAlive) delay(10) }
            assertFalse(process.isAlive)
        } finally {
            process.destroy()
            job.cancelAndJoin()
        }
    }

    @Test fun cancellationPreservesCancellationWhenClosingPipesFailsReads() = runBlocking {
        val stdout = CloseFailureInputStream()
        val stderr = CloseFailureInputStream()
        val process = HangingFixtureProcess(stdout, stderr)

        supervisorScope {
            val completion = CompletableDeferred<Throwable?>()
            val job = launch(Dispatchers.IO) {
                BoundedProcessRunner({ process }).execute("fixture")
            }
            job.invokeOnCompletion { completion.complete(it) }

            assertTrue(stdout.entered.await(1, TimeUnit.SECONDS))
            assertTrue(stderr.entered.await(1, TimeUnit.SECONDS))

            job.cancel()
            withTimeout(2_000) { job.join() }

            val cause = withTimeout(2_000) { completion.await() }
            assertTrue("pipe-close IOException must not replace cancellation: $cause", cause is CancellationException)
        }

        assertTrue(process.destroyed)
    }

    @Test fun partiallyAcquiredPipesAreClosedWhenAGetterFails() = runBlocking {
        var inputClosed = false
        var outputClosed = false
        var destroyed = false
        val input = object : ByteArrayInputStream(byteArrayOf()) {
            override fun close() {
                inputClosed = true
                super.close()
            }
        }
        val output = object : ByteArrayOutputStream() {
            override fun close() {
                outputClosed = true
                super.close()
            }
        }
        val process = object : Process() {
            override fun getInputStream() = input
            override fun getErrorStream(): InputStream = throw IOException("pipe unavailable")
            override fun getOutputStream() = output
            override fun waitFor() = 0
            override fun exitValue() = 0
            override fun destroy() {
                destroyed = true
            }
        }
        try {
            BoundedProcessRunner({ process }).execute("fixture")
            fail("Pipe acquisition must fail")
        } catch (_: IOException) { }
        assertTrue(inputClosed)
        assertTrue(outputClosed)
        assertTrue(destroyed)
    }

    @Test fun successfulProcessReturnsBothOutputsAndClosesResources() = runBlocking {
        val process = FixtureProcess(ByteArrayInputStream("out".toByteArray()), ByteArrayInputStream("err".toByteArray()))
        val result = BoundedProcessRunner({ process }).execute("fixture")
        assertEquals(ShellResult(true, 0, "out", "err"), result)
        assertTrue(process.destroyed)
    }
}
