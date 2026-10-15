package com.thaiclockfix.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.thaiclockfix.data.ClockFixPreferences
import com.thaiclockfix.data.ClockPreferencesStore
import com.thaiclockfix.model.LocaleMode
import com.thaiclockfix.service.ApplyResult
import com.thaiclockfix.service.SystemUiLocaleService
import com.thaiclockfix.service.SystemUiSettings
import com.thaiclockfix.shizuku.ShizukuManager
import com.thaiclockfix.shizuku.ShizukuStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClockFixViewModelTest {
    private lateinit var app: Application
    private val store = ViewModelStore()
    private val main = UnconfinedTestDispatcher()
    private lateinit var manager: ShizukuManager
    private lateinit var preferences: FixturePreferencesStore

    @Before fun setUp() {
        Dispatchers.setMain(main)
        app = ApplicationProvider.getApplicationContext()
        manager = ShizukuManager(app)
        preferences = FixturePreferencesStore()
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Suppress("UNCHECKED_CAST")
    private fun status(value: ShizukuStatus) {
        val field = ShizukuManager::class.java.getDeclaredField("_status").apply { isAccessible = true }
        (field.get(manager) as MutableStateFlow<ShizukuStatus>).value = value
    }

    private class FixtureService : SystemUiLocaleService {
        var apply: suspend (LocaleMode) -> Result<ApplyResult> = { mode -> Result.success(ApplyResult(true, mode.tag, mode.tag, true, "")) }
        var restore: suspend () -> Result<ApplyResult> = { Result.success(ApplyResult(true, "", "system_default", true, "")) }
        var is24Hour: Boolean? = true
        var query: suspend () -> Result<String> = { Result.success("en-US") }
        override suspend fun applyLocale(mode: LocaleMode) = apply(mode)
        override suspend fun restoreDefault() = restore()
        override suspend fun getCurrentSystemUiLocale() = query()
        override suspend fun getCurrentSystemUiSettings() = query().map { SystemUiSettings(it, is24Hour) }
        override fun getAdbCommands(mode: LocaleMode?) = emptyList<String>()
    }

    private class FixturePreferencesStore(
        initial: ClockFixPreferences = ClockFixPreferences(),
    ) : ClockPreferencesStore {
        private val state = MutableStateFlow(initial)
        override val preferencesFlow = state

        val current: ClockFixPreferences
            get() = state.value

        override suspend fun saveSelectedLocale(localeTag: String) {
            state.value = state.value.copy(selectedLocaleTag = localeTag)
        }

        override suspend fun saveApplySuccess(
            appliedLocaleTag: String,
            verifiedLocale: String,
        ) {
            state.value =
                state.value.copy(
                    lastAppliedLocaleTag = appliedLocaleTag,
                    lastApplySuccess = true,
                    lastApplyTimestamp = System.currentTimeMillis(),
                    lastVerifiedSystemLocale = verifiedLocale,
                    lastErrorMessage = null,
                )
        }

        override suspend fun saveApplyFailure(
            attemptedLocaleTag: String,
            errorMessage: String,
        ) {
            state.value =
                state.value.copy(
                    lastApplySuccess = false,
                    lastApplyTimestamp = System.currentTimeMillis(),
                    lastErrorMessage = errorMessage,
                )
        }

        override suspend fun saveRestoreDefault(verifiedLocale: String) {
            state.value =
                state.value.copy(
                    lastAppliedLocaleTag = "",
                    lastApplySuccess = true,
                    lastApplyTimestamp = System.currentTimeMillis(),
                    lastVerifiedSystemLocale = verifiedLocale,
                    lastErrorMessage = null,
                )
        }

        override suspend fun updateVerifiedSystemLocale(verifiedLocale: String?) {
            state.value = state.value.copy(lastVerifiedSystemLocale = verifiedLocale)
        }
    }

    private fun viewModel(service: FixtureService): ClockFixViewModel {
        val vm = ClockFixViewModel(app, preferences, manager, service)
        store.put("test", vm)
        status(ShizukuStatus.READY)
        return vm
    }

    @Test fun failedInnerRestoreCannotRecordOrDisplaySuccess() = runBlocking {
        val service = FixtureService().apply {
            restore = { Result.success(ApplyResult(false, "", "en-US", false, "logs", "reset denied")) }
        }
        val vm = viewModel(service)
        vm.restoreDefaultLocale()
        val state = withTimeout(3_000) { vm.uiState.first { !it.isApplying && (it.lastError != null || it.userNotice != null) } }
        assertEquals("reset denied", state.lastError)
        assertNull(state.userNotice)
        assertNotEquals("system_default", state.actualSystemUiLocale)
        assertFalse(preferences.current.lastApplySuccess)
        assertEquals("reset denied", preferences.current.lastErrorMessage)
    }

    @Test fun applyAndRestoreCannotOverlap() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val restored = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val service = FixtureService().apply {
            apply = { mode ->
                events += "apply-start"
                entered.complete(Unit)
                release.await()
                events += "apply-end"
                Result.success(ApplyResult(true, mode.tag, mode.tag, true, ""))
            }
            restore = {
                events += "restore"
                restored.complete(Unit)
                Result.success(ApplyResult(true, "", "system_default", true, ""))
            }
        }
        val vm = viewModel(service)
        vm.applySelectedLocale()
        entered.await()
        vm.restoreDefaultLocale()
        assertFalse("restore must wait for apply", restored.isCompleted)
        release.complete(Unit)
        withTimeout(3_000) { vm.uiState.first { !it.isApplying && it.actualSystemUiLocale == "system_default" } }
        assertEquals(listOf("apply-start", "apply-end", "restore"), events)
    }

    @Test fun queuedRestoreRechecksRevokedPermission() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var restoreCalls = 0
        val service = FixtureService().apply {
            apply = { mode ->
                entered.complete(Unit)
                release.await()
                Result.success(ApplyResult(true, mode.tag, mode.tag, true, ""))
            }
            restore = {
                restoreCalls++
                Result.success(ApplyResult(true, "", "system_default", true, ""))
            }
        }
        val vm = viewModel(service)
        vm.applySelectedLocale()
        entered.await()
        vm.restoreDefaultLocale()
        status(ShizukuStatus.DENIED)
        release.complete(Unit)
        withTimeout(3_000) { vm.uiState.first { !it.isApplying && it.userNotice?.contains("Shizuku") == true } }
        assertEquals(0, restoreCalls)
    }

    @Test fun readbackCannotRunAcrossMutationAndOverwriteItsResult() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val service = FixtureService()
        val vm = viewModel(service)
        service.query = {
            entered.complete(Unit)
            release.await()
            Result.success("en-US")
        }
        vm.checkActualSystemUiLocale()
        entered.await()
        val applied = CompletableDeferred<Unit>()
        service.apply = { mode ->
            applied.complete(Unit)
            Result.success(ApplyResult(true, mode.tag, mode.tag, true, ""))
        }
        vm.applySelectedLocale()
        assertFalse("mutation must wait for the readback", applied.isCompleted)
        release.complete(Unit)
        withTimeout(3_000) { vm.uiState.first { !it.isApplying && it.actualSystemUiLocale == "th-TH-u-ca-buddhist" } }
        Unit
    }

    @Test fun refreshReadsActual24HourSettingInsteadOfOnlyLocale() = runBlocking {
        val service = FixtureService()
        val vm = viewModel(service)
        vm.selectMode(LocaleMode.ENGLISH)
        vm.checkActualSystemUiLocale()
        withTimeout(3_000) { vm.uiState.first { !it.isCheckingSystem && it.actualSystemUiLocale == "en-US" } }
        assertEquals(true, vm.uiState.value.actualIs24Hour)
        service.is24Hour = false
        vm.checkActualSystemUiLocale()
        withTimeout(3_000) { vm.uiState.first { !it.isCheckingSystem && it.actualIs24Hour == false } }
        Unit
    }

    @Test fun permissionLossDuringReadbackCannotRestoreVerifiedState() = runBlocking {
        val service = FixtureService()
        val vm = viewModel(service)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        service.query = {
            entered.complete(Unit)
            release.await()
            Result.success("en-US")
        }
        vm.checkActualSystemUiLocale()
        entered.await()
        status(ShizukuStatus.DENIED)
        release.complete(Unit)
        assertNull(vm.uiState.value.actualSystemUiLocale)
        assertNull(vm.uiState.value.actualIs24Hour)
    }

    @Test fun permissionLossDuringApplyCannotRepopulateVerifiedState() = runBlocking {
        val service = FixtureService()
        val vm = viewModel(service)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        service.apply = { mode ->
            entered.complete(Unit)
            release.await()
            Result.success(ApplyResult(true, mode.tag, mode.tag, true, ""))
        }
        vm.applySelectedLocale()
        entered.await()
        status(ShizukuStatus.DENIED)
        release.complete(Unit)
        withTimeout(3_000) { vm.uiState.first { !it.isApplying } }
        assertNull(vm.uiState.value.actualSystemUiLocale)
        assertNull(vm.uiState.value.actualIs24Hour)
    }

    @Test fun cancellationClearsBusyStateWithoutSuccessNotice() = runBlocking {
        val service = FixtureService().apply { restore = { throw CancellationException("cancel") } }
        val vm = viewModel(service)
        vm.restoreDefaultLocale()
        assertFalse(vm.uiState.value.isApplying)
        assertNull(vm.uiState.value.userNotice)
    }

    @Test fun selectModeUpdatesStateAndPersistsSelection() = runBlocking {
        val vm = viewModel(FixtureService())
        vm.selectMode(LocaleMode.ENGLISH)
        assertEquals(LocaleMode.ENGLISH, vm.uiState.value.selectedMode)
        assertEquals(LocaleMode.ENGLISH, preferences.current.selectedMode)
        assertEquals(LocaleMode.ENGLISH, vm.uiState.value.selectedMode)
    }

    @Test fun persistedPreferenceCannotOverwriteSessionSelection() = runBlocking {
        val vm = viewModel(FixtureService())

        vm.selectMode(LocaleMode.ENGLISH)
        assertEquals(LocaleMode.ENGLISH, preferences.current.selectedMode)

        preferences.saveSelectedLocale(LocaleMode.THAI_BUDDHIST.tag)
        assertEquals(LocaleMode.THAI_BUDDHIST, preferences.current.selectedMode)
        assertEquals(LocaleMode.ENGLISH, vm.uiState.value.selectedMode)
    }

    @Test fun dialogFlagsRoundTripThroughUiState() {
        val vm = viewModel(FixtureService())
        vm.showRestoreDialog(true)
        vm.showAdbDialog(true)
        vm.showFaqDialog(true)
        vm.showShizukuSheet(true)
        val shown = vm.uiState.value
        assertTrue(shown.showRestoreConfirmDialog)
        assertTrue(shown.showAdbCommandsDialog)
        assertTrue(shown.showFaqDialog)
        assertTrue(shown.showShizukuSheet)
        vm.showRestoreDialog(false)
        vm.showAdbDialog(false)
        vm.showFaqDialog(false)
        vm.showShizukuSheet(false)
        val hidden = vm.uiState.value
        assertFalse(hidden.showRestoreConfirmDialog)
        assertFalse(hidden.showAdbCommandsDialog)
        assertFalse(hidden.showFaqDialog)
        assertFalse(hidden.showShizukuSheet)
    }

    @Test fun successfulApplyRaisesNoticeThatDismissClears() = runBlocking {
        val vm = viewModel(FixtureService())
        vm.applySelectedLocale()
        withTimeout(3_000) { vm.uiState.first { it.userNotice != null } }
        vm.dismissNotice()
        assertNull(vm.uiState.value.userNotice)
    }

    @Test fun defaultConstructorWiringCreatesUsableViewModel() {
        val vm = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
            .getInstance(app)
            .create(ClockFixViewModel::class.java)
        store.put("default", vm)
        assertNotNull(vm.uiState.value)
    }
}
