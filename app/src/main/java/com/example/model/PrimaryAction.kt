package com.example.model

import androidx.annotation.StringRes
import com.example.R
import com.example.shizuku.ShizukuStatus

/**
 * The single, state-aware primary action shown in the bottom bar.
 * The button label itself tells the user what the next step is.
 */
sealed class PrimaryAction(
    @StringRes val labelRes: Int,
    val enabled: Boolean,
) {
    data object InstallShizuku : PrimaryAction(R.string.action_install_shizuku, true)

    data object StartShizuku : PrimaryAction(R.string.action_start_shizuku, true)

    data object GrantPermission : PrimaryAction(R.string.action_grant_permission, true)

    data object Apply : PrimaryAction(R.string.action_apply, true)

    data object Checking : PrimaryAction(R.string.action_checking, false)

    data object Applying : PrimaryAction(R.string.action_applying, false)

    data object AlreadyApplied : PrimaryAction(R.string.action_already_applied, false)

    data object Retry : PrimaryAction(R.string.action_retry, true)

    companion object {
        fun resolve(
            shizukuStatus: ShizukuStatus,
            selectedMode: LocaleMode,
            actualSystemUiLocale: String?,
            isApplying: Boolean,
            hasError: Boolean,
            is24Hour: Boolean? = null,
            isChecking: Boolean = false,
        ): PrimaryAction = when {
            isApplying -> Applying

            isChecking -> Checking

            shizukuStatus == ShizukuStatus.NOT_INSTALLED -> InstallShizuku

            shizukuStatus == ShizukuStatus.SERVICE_STOPPED ||
                shizukuStatus == ShizukuStatus.UNKNOWN -> StartShizuku

            shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED ||
                shizukuStatus == ShizukuStatus.DENIED -> GrantPermission

            actualSystemUiLocale?.equals(selectedMode.tag, ignoreCase = true) == true && is24Hour == true -> AlreadyApplied

            hasError -> Retry

            else -> Apply
        }
    }
}
