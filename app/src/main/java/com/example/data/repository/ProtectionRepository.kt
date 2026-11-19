package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.local.BlockEventDao
import com.example.data.local.BlockListMatcher
import com.example.data.local.BlockMatchResult
import com.example.data.local.CustomRuleDao
import com.example.data.local.GamblingDatabaseLoader
import com.example.data.local.TargetCountProjection
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.BlockEventEntity
import com.example.data.model.BlockEventSource
import com.example.data.model.BlockEventType
import com.example.data.model.CustomRuleEntity
import com.example.data.model.CustomRuleType
import com.example.data.model.GamblingDatabasePayload
import com.example.data.model.InstalledAppSummary
import com.example.data.model.PlatformCategory
import com.example.service.ServiceStatusBridge
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface DatabaseSyncState {
  data object Idle : DatabaseSyncState

  data object Updating : DatabaseSyncState

  data class Success(val version: String, val platformCount: Int) : DatabaseSyncState

  data class Error(val reason: String) : DatabaseSyncState
}

class ProtectionRepository(
  private val context: Context,
  private val databaseLoader: GamblingDatabaseLoader,
  private val blockEventDao: BlockEventDao,
  private val customRuleDao: CustomRuleDao,
  val preferencesRepository: UserPreferencesRepository,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(12, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()

  private val _databasePayload = MutableStateFlow(databaseLoader.loadBundledDatabase())
  val databasePayload: StateFlow<GamblingDatabasePayload> = _databasePayload.asStateFlow()

  private val _syncState = MutableStateFlow<DatabaseSyncState>(DatabaseSyncState.Idle)
  val syncState: StateFlow<DatabaseSyncState> = _syncState.asStateFlow()

  private val _installedApps = MutableStateFlow<List<InstalledAppSummary>>(emptyList())
  val installedApps: StateFlow<List<InstalledAppSummary>> = _installedApps.asStateFlow()

  val userSettings: Flow<UserSettings> = preferencesRepository.settingsFlow
  val customRules: Flow<List<CustomRuleEntity>> = customRuleDao.observeAllRules()
  val recentEvents: Flow<List<BlockEventEntity>> = blockEventDao.observeRecentEvents()
  val totalBlockCount: Flow<Int> = blockEventDao.observeTotalCount()
  val topBlockedTargets: Flow<List<TargetCountProjection>> =
    blockEventDao.observeTopBlockedTargets()

  val activeMatcherFlow: Flow<BlockListMatcher> =
    combine(_databasePayload, userSettings, customRules) { payload, settings, rules ->
      BlockListMatcher(
        platforms = payload.platforms,
        enabledCategories = settings.enabledCategories,
        disabledPlatformIds = settings.disabledPlatformIds,
        customRules = rules,
      )
    }

  init {
    scope.launch {
      _databasePayload.value = databaseLoader.loadActiveDatabase()
      refreshInstalledApps()
    }
    scope.launch {
      activeMatcherFlow.collect { matcher ->
        ServiceStatusBridge.activeMatcher.set(matcher)
      }
    }
  }

  suspend fun refreshInstalledApps() =
    withContext(Dispatchers.IO) {
      val pm = context.packageManager
      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
      val resolved =
        runCatching {
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
          }
          .getOrDefault(emptyList())

      val knownPackages =
        _databasePayload.value.platforms
          .flatMap { platform -> platform.packageIds.map { it to platform.name } }
          .toMap()

      val customBlockedPackages =
        customRules.first()
          .filter { it.ruleType == CustomRuleType.APP.key && it.enabled }
          .map { it.value }
          .toSet()

      val ownPackage = context.packageName
      val summaries =
        resolved
          .mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == ownPackage) return@mapNotNull null
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { pkg }
            val matchedPlatform = knownPackages[pkg]
            InstalledAppSummary(
              packageName = pkg,
              appName = label,
              isKnownGamblingPackage = matchedPlatform != null,
              isBlockedByUser = pkg in customBlockedPackages,
              matchedPlatformName = matchedPlatform,
            )
          }
          .distinctBy { it.packageName }
          .sortedWith(
            compareByDescending<InstalledAppSummary> { it.isKnownGamblingPackage }
              .thenByDescending { it.isBlockedByUser }
              .thenBy { it.appName.lowercase() }
          )

      _installedApps.value = summaries
    }

  suspend fun addCustomDomain(domainInput: String, label: String = ""): Result<String> {
    val normalized =
      BlockListMatcher.normalizeDomain(domainInput)
        ?: return Result.failure(
          IllegalArgumentException("Enter a valid domain such as examplebet.com")
        )
    customRuleDao.upsertRule(
      CustomRuleEntity(
        value = normalized,
        label = label.trim().ifBlank { normalized },
        ruleType = CustomRuleType.DOMAIN.key,
        enabled = true,
      )
    )
    return Result.success(normalized)
  }

  suspend fun addOrToggleBlockedApp(packageName: String, appLabel: String, block: Boolean) {
    val cleanPkg = packageName.trim()
    if (cleanPkg.isEmpty()) return
    if (block) {
      customRuleDao.upsertRule(
        CustomRuleEntity(
          value = cleanPkg,
          label = appLabel.trim().ifBlank { cleanPkg },
          ruleType = CustomRuleType.APP.key,
          enabled = true,
        )
      )
    } else {
      customRuleDao.deleteByValueAndType(cleanPkg, CustomRuleType.APP.key)
    }
    refreshInstalledApps()
  }

  suspend fun toggleCustomRule(ruleId: Long, enabled: Boolean) {
    customRuleDao.setRuleEnabled(ruleId, enabled)
    refreshInstalledApps()
  }

  suspend fun deleteCustomRule(ruleId: Long) {
    customRuleDao.deleteById(ruleId)
    refreshInstalledApps()
  }

  suspend fun toggleCategory(category: PlatformCategory, enabled: Boolean) {
    preferencesRepository.toggleCategory(category, enabled)
  }

  suspend fun setPlatformEnabled(platformId: String, enabled: Boolean) {
    preferencesRepository.setPlatformEnabled(platformId, enabled)
  }

  fun testDomainMatch(domainInput: String): BlockMatchResult? {
    return ServiceStatusBridge.activeMatcher.get().matchDomain(domainInput)
  }

  suspend fun recordBlockEvent(
    target: String,
    type: BlockEventType,
    category: String,
    source: BlockEventSource,
  ) {
    blockEventDao.insert(
      BlockEventEntity(
        target = target,
        type = type.key,
        category = category,
        source = source.key,
      )
    )
  }

  suspend fun clearBlockHistory() {
    blockEventDao.clearAll()
  }

  suspend fun updateRemoteDatabase(url: String): DatabaseSyncState =
    withContext(Dispatchers.IO) {
      val cleanUrl = url.trim()
      if (!cleanUrl.startsWith("https://")) {
        val err = DatabaseSyncState.Error("Remote database URL must use HTTPS.")
        _syncState.value = err
        return@withContext err
      }
      _syncState.value = DatabaseSyncState.Updating
      return@withContext try {
        val request = Request.Builder().url(cleanUrl).get().build()
        httpClient.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            val err = DatabaseSyncState.Error("Server returned HTTP ${response.code}")
            _syncState.value = err
            return@withContext err
          }
          val body = response.body?.string().orEmpty()
          val saved = databaseLoader.saveRemoteOverride(body)
          saved.fold(
            onSuccess = { payload ->
              _databasePayload.value = payload
              preferencesRepository.recordRemoteSyncTimestamp(System.currentTimeMillis())
              refreshInstalledApps()
              val ok = DatabaseSyncState.Success(payload.version, payload.platforms.size)
              _syncState.value = ok
              ok
            },
            onFailure = { parseErr ->
              val err =
                DatabaseSyncState.Error(
                  parseErr.message ?: "Downloaded JSON failed schema validation"
                )
              _syncState.value = err
              err
            },
          )
        }
      } catch (e: IOException) {
        val err =
          DatabaseSyncState.Error(
            "Offline or unreachable host. Local bundled database remains active."
          )
        _syncState.value = err
        err
      }
    }

  suspend fun resetToBundledDatabase() {
    _databasePayload.value = databaseLoader.resetToBundled()
    _syncState.value = DatabaseSyncState.Idle
    refreshInstalledApps()
  }
}
