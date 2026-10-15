package com.example.data

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.model.LocaleMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesRepositoryTest {

    private lateinit var app: Application
    private lateinit var repository: PreferencesRepository

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        app.clockDataStore.edit { it.clear() }
        repository = PreferencesRepository(app)
        Unit
    }

    @Test
    fun emptyStoreYieldsDefaults() = runBlocking {
        val prefs = repository.preferencesFlow.first()
        assertEquals(LocaleMode.DEFAULT.tag, prefs.selectedLocaleTag)
        assertEquals(LocaleMode.DEFAULT, prefs.selectedMode)
        assertFalse(prefs.lastApplySuccess)
        assertNull(prefs.lastAppliedLocaleTag)
        assertNull(prefs.lastErrorMessage)
        assertEquals(0L, prefs.lastApplyTimestamp)
        assertNull(prefs.lastVerifiedSystemLocale)
    }

    @Test
    fun saveSelectedLocaleRoundTrips() = runBlocking {
        repository.saveSelectedLocale(LocaleMode.ENGLISH.tag)
        val prefs = repository.preferencesFlow.first()
        assertEquals("en-US", prefs.selectedLocaleTag)
        assertEquals(LocaleMode.ENGLISH, prefs.selectedMode)
    }

    @Test
    fun failureThenSuccessClearsErrorMessage() = runBlocking {
        repository.saveApplyFailure("en-US", "boom")
        val failed = repository.preferencesFlow.first()
        assertFalse(failed.lastApplySuccess)
        assertEquals("boom", failed.lastErrorMessage)

        repository.saveApplySuccess("en-US", "en-US")
        val saved = repository.preferencesFlow.first()
        assertTrue(saved.lastApplySuccess)
        assertNull(saved.lastErrorMessage)
        assertEquals("en-US", saved.lastAppliedLocaleTag)
    }

    @Test
    fun restoreDefaultMarksAppliedEmptyAndClearsError() = runBlocking {
        repository.saveApplyFailure("en-US", "boom")
        repository.saveRestoreDefault("system_default")
        val prefs = repository.preferencesFlow.first()
        assertTrue(prefs.lastApplySuccess)
        assertEquals("", prefs.lastAppliedLocaleTag)
        assertNull(prefs.lastAppliedMode)
        assertNull(prefs.lastErrorMessage)
    }

    @Test
    fun verifiedSystemLocaleUpdateHandlesNull() = runBlocking {
        repository.updateVerifiedSystemLocale("th-TH-u-ca-buddhist")
        assertEquals(
            "th-TH-u-ca-buddhist",
            repository.preferencesFlow.first().lastVerifiedSystemLocale,
        )
        repository.updateVerifiedSystemLocale(null)
        assertNull(repository.preferencesFlow.first().lastVerifiedSystemLocale)
    }
}
