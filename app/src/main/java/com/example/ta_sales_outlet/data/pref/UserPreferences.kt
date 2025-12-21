package com.example.ta_sales_outlet.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Model Data untuk Session
data class UserModel(
    val email: String,
    val role: String,
    val userId: Int,
    val name: String,
    val isLogin: Boolean
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sales_settings")

class UserPreferences(private val context: Context) {

    companion object {
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ID_KEY = intPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    // 1. FUNGSI UNTUK MAIN ACTIVITY (Ambil semua sekaligus)
    fun getSession(): Flow<UserModel> {
        return context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                UserModel(
                    email = preferences[USER_EMAIL_KEY] ?: "",
                    role = preferences[USER_ROLE_KEY] ?: "",
                    userId = preferences[USER_ID_KEY] ?: 0,
                    name = preferences[USER_NAME_KEY] ?: "",
                    isLogin = preferences[IS_LOGIN_KEY] ?: false
                )
            }
    }

    // 2. VARIABEL INDIVIDUAL (INI YANG TADI HILANG - UNTUK HOMESCREEN)
    // ------------------------------------------------------------------
    val isLogin: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[IS_LOGIN_KEY] ?: false }

    val userRole: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_ROLE_KEY] ?: "" }

    val userId: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_ID_KEY] ?: 0 }     // <--- SalesHomeScreen butuh ini

    val userName: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[USER_NAME_KEY] ?: "" }  // <--- SalesHomeScreen butuh ini
    // ------------------------------------------------------------------

    // 3. SIMPAN DATA
    suspend fun saveSession(
        isLogin: Boolean,
        role: String,
        email: String,
        userId: Int,
        name: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGIN_KEY] = isLogin
            preferences[USER_ROLE_KEY] = role
            preferences[USER_EMAIL_KEY] = email
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = name
        }
    }

    // 4. HAPUS DATA (LOGOUT)
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}