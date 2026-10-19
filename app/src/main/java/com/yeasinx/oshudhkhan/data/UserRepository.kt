package com.yeasinx.oshudhkhan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore(name = "user_profile")

// Local profile store. When Firestore is added, back this class with it; callers stay unchanged.
class UserRepository(context: Context) {
    private val dataStore = context.applicationContext.userDataStore

    val profile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            name = prefs[NAME] ?: "",
            age = prefs[AGE],
            email = prefs[EMAIL] ?: ""
        )
    }

    suspend fun save(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[NAME] = profile.name
            prefs[EMAIL] = profile.email
            if (profile.age != null) prefs[AGE] = profile.age else prefs.remove(AGE)
        }
    }

    private companion object {
        val NAME = stringPreferencesKey("name")
        val AGE = intPreferencesKey("age")
        val EMAIL = stringPreferencesKey("email")
    }
}
