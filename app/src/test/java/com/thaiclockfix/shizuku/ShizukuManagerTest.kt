package com.thaiclockfix.shizuku

import android.content.Intent
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

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
    fun openShizukuAppWhenMissingFallsBackToPlayStoreIntent() {
        val manager = ShizukuManager(app)
        val launched = manager.openShizukuApp()

        assertFalse(launched)
        val next = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, next.action)
        assertEquals("market://details?id=" + ShizukuManager.SHIZUKU_PACKAGE, next.data.toString())
    }
}
