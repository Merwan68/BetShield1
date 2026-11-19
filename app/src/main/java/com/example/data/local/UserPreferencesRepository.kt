package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PlatformCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.betShieldPrefs: DataStore<Preferences> by
  preferencesDataStore(name = "betshield_settings")

enum class AppThemeMode(val key: String) {
  SYSTEM("SYSTEM"),
  LIGHT("LIGHT"),
  DARK("DARK");

  companion object {
    fun fromKey(key: String?): AppThemeMode =
      entries.firstOrNull { it.key == key } ?: SYSTEM
  }
}

enum class AppLanguage(val code: String, val displayName: String) {
  ENGLISH("en", "English"),
  AMHARIC("am", "አማርኛ (Amharic)");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code == code } ?: ENGLISH
  }
}

data class UserSettings(
  val onboardingCompleted: Boolean = false,
  val protectionDesired: Boolean = false,
  val enabledCategories: Set<PlatformCategory> = PlatformCategory.entries.toSet(),
  val disabledPlatformIds: Set<String> = emptySet(),
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val notificationsEnabled: Boolean = true,
  val coolingOffSeconds: Int = 60,
  val autoUpdateEnabled: Boolean = false,
  val remoteDatabaseUrl: String = DEFAULT_REMOTE_DB_URL,
  val lastRemoteSyncMillis: Long = 0L,
) {
  companion object {
    const val DEFAULT_REMOTE_DB_URL =
      "https://raw.githubusercontent.com/betshield-org/betshield-database/main/gambling_database.json"
  }
}

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val PROTECTION_DESIRED = booleanPreferencesKey("protection_desired")
    val ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")
    val DISABLED_PLATFORM_IDS = stringSetPreferencesKey("disabled_platform_ids")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("language")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val COOLING_OFF_SECONDS = intPreferencesKey("cooling_off_seconds")
    val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
    val REMOTE_DB_URL = stringPreferencesKey("remote_db_url")
    val LAST_REMOTE_SYNC_MILLIS = longPreferencesKey("last_remote_sync_millis")
  }

  val settingsFlow: Flow<UserSettings> =
    context.betShieldPrefs.data.map { prefs ->
      val categoryKeys =
        prefs[Keys.ENABLED_CATEGORIES]
          ?: PlatformCategory.entries.map { it.storageKey }.toSet()
      UserSettings(
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        protectionDesired = prefs[Keys.PROTECTION_DESIRED] ?: false,
        enabledCategories = categoryKeys.map { PlatformCategory.fromKey(it) }.toSet(),
        disabledPlatformIds = prefs[Keys.DISABLED_PLATFORM_IDS] ?: emptySet(),
        themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE]),
        language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
        notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        coolingOffSeconds = prefs[Keys.COOLING_OFF_SECONDS] ?: 60,
        autoUpdateEnabled = prefs[Keys.AUTO_UPDATE_ENABLED] ?: false,
        remoteDatabaseUrl =
          prefs[Keys.REMOTE_DB_URL]?.takeIf { it.isNotBlank() }
            ?: UserSettings.DEFAULT_REMOTE_DB_URL,
        lastRemoteSyncMillis = prefs[Keys.LAST_REMOTE_SYNC_MILLIS] ?: 0L,
      )
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.betShieldPrefs.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setProtectionDesired(desired: Boolean) {
    context.betShieldPrefs.edit { it[Keys.PROTECTION_DESIRED] = desired }
  }

  suspend fun toggleCategory(category: PlatformCategory, enabled: Boolean) {
    context.betShieldPrefs.edit { prefs ->
      val current =
        prefs[Keys.ENABLED_CATEGORIES]?.toMutableSet()
          ?: PlatformCategory.entries.map { it.storageKey }.toMutableSet()
      if (enabled) current.add(category.storageKey) else current.remove(category.storageKey)
      prefs[Keys.ENABLED_CATEGORIES] = current
    }
  }

  suspend fun setPlatformEnabled(platformId: String, enabled: Boolean) {
    context.betShieldPrefs.edit { prefs ->
      val disabled = (prefs[Keys.DISABLED_PLATFORM_IDS] ?: emptySet()).toMutableSet()
      if (enabled) disabled.remove(platformId) else disabled.add(platformId)
      prefs[Keys.DISABLED_PLATFORM_IDS] = disabled
    }
  }

  suspend fun setThemeMode(mode: AppThemeMode) {
    context.betShieldPrefs.edit { it[Keys.THEME_MODE] = mode.key }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.betShieldPrefs.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    context.betShieldPrefs.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
  }

  suspend fun setCoolingOffSeconds(seconds: Int) {
    context.betShieldPrefs.edit { it[Keys.COOLING_OFF_SECONDS] = seconds.coerceIn(15, 300) }
  }

  suspend fun setAutoUpdateEnabled(enabled: Boolean) {
    context.betShieldPrefs.edit { it[Keys.AUTO_UPDATE_ENABLED] = enabled }
  }

  suspend fun setRemoteDatabaseUrl(url: String) {
    context.betShieldPrefs.edit { it[Keys.REMOTE_DB_URL] = url.trim() }
  }

  suspend fun recordRemoteSyncTimestamp(timestampMillis: Long) {
    context.betShieldPrefs.edit { it[Keys.LAST_REMOTE_SYNC_MILLIS] = timestampMillis }
  }
}
