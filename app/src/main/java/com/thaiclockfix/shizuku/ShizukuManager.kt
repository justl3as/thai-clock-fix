package com.thaiclockfix.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    READY, // Service running and permission granted
    PERMISSION_REQUIRED, // Service running but permission not yet granted
    SERVICE_STOPPED, // Shizuku installed, but service is not running
    NOT_INSTALLED, // Shizuku app is not installed
    DENIED, // Permission was denied
    UNKNOWN,
}

class ShizukuManager(
    private val context: Context,
) {
    private var shizukuApi: ShizukuApi = AndroidShizukuApi

    internal constructor(
        context: Context,
        shizukuApi: ShizukuApi,
    ) : this(context) {
        this.shizukuApi = shizukuApi
    }
    companion object {
        const val TAG = "ShizukuManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU = 1001
    }

    private val _status = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    @Volatile
    private var binderReady = false

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            Log.d(TAG, "Shizuku binder received")
            binderReady = true
            refreshStatus()
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            Log.d(TAG, "Shizuku binder died")
            binderReady = false
            updateDisconnectedStatus()
        }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Shizuku permission granted")
                    refreshStatus()
                } else {
                    Log.d(TAG, "Shizuku permission denied")
                    _status.value = ShizukuStatus.DENIED
                }
            }
        }

    fun init() {
        try {
            shizukuApi.addBinderReceivedListenerSticky(binderReceivedListener)
            shizukuApi.addBinderDeadListener(binderDeadListener)
            shizukuApi.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (t: Throwable) {
            binderReady = false
            Log.e(TAG, "Error registering Shizuku listeners", t)
        }
        refreshStatus()
    }

    fun destroy() {
        binderReady = false
        try {
            shizukuApi.removeBinderReceivedListener(binderReceivedListener)
            shizukuApi.removeBinderDeadListener(binderDeadListener)
            shizukuApi.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (t: Throwable) {
            Log.e(TAG, "Error unregistering Shizuku listeners", t)
        }
    }

    fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (t: Throwable) {
        false
    }

    fun refreshStatus() {
        if (!isInstalled()) {
            binderReady = false
            _status.value = ShizukuStatus.NOT_INSTALLED
            return
        }

        // Shizuku APIs are only safe after OnBinderReceived has completed the
        // application attach handshake. A live binder alone is not sufficient.
        if (!binderReady) {
            _status.value = ShizukuStatus.SERVICE_STOPPED
            return
        }

        val pingOk =
            try {
                shizukuApi.pingBinder()
            } catch (t: Throwable) {
                false
            }

        if (!pingOk) {
            binderReady = false
            _status.value = ShizukuStatus.SERVICE_STOPPED
            return
        }

        try {
            // Legacy servers do not expose the permission contract required by this app.
            if (shizukuApi.isPreV11()) {
                _status.value = ShizukuStatus.UNKNOWN
                return
            }

            _status.value =
                if (shizukuApi.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    ShizukuStatus.READY
                } else {
                    ShizukuStatus.PERMISSION_REQUIRED
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to check Shizuku permission", t)
            _status.value = ShizukuStatus.UNKNOWN
        }
    }

    fun requestPermission(): Boolean {
        // Re-evaluate immediately before requesting so a stale UI state cannot
        // issue a permission call after the Shizuku service has stopped.
        refreshStatus()
        if (_status.value != ShizukuStatus.PERMISSION_REQUIRED || !binderReady) {
            return false
        }

        return try {
            if (!shizukuApi.pingBinder()) {
                binderReady = false
                _status.value = ShizukuStatus.SERVICE_STOPPED
                false
            } else if (shizukuApi.isPreV11()) {
                _status.value = ShizukuStatus.UNKNOWN
                false
            } else if (shizukuApi.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _status.value = ShizukuStatus.READY
                false
            } else {
                shizukuApi.requestPermission(REQUEST_CODE_SHIZUKU)
                true
            }
        } catch (t: Throwable) {
            // Treat a failed permission IPC as a disconnected service. This
            // prevents repeated calls against a stale/dead binder.
            binderReady = false
            updateDisconnectedStatus()
            Log.e(TAG, "Failed to request Shizuku permission", t)
            false
        }
    }

    private fun updateDisconnectedStatus() {
        _status.value =
            if (isInstalled()) {
                ShizukuStatus.SERVICE_STOPPED
            } else {
                ShizukuStatus.NOT_INSTALLED
            }
    }

    fun openShizukuApp(): Boolean = try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } else {
            // If not found, open Play Store or web link
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$SHIZUKU_PACKAGE".toUri())
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(marketIntent)
            } catch (anfe: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, "https://shizuku.rikka.app".toUri())
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
            false
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open Shizuku app", e)
        false
    }
}
