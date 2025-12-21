package com.example.ta_sales_outlet.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sales_settings")

class UserPreferences(private val context: Context) {

    companion object {
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        // TAMBAHAN BARU: Kunci untuk ID dan Nama
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    // --- BACA DATA ---
    val isLogin: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_LOGIN_KEY] ?: false }

    val userRole: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_ROLE_KEY] ?: "" }

    val userEmail: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_EMAIL_KEY] ?: "" }

    // BACA ID & NAMA (Untuk Session Manager di MainActivity)
    val userId: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_ID_KEY] ?: 0 }

    val userName: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_NAME_KEY] ?: "" }

    // --- SIMPAN DATA (UPDATE BAGIAN INI) ---
    suspend fun saveSession(
        isLogin: Boolean,
        role: String,
        email: String,
        userId: Int,    // Parameter Baru
        name: String    // Parameter Baru
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGIN_KEY] = isLogin
            preferences[USER_ROLE_KEY] = role
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ID_KEY] = userId
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}