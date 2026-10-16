package com.thaiclockfix

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManifestPrivacyTest {

    @Test
    fun appDoesNotRequestInternetPermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val packageInfo =
            app.packageManager.getPackageInfo(
                app.packageName,
                PackageManager.GET_PERMISSIONS,
            )
        val requestedPermissions = packageInfo.requestedPermissions?.toSet().orEmpty()

        assertFalse(Manifest.permission.INTERNET in requestedPermissions)
    }
}
