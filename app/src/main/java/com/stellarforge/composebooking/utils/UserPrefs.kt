package com.stellarforge.composebooking.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * A utility class for managing local user preferences using [SharedPreferences].
 *
 * **Purpose:**
 * Supports the "Offline-First" architecture by caching essential user session data
 * (specifically the User Role). This allows the application to determine the correct
 * UI flow (Owner vs. Customer) immediately upon launch, without waiting for a network response.
 *
 * @param context Application context used to initialize SharedPreferences.
 */
class UserPrefs(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "booking_app_prefs"
        private const val KEY_USER_ROLE = "user_role"
    }

    /**
     * Persists the authenticated user's role locally.
     *
     * @param role The role string (e.g., "owner" or "customer").
     */
    fun saveUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    /**
     * Retrieves the cached user role.
     *
     * @return The role string if it exists, or null if the cache is empty (first install or logged out).
     */
    fun getUserRole(): String? {
        return prefs.getString(KEY_USER_ROLE, null)
    }

    /**
     * Clears all cached preferences.
     * Should be called when the user signs out to ensure security.
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}