package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.local.BlockMatchResult
import com.example.data.local.TargetCountProjection
import com.example.data.local.UserSettings
import com.example.data.model.BlockEventEntity
import com.example.data.model.BlockEventSource
import com.example.data.model.BlockEventType
import com.example.data.model.CustomRuleEntity
import com.example.data.model.GamblingDatabasePayload
import com.example.data.model.InstalledAppSummary
import com.example.data.model.PlatformCategory
import com.example.data.repository.DatabaseSyncState
import com.example.data.repository.ProtectionRepository
import com.example.service.ServiceStatusBridge
import com.example.service.VpnProtectionState
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StatsTimeframe {
  DAILY,
  WEEKLY,
  MONTHLY,
}

data class StatBarBucket(
  val label: String,
  val count: Int,
)

data class BetShieldUiState(
  val settings: UserSettings = UserSettings(),
  val vpnState: VpnProtectionState = VpnProtectionState.STOPPED,
  val vpnStatusMessage: String? = null,
  val accessibilityActive: Boolean = false,
  val sessionBlockedQueries: Int = 0,
  val sessionForwardedQueries: Int = 0,
  val databasePayload: GamblingDatabasePayload? = null,
  val activeDomainCount: Int = 0,
  val activePackageCount: Int = 0,
  val totalRecordedBlocks: Int = 0,
  val recentEvents: List<BlockEventEntity> = emptyList(),
  val topBlockedTargets: List<TargetCountProjection> = emptyList(),
  val customRules: List<CustomRuleEntity> = emptyList(),
  val installedApps: List<InstalledAppSummary> = emptyList(),
  val syncState: DatabaseSyncState = DatabaseSyncState.Idle,
)

class BetShieldViewModel(private val repository: ProtectionRepository) : ViewModel() {

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _domainTestResult = MutableStateFlow<Pair<String, BlockMatchResult?>?>(null)
  val domainTestResult: StateFlow<Pair<String, BlockMatchResult?>?> =
    _domainTestResult.asStateFlow()

  private val coreStateFlow =
    combine(
      repository.userSettings,
      ServiceStatusBridge.vpnState,
      ServiceStatusBridge.vpnStatusMessage,
      ServiceStatusBridge.accessibilityActive,
      repository.activeMatcherFlow,
    ) { settings, vpnState, vpnMsg, accessibilityActive, matcher ->
      CoreBundle(settings, vpnState, vpnMsg, accessibilityActive, matcher)
    }

  private val dataStateFlow =
    combine(
      repository.databasePayload,
      repository.totalBlockCount,
      repository.recentEvents,
      repository.topBlockedTargets,
      repository.customRules,
    ) { payload, totalBlocks, events, topTargets, rules ->
      DataBundle(payload, totalBlocks, events, topTargets, rules)
    }

  private val auxStateFlow =
    combine(
      repository.installedApps,
      repository.syncState,
      ServiceStatusBridge.sessionBlockedQueries,
      ServiceStatusBridge.sessionForwardedQueries,
    ) { apps, syncState, sessionBlocked, sessionForwarded ->
      AuxBundle(apps, syncState, sessionBlocked, sessionForwarded)
    }

  val uiState: StateFlow<BetShieldUiState> =
    combine(coreStateFlow, dataStateFlow, auxStateFlow) { core, data, aux ->
        BetShieldUiState(
          settings = core.settings,
          vpnState = core.vpnState,
          vpnStatusMessage = core.vpnMsg,
          accessibilityActive = core.accessibilityActive,
          sessionBlockedQueries = aux.sessionBlocked,
          sessionForwardedQueries = aux.sessionForwarded,
          databasePayload = data.payload,
          activeDomainCount = core.matcher.activeDomainCount,
          activePackageCount = core.matcher.activePackageCount,
          totalRecordedBlocks = data.totalBlocks,
          recentEvents = data.events,
          topBlockedTargets = data.topTargets,
          customRules = data.rules,
          installedApps = aux.apps,
          syncState = aux.syncState,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BetShieldUiState(),
      )

  fun completeOnboarding() {
    viewModelScope.launch { repository.preferencesRepository.setOnboardingCompleted(true) }
  }

  fun setProtectionDesired(desired: Boolean) {
    viewModelScope.launch { repository.preferencesRepository.setProtectionDesired(desired) }
  }

  fun toggleCategory(category: PlatformCategory, enabled: Boolean) {
    viewModelScope.launch { repository.toggleCategory(category, enabled) }
  }

  fun setPlatformEnabled(platformId: String, enabled: Boolean) {
    viewModelScope.launch { repository.setPlatformEnabled(platformId, enabled) }
  }

  fun addCustomDomain(domainInput: String) {
    viewModelScope.launch {
      val result = repository.addCustomDomain(domainInput)
      result.fold(
        onSuccess = { normalized -> _feedbackMessage.value = "Added $normalized to domain filter." },
        onFailure = { err -> _feedbackMessage.value = err.message ?: "Invalid domain format." },
      )
    }
  }

  fun addManualBlockedPackage(packageId: String, label: String) {
    val cleanPkg = packageId.trim()
    if (!cleanPkg.contains(".") || cleanPkg.contains(" ")) {
      _feedbackMessage.value = "Enter a valid Android package ID (e.g. com.example.bet)."
      return
    }
    viewModelScope.launch {
      repository.addOrToggleBlockedApp(cleanPkg, label.ifBlank { cleanPkg }, block = true)
      _feedbackMessage.value = "Added $cleanPkg to App Guard blocklist."
    }
  }

  fun toggleInstalledAppBlock(app: InstalledAppSummary, block: Boolean) {
    viewModelScope.launch {
      repository.addOrToggleBlockedApp(app.packageName, app.appName, block)
    }
  }

  fun toggleCustomRule(ruleId: Long, enabled: Boolean) {
    viewModelScope.launch { repository.toggleCustomRule(ruleId, enabled) }
  }

  fun deleteCustomRule(ruleId: Long) {
    viewModelScope.launch { repository.deleteCustomRule(ruleId) }
  }

  fun refreshInstalledApps() {
    viewModelScope.launch { repository.refreshInstalledApps() }
  }

  fun testDomainFilter(domainInput: String, recordCheckEvent: Boolean = true) {
    val trimmed = domainInput.trim()
    if (trimmed.isEmpty()) return
    val match = repository.testDomainMatch(trimmed)
    _domainTestResult.value = trimmed to match
    if (recordCheckEvent && match != null) {
      viewModelScope.launch {
        repository.recordBlockEvent(
          target = match.matchedTarget,
          type = BlockEventType.DOMAIN,
          category = match.category,
          source = BlockEventSource.FILTER_CHECK,
        )
      }
    }
  }

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun clearHistory() {
    viewModelScope.launch {
      repository.clearBlockHistory()
      _feedbackMessage.value = "Blocking history cleared."
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch { repository.preferencesRepository.setThemeMode(mode) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { repository.preferencesRepository.setLanguage(language) }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.preferencesRepository.setNotificationsEnabled(enabled) }
  }

  fun setCoolingOffSeconds(seconds: Int) {
    viewModelScope.launch { repository.preferencesRepository.setCoolingOffSeconds(seconds) }
  }

  fun setRemoteDatabaseUrl(url: String) {
    viewModelScope.launch { repository.preferencesRepository.setRemoteDatabaseUrl(url) }
  }

  fun triggerRemoteDatabaseUpdate(url: String) {
    viewModelScope.launch {
      val result = repository.updateRemoteDatabase(url)
      when (result) {
        is DatabaseSyncState.Success ->
          _feedbackMessage.value =
            "Updated database to v${result.version} (${result.platformCount} platforms)."
        is DatabaseSyncState.Error -> _feedbackMessage.value = result.reason
        else -> Unit
      }
    }
  }

  fun resetDatabaseToBundled() {
    viewModelScope.launch {
      repository.resetToBundledDatabase()
      _feedbackMessage.value = "Restored bundled offline database."
    }
  }

  fun computeStatBuckets(
    events: List<BlockEventEntity>,
    timeframe: StatsTimeframe,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<StatBarBucket> {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    return when (timeframe) {
      StatsTimeframe.DAILY -> {
        // Last 6 four-hour blocks today
        val startOfDay =
          Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }.timeInMillis
        val labels = listOf("00h", "04h", "08h", "12h", "16h", "20h")
        val counts = IntArray(6)
        for (ev in events) {
          if (ev.timestampMillis >= startOfDay) {
            val hour = ((ev.timestampMillis - startOfDay) / (4L * 3600_000L)).toInt().coerceIn(0, 5)
            counts[hour]++
          }
        }
        labels.mapIndexed { idx, label -> StatBarBucket(label, counts[idx]) }
      }
      StatsTimeframe.WEEKLY -> {
        val dayLabels = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val buckets = mutableListOf<StatBarBucket>()
        for (daysAgo in 6 downTo 0) {
          val dayCal =
            (cal.clone() as Calendar).apply {
              add(Calendar.DAY_OF_YEAR, -daysAgo)
              set(Calendar.HOUR_OF_DAY, 0)
              set(Calendar.MINUTE, 0)
              set(Calendar.SECOND, 0)
              set(Calendar.MILLISECOND, 0)
            }
          val start = dayCal.timeInMillis
          val end = start + 86_400_000L
          val count = events.count { it.timestampMillis in start until end }
          val label = dayLabels[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
          buckets.add(StatBarBucket(label, count))
        }
        buckets
      }
      StatsTimeframe.MONTHLY -> {
        val buckets = mutableListOf<StatBarBucket>()
        for (weekIdx in 3 downTo 0) {
          val end = nowMillis - weekIdx * 7L * 86_400_000L
          val start = end - 7L * 86_400_000L
          val count = events.count { it.timestampMillis in start until end }
          buckets.add(StatBarBucket("W${4 - weekIdx}", count))
        }
        buckets
      }
    }
  }

  private data class CoreBundle(
    val settings: UserSettings,
    val vpnState: VpnProtectionState,
    val vpnMsg: String?,
    val accessibilityActive: Boolean,
    val matcher: com.example.data.local.BlockListMatcher,
  )

  private data class DataBundle(
    val payload: GamblingDatabasePayload,
    val totalBlocks: Int,
    val events: List<BlockEventEntity>,
    val topTargets: List<TargetCountProjection>,
    val rules: List<CustomRuleEntity>,
  )

  private data class AuxBundle(
    val apps: List<InstalledAppSummary>,
    val syncState: DatabaseSyncState,
    val sessionBlocked: Int,
    val sessionForwarded: Int,
  )

  class Factory(private val repository: ProtectionRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return BetShieldViewModel(repository) as T
    }
  }
}
