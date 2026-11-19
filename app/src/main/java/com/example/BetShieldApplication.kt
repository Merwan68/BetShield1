package com.example

import android.app.Application
import com.example.data.local.BetShieldDatabase
import com.example.data.local.GamblingDatabaseLoader
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.ProtectionRepository
import com.example.service.ProtectionNotificationHelper

class AppContainer(application: Application) {
  private val database = BetShieldDatabase.getInstance(application)
  private val databaseLoader = GamblingDatabaseLoader(application)
  val preferencesRepository = UserPreferencesRepository(application)
  val protectionRepository =
    ProtectionRepository(
      context = application,
      databaseLoader = databaseLoader,
      blockEventDao = database.blockEventDao(),
      customRuleDao = database.customRuleDao(),
      preferencesRepository = preferencesRepository,
    )
}

class BetShieldApplication : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    ProtectionNotificationHelper.ensureChannels(this)
    container = AppContainer(this)
  }
}
