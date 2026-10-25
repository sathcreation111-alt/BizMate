package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bizmate_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_SHOP_NAME = stringPreferencesKey("shop_name")
        private val KEY_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "si"
    }

    val shopNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOP_NAME] ?: "BizMate Store"
    }

    val isSetupCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SETUP_COMPLETED] ?: false
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    suspend fun setShopName(shopName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOP_NAME] = shopName
        }
    }

    suspend fun completeSetup(shopName: String, language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOP_NAME] = shopName
            preferences[KEY_LANGUAGE] = language
            preferences[KEY_SETUP_COMPLETED] = true
        }
    }
}
