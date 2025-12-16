package com.example.ta_sales_outlet.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Membuat instance DataStore (seperti database kecil key-value)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_ROLE = stringPreferencesKey("role") // "SALESPERSON" atau "OUTLET"
        val KEY_NAME = stringPreferencesKey("name")
        val KEY_USER_ID = intPreferencesKey("user_id") // Sesuai database Anda (Integer)
        val KEY_IS_LOGIN = androidx.datastore.preferences.core.booleanPreferencesKey("is_login")
    }

    // 1. Simpan Sesi Login
    suspend fun saveSession(token: String, role: String, name: String, userId: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
            preferences[KEY_ROLE] = role
            preferences[KEY_NAME] = name
            preferences[KEY_USER_ID] = userId
            preferences[KEY_IS_LOGIN] = true
        }
    }

    // 2. Ambil Data Sesi (Menggunakan Flow agar reaktif / real-time)
    val userToken: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }
    val userRole: Flow<String?> = context.dataStore.data.map { it[KEY_ROLE] }
    val userName: Flow<String?> = context.dataStore.data.map { it[KEY_NAME] }
    val userId: Flow<Int?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val isLogin: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGIN] ?: false }

    // 3. Logout (Hapus Sesi)
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}