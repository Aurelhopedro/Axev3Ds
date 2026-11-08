package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages GitHub auth token with encrypted storage via Android Keystore.
 * Tokens are NEVER stored in plain text.
 */
class AuthTokenManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "axev_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback: clear and recreate (handles key rotation edge cases)
            context.getSharedPreferences("axev_secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "axev_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "gh_access_token"
        private const val KEY_SELECTED_REPO = "selected_repo"
        private const val KEY_CREATED_REPO_URL = "created_repo_url"
        private const val KEY_CREATED_REPO_NAME = "created_repo_name"
        private const val KEY_CREATED_REPO_OWNER = "created_repo_owner"
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun hasToken(): Boolean = !getAccessToken().isNullOrBlank()

    fun saveSelectedRepo(repoFullName: String) {
        prefs.edit().putString(KEY_SELECTED_REPO, repoFullName).apply()
    }

    fun getSelectedRepo(): String? = prefs.getString(KEY_SELECTED_REPO, null)

    fun saveCreatedRepo(name: String, owner: String, htmlUrl: String) {
        prefs.edit()
            .putString(KEY_CREATED_REPO_NAME, name)
            .putString(KEY_CREATED_REPO_OWNER, owner)
            .putString(KEY_CREATED_REPO_URL, htmlUrl)
            .apply()
    }

    fun getCreatedRepoUrl(): String? = prefs.getString(KEY_CREATED_REPO_URL, null)
    fun getCreatedRepoName(): String? = prefs.getString(KEY_CREATED_REPO_NAME, null)
    fun getCreatedRepoOwner(): String? = prefs.getString(KEY_CREATED_REPO_OWNER, null)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Returns the token formatted for Authorization header */
    fun getBearerToken(): String? {
        val token = getAccessToken() ?: return null
        return "Bearer $token"
    }
}
