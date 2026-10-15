package com.example.shizuku

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
    companion object {
        const val TAG = "ShizukuManager"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU = 1001
    }

    private val _status = MutableStateFlow(ShizukuStatus.UNKNOWN)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            Log.d(TAG, "Shizuku binder received")
            refreshStatus()
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            Log.d(TAG, "Shizuku binder died")
            refreshStatus()
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
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        } catch (t: Throwable) {
            Log.e(TAG, "Error registering Shizuku listeners", t)
        }
        refreshStatus()
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
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
            _status.value = ShizukuStatus.NOT_INSTALLED
            return
        }

        val pingOk =
            try {
                Shizuku.pingBinder()
            } catch (t: Throwable) {
                false
            }

        if (!pingOk) {
            _status.value = ShizukuStatus.SERVICE_STOPPED
            return
        }

        try {
            // Legacy servers do not expose the permission contract required by this app.
            if (Shizuku.isPreV11()) {
                _status.value = ShizukuStatus.UNKNOWN
                return
            }
            val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

            _status.value =
                if (hasPermission) {
                    ShizukuStatus.READY
                } else {
                    ShizukuStatus.PERMISSION_REQUIRED
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to check Shizuku permission", t)
            _status.value = ShizukuStatus.UNKNOWN
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                } else {
                    refreshStatus()
                }
            } else {
                refreshStatus()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to request Shizuku permission", t)
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
