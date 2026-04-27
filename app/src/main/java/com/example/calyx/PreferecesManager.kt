package com.example.calyx

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calyx_prefs")

object PreferencesManager {

    private val CHANNEL_ID_KEY       = stringPreferencesKey("channel_id")
    private val HUMIDITY_FIELD_KEY   = stringPreferencesKey("humidity_field_key")

    fun getChannelId(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[CHANNEL_ID_KEY] ?: Constants.CHANNEL_ID
        }

    fun getHumidityFieldKey(context: Context): Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[HUMIDITY_FIELD_KEY] ?: ""  // empty = auto-detect
        }

    suspend fun saveChannelId(context: Context, channelId: String) {
        context.dataStore.edit { prefs ->
            prefs[CHANNEL_ID_KEY] = channelId
        }
    }

    suspend fun saveHumidityFieldKey(context: Context, fieldKey: String) {
        context.dataStore.edit { prefs ->
            prefs[HUMIDITY_FIELD_KEY] = fieldKey
        }
    }
}