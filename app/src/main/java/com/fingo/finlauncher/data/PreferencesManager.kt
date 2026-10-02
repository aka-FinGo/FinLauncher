package com.fingo.finlauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fin_launcher_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("favorite_apps")
        val HIDDEN_APPS_KEY = stringSetPreferencesKey("hidden_apps")
        val FIRST_RUN_DONE_KEY = booleanPreferencesKey("is_first_run_done")
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY] ?: emptySet()
    }

    val hiddenAppsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[HIDDEN_APPS_KEY] ?: emptySet()
    }

    val isFirstRunDoneFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_RUN_DONE_KEY] ?: false
    }

    suspend fun setFirstRunDone(done: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_RUN_DONE_KEY] = done
        }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferences[FAVORITES_KEY] = current
        }
    }

    suspend fun toggleHidden(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_APPS_KEY]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            preferences[HIDDEN_APPS_KEY] = current
        }
    }

    suspend fun setFavorites(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITES_KEY] = packages
        }
    }
}
