package com.example.service

import com.example.model.LocaleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class ShellResult(
    val isSuccess: Boolean,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

data class ApplyResult(
    val isSuccess: Boolean,
    val appliedLocale: String,
    val verifiedLocale: String,
    val isVerified: Boolean,
    val logOutput: String,
    val errorMessage: String? = null,
)

data class SystemUiSettings(
    val locale: String,
    val is24Hour: Boolean?,
)

interface SystemUiLocaleService {
    suspend fun applyLocale(mode: LocaleMode): Result<ApplyResult>

    suspend fun restoreDefault(): Result<ApplyResult>

    suspend fun getCurrentSystemUiLocale(): Result<String>

    suspend fun getCurrentSystemUiSettings(): Result<SystemUiSettings>

    fun getAdbCommands(mode: LocaleMode?): List<String>
}

class ShizukuSystemUiLocaleService(
    private val shell: (suspend (String) -> ShellResult)? = null,
) : SystemUiLocaleService {
    companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }

    override suspend fun applyLocale(mode: LocaleMode): Result<ApplyResult> = withContext(Dispatchers.IO) {
        captureResult {
            val logs = StringBuilder()

            suspend fun command(value: String): ShellResult = runShell(value).also {
                logs.append("$value\nexitCode=${it.exitCode}\nstdout=${it.stdout}\nstderr=${it.stderr}\n")
            }

            fun failure(message: String) = ApplyResult(false, mode.tag, "", false, logs.toString(), message)

            val time = command("settings --user 0 put system time_12_24 24")
            if (!time.succeeded()) return@captureResult failure("ตั้งเวลา 24 ชั่วโมงไม่สำเร็จ: ${time.stderr}")
            val set = command("cmd locale set-app-locales $SYSTEM_UI_PACKAGE --user 0 --locales ${mode.tag}")
            if (!set.succeeded()) return@captureResult failure("ตั้งภาษา SystemUI ไม่สำเร็จ: ${set.stderr}")
            val verify = command("cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0")
            val locale = if (verify.succeeded()) extractLocaleFromOutput(verify.stdout) else null
            if (locale == null || !locale.equals(mode.tag, ignoreCase = true)) {
                return@captureResult failure("ไม่สามารถยืนยันภาษา SystemUI: ${verify.stderr.ifBlank { verify.stdout }}")
            }
            val timeReadback = command("settings --user 0 get system time_12_24")
            if (!timeReadback.succeeded() || timeReadback.stdout.trim() != "24") {
                return@captureResult failure("ไม่สามารถยืนยันเวลา 24 ชั่วโมง: ${timeReadback.stderr.ifBlank { timeReadback.stdout }}")
            }
            ApplyResult(true, mode.tag, locale, true, logs.toString())
        }
    }

    override suspend fun restoreDefault(): Result<ApplyResult> = withContext(Dispatchers.IO) {
        captureResult {
            val logs = StringBuilder()

            suspend fun command(value: String): ShellResult = runShell(value).also {
                logs.append("$value\nexitCode=${it.exitCode}\nstdout=${it.stdout}\nstderr=${it.stderr}\n")
            }
            val reset = command("cmd locale set-app-locales $SYSTEM_UI_PACKAGE --user 0 --locales \"\"")
            if (!reset.succeeded()) {
                return@captureResult ApplyResult(false, "", "", false, logs.toString(), "คืนค่าภาษาไม่สำเร็จ: ${reset.stderr}")
            }
            val verify = command("cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0")
            val locale = if (verify.succeeded()) extractLocaleFromOutput(verify.stdout) else null
            val cleared = locale != null && locale.isEmpty()
            ApplyResult(
                isSuccess = cleared,
                appliedLocale = "",
                verifiedLocale = if (cleared) "system_default" else locale.orEmpty(),
                isVerified = cleared,
                logOutput = logs.toString(),
                errorMessage = if (cleared) null else "ไม่สามารถยืนยันการคืนค่า: ${verify.stderr.ifBlank { verify.stdout }}",
            )
        }
    }

    override suspend fun getCurrentSystemUiLocale(): Result<String> = withContext(Dispatchers.IO) {
        captureResult {
            val result = runShell("cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0")
            check(result.succeeded()) { "Exit code ${result.exitCode}: ${result.stderr}" }
            val locale =
                extractLocaleFromOutput(result.stdout)
                    ?: error("ไม่สามารถอ่านภาษา SystemUI: ${result.stdout}")
            locale.ifEmpty { "system_default" }
        }
    }

    override suspend fun getCurrentSystemUiSettings(): Result<SystemUiSettings> = withContext(Dispatchers.IO) {
        captureResult {
            val localeResult = runShell("cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0")
            check(localeResult.succeeded()) { "Exit code ${localeResult.exitCode}: ${localeResult.stderr}" }
            val rawLocale =
                extractLocaleFromOutput(localeResult.stdout)
                    ?: error("ไม่สามารถอ่านภาษา SystemUI: ${localeResult.stdout}")
            val locale = rawLocale.ifEmpty { "system_default" }

            val timeResult = runShell("settings --user 0 get system time_12_24")
            check(timeResult.succeeded()) { "Exit code ${timeResult.exitCode}: ${timeResult.stderr}" }
            val timeText = timeResult.stdout.trim()
            check(timeText.isNotEmpty()) { "รูปแบบเวลาว่างเปล่า" }
            val is24Hour =
                when (timeText) {
                    "24" -> true
                    "12" -> false
                    "null" -> null
                    else -> error("ไม่สามารถอ่านรูปแบบเวลา: $timeText")
                }
            SystemUiSettings(locale, is24Hour)
        }
    }

    override fun getAdbCommands(mode: LocaleMode?): List<String> = if (mode != null) {
        listOf(
            "adb shell settings --user 0 put system time_12_24 24",
            "adb shell cmd locale set-app-locales $SYSTEM_UI_PACKAGE --user 0 --locales ${mode.tag}",
            "adb shell cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0",
        )
    } else {
        listOf(
            "adb shell cmd locale set-app-locales $SYSTEM_UI_PACKAGE --user 0 --locales \"\"",
            "adb shell cmd locale get-app-locales $SYSTEM_UI_PACKAGE --user 0",
        )
    }

    private suspend fun runShell(command: String): ShellResult = shell?.invoke(command)
        ?: BoundedProcessRunner(startProcess = { value ->
            check(Shizuku.pingBinder()) { "Shizuku ไม่ได้ทำงาน" }
            check(!Shizuku.isPreV11()) { "กรุณาอัปเดต Shizuku เป็น v11 ขึ้นไป" }
            check(Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                "Shizuku ยังไม่ได้รับสิทธิ์ หรือสิทธิ์ถูกถอนแล้ว"
            }
            val method =
                Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java,
                )
            method.isAccessible = true
            method.invoke(null, arrayOf("sh", "-c", value), null, null) as Process
        }).execute(command)

    private fun ShellResult.succeeded(): Boolean = isSuccess && exitCode == 0

    // AOSP prints this exact resource-qualified prefix. Accept bracket-only output for OEMs.
    // Empty stdout, arbitrary errors with brackets and another package/user are never a default.
    private fun extractLocaleFromOutput(raw: String): String? {
        val text = raw.trim()
        val list =
            Regex("\\[([^\\[\\]\\r\\n]*)]").matchEntire(text)?.groupValues?.get(1)
                ?: Regex("Locales for com\\.android\\.systemui for user 0 are \\[([^\\[\\]\\r\\n]*)]")
                    .matchEntire(text)
                    ?.groupValues
                    ?.get(1)
                ?: return null
        if (list.isBlank()) return ""
        val tags = list.split(',').map { it.trim() }
        if (tags.any { !Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*").matches(it) }) return null
        return tags.joinToString(",")
    }

    private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
