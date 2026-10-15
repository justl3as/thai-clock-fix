package com.example.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.PreferencesRepository
import com.example.model.LocaleMode
import com.example.service.ShizukuSystemUiLocaleService
import com.example.service.SystemUiLocaleService
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ClockFixUiState(
    val selectedMode: LocaleMode = LocaleMode.DEFAULT,
    val shizukuStatus: ShizukuStatus = ShizukuStatus.UNKNOWN,
    val actualSystemUiLocale: String? = null,
    val actualIs24Hour: Boolean? = null,
    val isApplying: Boolean = false,
    val isCheckingSystem: Boolean = false,
    val userNotice: String? = null,
    val lastError: String? = null,
    val showRestoreConfirmDialog: Boolean = false,
    val showAdbCommandsDialog: Boolean = false,
    val showFaqDialog: Boolean = false,
    val showShizukuSheet: Boolean = false,
)

class ClockFixViewModel(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    val shizukuManager: ShizukuManager,
    private val localeService: SystemUiLocaleService,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        preferencesRepository = PreferencesRepository(application),
        shizukuManager = ShizukuManager(application),
        localeService = ShizukuSystemUiLocaleService(),
    )

    private val _uiState = MutableStateFlow(ClockFixUiState())
    val uiState: StateFlow<ClockFixUiState> = _uiState.asStateFlow()

    private val operationMutex = Mutex()

    init {
        // 1. Initialize Shizuku manager
        shizukuManager.init()

        // 2. Collect DataStore preferences.
        // A persisted error stays in storage for diagnostics only; copying it into the UI state
        // here would replay a stale error snackbar on every app launch.
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(selectedMode = prefs.selectedMode) }
            }
        }

        // 3. Collect Shizuku status updates
        viewModelScope.launch {
            shizukuManager.status.collectLatest { status ->
                _uiState.update { it.copy(shizukuStatus = status) }
                if (status == ShizukuStatus.READY) {
                    checkActualSystemUiLocale()
                } else {
                    _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null) }
                }
            }
        }
    }

    fun selectMode(mode: LocaleMode) {
        if (_uiState.value.selectedMode == mode) return

        _uiState.update { it.copy(selectedMode = mode, userNotice = null) }
        viewModelScope.launch {
            preferencesRepository.saveSelectedLocale(mode.tag)
        }
    }

    fun applySelectedLocale() {
        val mode = _uiState.value.selectedMode
        viewModelScope.launch { operationMutex.withLock { performMutation(mode) } }
    }

    fun restoreDefaultLocale() {
        _uiState.update { it.copy(showRestoreConfirmDialog = false) }
        viewModelScope.launch { operationMutex.withLock { performMutation(null) } }
    }

    // Mutation and readback share one queue so an old query cannot overwrite a newer result.
    // Check current readiness after acquiring the lock; each real shell command also checks IPC permission.
    private suspend fun performMutation(mode: LocaleMode?) {
        if (shizukuManager.status.value != ShizukuStatus.READY) {
            _uiState.update { it.copy(userNotice = stringRes(R.string.vm_shizuku_not_ready)) }
            return
        }
        _uiState.update { it.copy(isApplying = true, userNotice = null, lastError = null) }
        try {
            val result = if (mode == null) localeService.restoreDefault() else localeService.applyLocale(mode)
            val applied = result.getOrThrow()
            val expected = mode?.tag ?: "system_default"
            check(applied.isSuccess && applied.isVerified && applied.verifiedLocale.equals(expected, ignoreCase = true)) {
                applied.errorMessage ?: stringRes(R.string.vm_verify_failed)
            }
            if (shizukuManager.status.value == ShizukuStatus.READY) {
                if (mode == null) {
                    preferencesRepository.saveRestoreDefault(applied.verifiedLocale)
                } else {
                    preferencesRepository.saveApplySuccess(mode.tag, applied.verifiedLocale)
                }
                _uiState.update {
                    it.copy(
                        actualSystemUiLocale = applied.verifiedLocale,
                        actualIs24Hour = if (mode == null) null else true,
                        userNotice =
                        if (mode == null) {
                            stringRes(R.string.vm_restore_success)
                        } else {
                            stringRes(R.string.vm_apply_success)
                        },
                        lastError = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        actualSystemUiLocale = null,
                        actualIs24Hour = null,
                        userNotice = null,
                    )
                }
            }
        } catch (cancelled: CancellationException) {
            // A partially executed operation has an unknown outcome until the next readback.
            _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null, userNotice = null) }
            throw cancelled
        } catch (exception: Exception) {
            val message = exception.localizedMessage ?: stringRes(R.string.vm_error_generic)
            android.util.Log.e("ClockFixViewModel", "performMutation failed: $message", exception)
            _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null, lastError = message, userNotice = null) }
            preferencesRepository.saveApplyFailure(mode?.tag.orEmpty(), message)
        } finally {
            if (shizukuManager.status.value != ShizukuStatus.READY) {
                _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null) }
            }
            _uiState.update { it.copy(isApplying = false) }
        }
    }

    fun checkActualSystemUiLocale() {
        viewModelScope.launch {
            operationMutex.withLock {
                if (shizukuManager.status.value != ShizukuStatus.READY) return@withLock
                _uiState.update { it.copy(isCheckingSystem = true) }
                try {
                    val settings = localeService.getCurrentSystemUiSettings().getOrThrow()
                    android.util.Log.d("ClockFixViewModel", "Read settings: locale=${settings.locale}, is24h=${settings.is24Hour}")
                    if (shizukuManager.status.value == ShizukuStatus.READY) {
                        preferencesRepository.updateVerifiedSystemLocale(settings.locale)
                        _uiState.update {
                            it.copy(
                                actualSystemUiLocale = settings.locale,
                                actualIs24Hour = settings.is24Hour,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                actualSystemUiLocale = null,
                                actualIs24Hour = null,
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    android.util.Log.e("ClockFixViewModel", "checkActualSystemUiLocale failed", e)
                    _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null) }
                } finally {
                    if (shizukuManager.status.value != ShizukuStatus.READY) {
                        _uiState.update { it.copy(actualSystemUiLocale = null, actualIs24Hour = null) }
                    }
                    _uiState.update { it.copy(isCheckingSystem = false) }
                }
            }
        }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun refreshShizukuStatus() {
        shizukuManager.refreshStatus()
        if (shizukuManager.status.value == ShizukuStatus.READY) {
            checkActualSystemUiLocale()
        }
    }

    fun openShizukuApp() {
        shizukuManager.openShizukuApp()
    }

    fun showRestoreDialog(show: Boolean) {
        _uiState.update { it.copy(showRestoreConfirmDialog = show) }
    }

    fun showAdbDialog(show: Boolean) {
        _uiState.update { it.copy(showAdbCommandsDialog = show) }
    }

    fun showFaqDialog(show: Boolean) {
        _uiState.update { it.copy(showFaqDialog = show) }
    }

    fun showShizukuSheet(show: Boolean) {
        _uiState.update { it.copy(showShizukuSheet = show) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(lastError = null) }
    }

    fun getAdbCommands(): List<String> = localeService.getAdbCommands(_uiState.value.selectedMode)

    fun getRestoreAdbCommands(): List<String> = localeService.getAdbCommands(null)

    private fun stringRes(
        @StringRes id: Int,
    ): String = getApplication<Application>().getString(id)

    override fun onCleared() {
        super.onCleared()
        shizukuManager.destroy()
    }
}
