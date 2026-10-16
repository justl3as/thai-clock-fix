package com.thaiclockfix.shizuku

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import rikka.shizuku.Shizuku

/**
 * Status-mapping and fallback-launch behavior of ShizukuManager on a device
 * without the Shizuku binder (Robolectric): the exact environment a fresh
 * install of the app runs in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShizukuManagerTest {

    private lateinit var app: android.app.Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun withoutShizukuPackageStatusIsNotInstalled() {
        val manager = ShizukuManager(app)
        manager.refreshStatus()
        assertEquals(ShizukuStatus.NOT_INSTALLED, manager.status.value)
    }

    @Test
    fun withPackageInstalledButNoBinderStatusIsServiceStopped() {
        val info = PackageInfo().apply { packageName = ShizukuManager.SHIZUKU_PACKAGE }
        shadowOf(app.packageManager).installPackage(info)

        val manager = ShizukuManager(app)
        manager.refreshStatus()

        assertEquals(ShizukuStatus.SERVICE_STOPPED, manager.status.value)
    }

    @Test
    fun initAndDestroySurviveWithoutShizukuBinder() {
        val manager = ShizukuManager(app)
        manager.init()
        manager.destroy()
        // init() refreshes as its last step; nothing crashed along the way.
        assertEquals(ShizukuStatus.NOT_INSTALLED, manager.status.value)
    }

    @Test
    fun requestPermissionWithoutBinderDoesNotCrash() {
        val manager = ShizukuManager(app)
        manager.requestPermission()
        assertEquals(ShizukuStatus.NOT_INSTALLED, manager.status.value)
    }

    @Test
    fun installedButBinderCallbackNotReceivedStaysStoppedAndNeverRequestsPermission() {
        installShizukuPackage()
        val api = FakeShizukuApi(pingBinder = true)
        val manager = ShizukuManager(app, api)

        manager.init()
        val requested = manager.requestPermission()

        assertFalse(requested)
        assertEquals(ShizukuStatus.SERVICE_STOPPED, manager.status.value)
        assertEquals(0, api.permissionRequestCount)
    }

    @Test
    fun binderReceivedWithMissingPermissionAllowsPermissionRequest() {
        installShizukuPackage()
        val api = FakeShizukuApi(pingBinder = true)
        val manager = ShizukuManager(app, api)

        manager.init()
        api.dispatchBinderReceived()
        val requested = manager.requestPermission()

        assertTrue(requested)
        assertEquals(ShizukuStatus.PERMISSION_REQUIRED, manager.status.value)
        assertEquals(1, api.permissionRequestCount)
    }

    @Test
    fun binderDeathImmediatelyDowngradesToStoppedAndBlocksStalePermissionClick() {
        installShizukuPackage()
        val api = FakeShizukuApi(pingBinder = true)
        val manager = ShizukuManager(app, api)

        manager.init()
        api.dispatchBinderReceived()
        assertEquals(ShizukuStatus.PERMISSION_REQUIRED, manager.status.value)

        api.dispatchBinderDead()
        val requested = manager.requestPermission()

        assertFalse(requested)
        assertEquals(ShizukuStatus.SERVICE_STOPPED, manager.status.value)
        assertEquals(0, api.permissionRequestCount)
    }

    @Test
    fun serviceStoppingBetweenStatusAndClickBlocksPermissionRequest() {
        installShizukuPackage()
        val api = FakeShizukuApi(pingBinder = true)
        val manager = ShizukuManager(app, api)

        manager.init()
        api.dispatchBinderReceived()
        assertEquals(ShizukuStatus.PERMISSION_REQUIRED, manager.status.value)

        api.pingBinder = false
        val requested = manager.requestPermission()

        assertFalse(requested)
        assertEquals(ShizukuStatus.SERVICE_STOPPED, manager.status.value)
        assertEquals(0, api.permissionRequestCount)
    }

    @Test
    fun permissionIpcFailureDowngradesToStoppedAndCannotRepeatAgainstStaleBinder() {
        installShizukuPackage()
        val api = FakeShizukuApi(pingBinder = true, failPermissionRequest = true)
        val manager = ShizukuManager(app, api)

        manager.init()
        api.dispatchBinderReceived()

        assertFalse(manager.requestPermission())
        assertEquals(ShizukuStatus.SERVICE_STOPPED, manager.status.value)
        assertEquals(1, api.permissionRequestCount)

        assertFalse(manager.requestPermission())
        assertEquals(1, api.permissionRequestCount)
    }

    @Test
    fun openShizukuAppWhenMissingFallsBackToPlayStoreIntent() {
        val manager = ShizukuManager(app)
        val launched = manager.openShizukuApp()

        assertFalse(launched)
        val next = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, next.action)
        assertEquals("market://details?id=" + ShizukuManager.SHIZUKU_PACKAGE, next.data.toString())
    }

    private fun installShizukuPackage() {
        val info = PackageInfo().apply { packageName = ShizukuManager.SHIZUKU_PACKAGE }
        shadowOf(app.packageManager).installPackage(info)
    }

    private class FakeShizukuApi(
        var pingBinder: Boolean,
        var preV11: Boolean = false,
        var permission: Int = PackageManager.PERMISSION_DENIED,
        var failPermissionRequest: Boolean = false,
    ) : ShizukuApi {
        private var binderReceivedListener: Shizuku.OnBinderReceivedListener? = null
        private var binderDeadListener: Shizuku.OnBinderDeadListener? = null
        private var permissionResultListener: Shizuku.OnRequestPermissionResultListener? = null

        var permissionRequestCount = 0
            private set

        override fun addBinderReceivedListenerSticky(listener: Shizuku.OnBinderReceivedListener) {
            binderReceivedListener = listener
        }

        override fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
            if (binderReceivedListener === listener) binderReceivedListener = null
        }

        override fun addBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
            binderDeadListener = listener
        }

        override fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
            if (binderDeadListener === listener) binderDeadListener = null
        }

        override fun addRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
            permissionResultListener = listener
        }

        override fun removeRequestPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
            if (permissionResultListener === listener) permissionResultListener = null
        }

        override fun pingBinder(): Boolean = pingBinder

        override fun isPreV11(): Boolean = preV11

        override fun checkSelfPermission(): Int = permission

        override fun requestPermission(requestCode: Int) {
            permissionRequestCount += 1
            if (failPermissionRequest) {
                throw IllegalStateException("binder not ready")
            }
        }

        fun dispatchBinderReceived() {
            binderReceivedListener?.onBinderReceived()
        }

        fun dispatchBinderDead() {
            binderDeadListener?.onBinderDead()
        }
    }
}
