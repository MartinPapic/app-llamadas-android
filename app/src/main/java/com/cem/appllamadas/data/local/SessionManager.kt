package com.cem.appllamadas.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona la sesión del usuario guardando el JWT de forma cifrada
 * con EncryptedSharedPreferences (RNF-A9).
 *
 * En caso de expiración, el token se borra y la app redirige al Login.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_FILE = "secure_session"
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_NOMBRE        = "nombre"
        private const val KEY_ROL           = "rol"

        private const val KEY_SAVED_EMAIL   = "saved_email"
        private const val KEY_SAVED_PASSWORD= "saved_password"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Android Keystore corruption fallback (prevents fatal startup crash)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().clear().apply()
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        nombre: String,
        rol: String
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getNombre(): String? = prefs.getString(KEY_NOMBRE, null)
    fun getRol(): String? = prefs.getString(KEY_ROL, null)

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_NOMBRE)
            .remove(KEY_ROL)
            .apply()
    }

    // --- Funciones para Recordar Credenciales ---

    fun saveCredentials(email: String, pass: String) {
        prefs.edit()
            .putString(KEY_SAVED_EMAIL, email)
            .putString(KEY_SAVED_PASSWORD, pass)
            .apply()
    }

    fun getSavedEmail(): String? = prefs.getString(KEY_SAVED_EMAIL, null)
    fun getSavedPassword(): String? = prefs.getString(KEY_SAVED_PASSWORD, null)

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_SAVED_EMAIL)
            .remove(KEY_SAVED_PASSWORD)
            .apply()
    }
}
