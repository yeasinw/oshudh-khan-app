package com.yeasinx.oshudhkhan.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_profile")

// Local profile store, mirrored to the cloud backup while signed in.
class UserRepository(context: Context, private val backup: CloudBackup) {
    private val dataStore = context.applicationContext.userDataStore

    val profile: Flow<UserProfile> = dataStore.data.map { it.toProfile() }

    suspend fun save(profile: UserProfile) {
        val now = System.currentTimeMillis()
        write(profile, now)
        backup.pushProfile(profile.toRecord(now))
    }

    // The profile as a sync record; updatedAt is 0 if it was never saved, so the cloud copy wins
    suspend fun record(): SyncRecord {
        val prefs = dataStore.data.first()
        return prefs.toProfile().toRecord(prefs[UPDATED_AT] ?: 0L)
    }

    suspend fun saveFromCloud(record: SyncRecord) = write(profileFromRecord(record), record.updatedAt)

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private suspend fun write(profile: UserProfile, updatedAt: Long) {
        dataStore.edit { prefs ->
            prefs[NAME] = profile.name
            prefs[EMAIL] = profile.email
            if (profile.age != null) prefs[AGE] = profile.age else prefs.remove(AGE)
            prefs[UPDATED_AT] = updatedAt
        }
    }

    private fun Preferences.toProfile() = UserProfile(
        name = this[NAME] ?: "",
        age = this[AGE],
        email = this[EMAIL] ?: ""
    )

    private companion object {
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val EMAIL = stringPreferencesKey("email")
        val UPDATED_AT = longPreferencesKey("updated_at")
    }
}
