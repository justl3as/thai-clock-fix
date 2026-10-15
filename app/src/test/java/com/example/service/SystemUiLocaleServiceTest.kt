package com.example.service

import com.example.model.LocaleMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SystemUiLocaleServiceTest {
    private val readback = "Locales for com.android.systemui for user 0 are [en-US]"
    private fun ok(stdout: String = "") = ShellResult(true, 0, stdout, "")
    private fun failed(stdout: String = "") = ShellResult(false, 1, stdout, "denied")
    private fun service(vararg results: ShellResult): ShizukuSystemUiLocaleService {
        val remaining = ArrayDeque(results.toList())
        return ShizukuSystemUiLocaleService { remaining.removeFirst() }
    }

    @Test fun failedResetCannotReportSuccessEvenWhenLocaleIsEmpty() = runBlocking {
        val result = service(failed(), ok("[]")).restoreDefault().getOrThrow()
        assertFalse(result.isSuccess)
        assertFalse(result.isVerified)
    }

    @Test fun failedVerificationWithEmptyOutputCannotMeanDefault() = runBlocking {
        val result = service(ok(), failed()).restoreDefault().getOrThrow()
        assertFalse(result.isSuccess)
        assertFalse(result.isVerified)
    }

    @Test fun blankOrUnrelatedOutputCannotMeanDefault() = runBlocking {
        for (output in listOf("", "Unknown package com.android.systemui for userId 0", "error []")) {
            val result = service(ok(), ok(output)).restoreDefault().getOrThrow()
            assertFalse(output, result.isSuccess)
        }
    }

    @Test fun exactEmptyLocaleListVerifiesRestore() = runBlocking {
        for (output in listOf("[]", "Locales for com.android.systemui for user 0 are []")) {
            val result = service(ok(), ok(output)).restoreDefault().getOrThrow()
            assertTrue(result.isSuccess)
            assertTrue(result.isVerified)
            assertEquals("system_default", result.verifiedLocale)
        }
    }

    @Test fun failedTimeSettingCannotBeHiddenByMatchingLocale() = runBlocking {
        assertFalse(service(failed(), ok(), ok(readback)).applyLocale(LocaleMode.ENGLISH).getOrThrow().isSuccess)
    }

    @Test fun failedSetCannotBeHiddenByExistingMatchingLocale() = runBlocking {
        assertFalse(service(ok(), failed(), ok(readback)).applyLocale(LocaleMode.ENGLISH).getOrThrow().isSuccess)
    }

    @Test fun failedReadbackCannotBeHiddenByMatchingOutput() = runBlocking {
        val result = service(ok(), ok(), failed(readback)).applyLocale(LocaleMode.ENGLISH).getOrThrow()
        assertFalse(result.isSuccess)
        assertFalse(result.isVerified)
    }

    @Test fun substringOrWrongResourceReadbackDoesNotVerify() = runBlocking {
        for (output in listOf("[en-US-x-wrong]", "error: expected en-US", "Locales for other.app for user 0 are [en-US]", "Locales for com.android.systemui for user 10 are [en-US]")) {
            assertFalse(output, service(ok(), ok(), ok(output)).applyLocale(LocaleMode.ENGLISH).getOrThrow().isSuccess)
        }
    }

    @Test fun successfulApplyRequiresTimeFormatReadback() = runBlocking {
        val commands = mutableListOf<String>()
        val service = ShizukuSystemUiLocaleService { command ->
            commands += command
            when {
                "get-app-locales" in command -> ok(readback)
                "settings" in command && "get" in command -> ok("12")
                else -> ok()
            }
        }
        assertFalse(service.applyLocale(LocaleMode.ENGLISH).getOrThrow().isSuccess)
        assertTrue(commands.any { "settings" in it && "get" in it })
    }

    @Test fun exactLocaleAnd24HourReadbacksVerifyApplyForOwnerUser() = runBlocking {
        val commands = mutableListOf<String>()
        val service = ShizukuSystemUiLocaleService { command ->
            commands += command
            when {
                "get-app-locales" in command -> ok(readback)
                "settings" in command && "get" in command -> ok("24")
                else -> ok()
            }
        }
        val result = service.applyLocale(LocaleMode.ENGLISH).getOrThrow()
        assertTrue(result.isSuccess)
        assertTrue(result.isVerified)
        assertEquals("en-US", result.verifiedLocale)
        assertTrue(commands.all { "--user 0" in it })
    }

    @Test fun blankQueryResultIsFailureNotDefault() = runBlocking {
        assertTrue(service(ok()).getCurrentSystemUiLocale().isFailure)
    }

    @Test fun settingsSnapshotReadsTimeFormatFromTheOwnerUser() = runBlocking {
        assertEquals(SystemUiSettings("en-US", true), service(ok(readback), ok("24")).getCurrentSystemUiSettings().getOrThrow())
        assertEquals(SystemUiSettings("en-US", false), service(ok(readback), ok("12")).getCurrentSystemUiSettings().getOrThrow())
        assertEquals(SystemUiSettings("en-US", null), service(ok(readback), ok("null")).getCurrentSystemUiSettings().getOrThrow())
    }

    @Test fun unreadableTimeFormatCannotProduceVerifiedSnapshot() = runBlocking {
        assertTrue(service(ok(readback), failed()).getCurrentSystemUiSettings().isFailure)
        assertTrue(service(ok(readback), ok("")).getCurrentSystemUiSettings().isFailure)
    }

    @Test fun cancellationPropagatesInsteadOfBecomingAResult() = runBlocking {
        val service = ShizukuSystemUiLocaleService { throw CancellationException("cancelled") }
        try {
            service.restoreDefault()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
