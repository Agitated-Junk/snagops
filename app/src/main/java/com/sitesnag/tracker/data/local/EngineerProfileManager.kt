package com.sitesnag.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EngineerProfile(
    val fullName: String = "Site Engineer",
    val initials: String = "SE",
    val isConfigured: Boolean = false
) {
    val displayTag: String
        get() = "$fullName ($initials)"
}

class EngineerProfileManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("snagops_engineer_profile", Context.MODE_PRIVATE)

    private val _profileFlow = MutableStateFlow(loadProfile())
    val profileFlow: StateFlow<EngineerProfile> = _profileFlow.asStateFlow()

    fun getProfile(): EngineerProfile = _profileFlow.value

    fun isConfigured(): Boolean = prefs.getBoolean(KEY_CONFIGURED, false)

    fun saveProfile(fullName: String, rawInitials: String) {
        val sanitizedName = fullName.trim().ifBlank { "Site Engineer" }
        val sanitizedInitials = rawInitials.trim().uppercase()
            .filter { it.isLetter() }
            .take(4)
            .ifBlank {
                sanitizedName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .take(3)
                    .joinToString("")
                    .ifBlank { "SE" }
            }

        prefs.edit()
            .putString(KEY_NAME, sanitizedName)
            .putString(KEY_INITIALS, sanitizedInitials)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()

        _profileFlow.value = EngineerProfile(sanitizedName, sanitizedInitials, true)
    }

    private fun loadProfile(): EngineerProfile {
        val configured = prefs.getBoolean(KEY_CONFIGURED, false)
        val name = prefs.getString(KEY_NAME, "Site Engineer") ?: "Site Engineer"
        val initials = prefs.getString(KEY_INITIALS, "SE") ?: "SE"
        return EngineerProfile(name, initials, configured)
    }

    companion object {
        private const val KEY_NAME = "engineer_full_name"
        private const val KEY_INITIALS = "engineer_initials"
        private const val KEY_CONFIGURED = "engineer_is_configured"

        @Volatile
        private var INSTANCE: EngineerProfileManager? = null

        fun getInstance(context: Context): EngineerProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EngineerProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
