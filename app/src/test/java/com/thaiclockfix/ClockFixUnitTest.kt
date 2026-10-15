package com.thaiclockfix

import com.thaiclockfix.model.LocaleMode
import com.thaiclockfix.service.ShizukuSystemUiLocaleService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ClockFixUnitTest {
    @Test
    fun testLocaleModeTags() {
        assertEquals("en-US", LocaleMode.ENGLISH.tag)
        assertEquals("th-TH-u-ca-buddhist", LocaleMode.THAI_BUDDHIST.tag)
    }

    @Test
    fun testFromTagMapping() {
        assertEquals(LocaleMode.ENGLISH, LocaleMode.fromTag("en-US"))
        assertEquals(LocaleMode.THAI_BUDDHIST, LocaleMode.fromTag("th-TH-u-ca-buddhist"))

        // With bracket or extra info
        assertEquals(LocaleMode.THAI_BUDDHIST, LocaleMode.fromTag("[th-TH-u-ca-buddhist]"))
        assertEquals(null, LocaleMode.fromTag("unknown-tag"))
        assertEquals(null, LocaleMode.fromTag(null))
        assertEquals(null, LocaleMode.fromTag(""))
    }

    @Test
    fun test24HourTimeFormattingWithoutSuffix() {
        // 20:45:15
        val testTime = LocalDateTime.of(2026, 10, 14, 20, 45, 15)

        LocaleMode.entries.forEach { mode ->
            val time24h = mode.formatTime24h(testTime)
            assertEquals("20:45", time24h)
            assertFalse("Time should not contain Thai 'น.'", time24h.contains("น."))
        }
    }

    @Test
    fun testDateFormattingAcrossModes() {
        // 14 October 2026 -> Buddhist Year 2569
        val testDate = LocalDateTime.of(2026, 10, 14, 12, 0, 0)

        val enDate = LocaleMode.ENGLISH.formatDatePreview(testDate)
        assertTrue("English date should contain 2026", enDate.contains("2026"))
        assertTrue("English date should contain Oct", enDate.contains("Oct"))

        val thaiBudDate = LocaleMode.THAI_BUDDHIST.formatDatePreview(testDate)
        assertTrue("Thai Buddhist date should contain พ.ศ. 2569", thaiBudDate.contains("2569"))
        assertTrue("Thai Buddhist date should contain ต.ค.", thaiBudDate.contains("ต.ค."))
    }

    @Test
    fun testAdbCommandsGeneration() {
        val service = ShizukuSystemUiLocaleService()

        val thaiBuddhistCmds = service.getAdbCommands(LocaleMode.THAI_BUDDHIST)
        assertEquals(3, thaiBuddhistCmds.size)
        assertEquals("adb shell settings --user 0 put system time_12_24 24", thaiBuddhistCmds[0])
        assertEquals(
            "adb shell cmd locale set-app-locales com.android.systemui --user 0 --locales th-TH-u-ca-buddhist",
            thaiBuddhistCmds[1],
        )
        assertEquals("adb shell cmd locale get-app-locales com.android.systemui --user 0", thaiBuddhistCmds[2])

        val restoreCmds = service.getAdbCommands(null)
        assertEquals(2, restoreCmds.size)
        assertEquals("adb shell cmd locale set-app-locales com.android.systemui --user 0 --locales \"\"", restoreCmds[0])
        assertEquals("adb shell cmd locale get-app-locales com.android.systemui --user 0", restoreCmds[1])
    }

    @Test
    fun localePrefixMustNotDisableApplyForADifferentLocale() {
        for (actual in listOf("en-US-x-wrong", "en-US,th-TH-u-ca-buddhist")) {
            val action =
                com.thaiclockfix.model.PrimaryAction.resolve(
                    com.thaiclockfix.shizuku.ShizukuStatus.READY,
                    LocaleMode.ENGLISH,
                    actual,
                    isApplying = false,
                    hasError = false,
                )
            assertEquals(com.thaiclockfix.model.PrimaryAction.Apply, action)
        }
    }

    @Test
    fun matchingLocaleRequiresConfirmed24HourTimeBeforeDisablingApply() {
        for (time in listOf<Boolean?>(false, null)) {
            assertEquals(
                com.thaiclockfix.model.PrimaryAction.Apply,
                com.thaiclockfix.model.PrimaryAction.resolve(
                    com.thaiclockfix.shizuku.ShizukuStatus.READY,
                    LocaleMode.ENGLISH,
                    "en-US",
                    isApplying = false,
                    hasError = false,
                    is24Hour = time,
                ),
            )
        }
    }

    @Test
    fun omittedTimeReadbackMustNotAssume24HourTime() {
        assertEquals(
            com.thaiclockfix.model.PrimaryAction.Apply,
            com.thaiclockfix.model.PrimaryAction.resolve(
                com.thaiclockfix.shizuku.ShizukuStatus.READY,
                LocaleMode.ENGLISH,
                "en-US",
                isApplying = false,
                hasError = false,
            ),
        )
    }

    @Test
    fun testPrimaryActionResolution() {
        val thai = LocaleMode.THAI_BUDDHIST
        val en = LocaleMode.ENGLISH

        // 1. Applying state
        val applyingAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.READY,
                selectedMode = thai,
                actualSystemUiLocale = null,
                isApplying = true,
                hasError = false,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.Applying, applyingAction)
        assertFalse(applyingAction.enabled)

        // 2. Shizuku not installed
        val notInstalledAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.NOT_INSTALLED,
                selectedMode = thai,
                actualSystemUiLocale = null,
                isApplying = false,
                hasError = false,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.InstallShizuku, notInstalledAction)

        // 3. Shizuku stopped
        val stoppedAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.SERVICE_STOPPED,
                selectedMode = thai,
                actualSystemUiLocale = null,
                isApplying = false,
                hasError = false,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.StartShizuku, stoppedAction)

        // 4. Shizuku permission required
        val permAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.PERMISSION_REQUIRED,
                selectedMode = thai,
                actualSystemUiLocale = null,
                isApplying = false,
                hasError = false,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.GrantPermission, permAction)

        // 5. Already applied
        val appliedAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.READY,
                selectedMode = thai,
                actualSystemUiLocale = "th-TH-u-ca-buddhist",
                isApplying = false,
                hasError = false,
                is24Hour = true,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.AlreadyApplied, appliedAction)
        assertFalse(appliedAction.enabled)

        // 6. Ready to apply (selected differs from actual)
        val readyAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.READY,
                selectedMode = en,
                actualSystemUiLocale = "th-TH-u-ca-buddhist",
                isApplying = false,
                hasError = false,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.Apply, readyAction)
        assertTrue(readyAction.enabled)

        // 7. Retry on error
        val retryAction =
            com.thaiclockfix.model.PrimaryAction.resolve(
                shizukuStatus = com.thaiclockfix.shizuku.ShizukuStatus.READY,
                selectedMode = en,
                actualSystemUiLocale = null,
                isApplying = false,
                hasError = true,
            )
        assertEquals(com.thaiclockfix.model.PrimaryAction.Retry, retryAction)
    }
}
