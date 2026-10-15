package com.thaiclockfix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thaiclockfix.model.LocaleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.clockDataStore: DataStore<Preferences> by preferencesDataStore(name = "clock_fix_settings")

data class ClockFixPreferences(
    val selectedLocaleTag: String = LocaleMode.DEFAULT.tag,
    val lastAppliedLocaleTag: String? = null,
    val lastApplySuccess: Boolean = false,
    val lastApplyTimestamp: Long = 0L,
    val lastVerifiedSystemLocale: String? = null,
    val lastErrorMessage: String? = null,
) {
    val selectedMode: LocaleMode
        get() = LocaleMode.fromTag(selectedLocaleTag) ?: LocaleMode.DEFAULT

    val lastAppliedMode: LocaleMode?
        get() = LocaleMode.fromTag(lastAppliedLocaleTag)
}

interface ClockPreferencesStore {
    val preferencesFlow: Flow<ClockFixPreferences>

    suspend fun saveSelectedLocale(localeTag: String)

    suspend fun saveApplySuccess(
        appliedLocaleTag: String,
        verifiedLocale: String,
    )

    suspend fun saveApplyFailure(
        attemptedLocaleTag: String,
        errorMessage: String,
    )

    suspend fun saveRestoreDefault(verifiedLocale: String)

    suspend fun updateVerifiedSystemLocale(verifiedLocale: String?)
}

class PreferencesRepository(
    private val context: Context,
) : ClockPreferencesStore {
    private object PreferencesKeys {
        val SELECTED_LOCALE = stringPreferencesKey("selected_locale")
        val LAST_APPLIED_LOCALE = stringPreferencesKey("last_applied_locale")
        val LAST_APPLY_SUCCESS = booleanPreferencesKey("last_apply_success")
        val LAST_APPLY_TIMESTAMP = longPreferencesKey("last_apply_timestamp")
        val LAST_VERIFIED_SYSTEM_LOCALE = stringPreferencesKey("last_verified_system_locale")
        val LAST_ERROR_MESSAGE = stringPreferencesKey("last_error_message")
    }

    override val preferencesFlow: Flow<ClockFixPreferences> =
        context.clockDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { preferences ->
                ClockFixPreferences(
                    selectedLocaleTag = preferences[PreferencesKeys.SELECTED_LOCALE] ?: LocaleMode.DEFAULT.tag,
                    lastAppliedLocaleTag = preferences[PreferencesKeys.LAST_APPLIED_LOCALE],
                    lastApplySuccess = preferences[PreferencesKeys.LAST_APPLY_SUCCESS] ?: false,
                    lastApplyTimestamp = preferences[PreferencesKeys.LAST_APPLY_TIMESTAMP] ?: 0L,
                    lastVerifiedSystemLocale = preferences[PreferencesKeys.LAST_VERIFIED_SYSTEM_LOCALE],
                    lastErrorMessage = preferences[PreferencesKeys.LAST_ERROR_MESSAGE],
                )
            }

    override suspend fun saveSelectedLocale(localeTag: String) {
        context.clockDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LOCALE] = localeTag
        }
    }

    override suspend fun saveApplySuccess(
        appliedLocaleTag: String,
        verifiedLocale: String,
    ) {
        val now = System.currentTimeMillis()
        context.clockDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APPLIED_LOCALE] = appliedLocaleTag
            preferences[PreferencesKeys.LAST_APPLY_SUCCESS] = true
            preferences[PreferencesKeys.LAST_APPLY_TIMESTAMP] = now
            preferences[PreferencesKeys.LAST_VERIFIED_SYSTEM_LOCALE] = verifiedLocale
            preferences.remove(PreferencesKeys.LAST_ERROR_MESSAGE)
        }
    }

    override suspend fun saveApplyFailure(
        attemptedLocaleTag: String,
        errorMessage: String,
    ) {
        val now = System.currentTimeMillis()
        context.clockDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APPLY_SUCCESS] = false
            preferences[PreferencesKeys.LAST_APPLY_TIMESTAMP] = now
            preferences[PreferencesKeys.LAST_ERROR_MESSAGE] = errorMessage
        }
    }

    override suspend fun saveRestoreDefault(verifiedLocale: String) {
        val now = System.currentTimeMillis()
        context.clockDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_APPLIED_LOCALE] = ""
            preferences[PreferencesKeys.LAST_APPLY_SUCCESS] = true
            preferences[PreferencesKeys.LAST_APPLY_TIMESTAMP] = now
            preferences[PreferencesKeys.LAST_VERIFIED_SYSTEM_LOCALE] = verifiedLocale
            preferences.remove(PreferencesKeys.LAST_ERROR_MESSAGE)
        }
    }

    override suspend fun updateVerifiedSystemLocale(verifiedLocale: String?) {
        context.clockDataStore.edit { preferences ->
            if (verifiedLocale != null) {
                preferences[PreferencesKeys.LAST_VERIFIED_SYSTEM_LOCALE] = verifiedLocale
            } else {
                preferences.remove(PreferencesKeys.LAST_VERIFIED_SYSTEM_LOCALE)
            }
        }
    }
}
