package com.example.service

import com.example.data.local.BlockListMatcher
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnProtectionState {
  STOPPED,
  STARTING,
  ACTIVE,
  PAUSED,
  ERROR,
}

object ServiceStatusBridge {
  private val _vpnState = MutableStateFlow(VpnProtectionState.STOPPED)
  val vpnState: StateFlow<VpnProtectionState> = _vpnState.asStateFlow()

  private val _vpnStatusMessage = MutableStateFlow<String?>(null)
  val vpnStatusMessage: StateFlow<String?> = _vpnStatusMessage.asStateFlow()

  private val _accessibilityActive = MutableStateFlow(false)
  val accessibilityActive: StateFlow<Boolean> = _accessibilityActive.asStateFlow()

  private val _sessionBlockedQueries = MutableStateFlow(0)
  val sessionBlockedQueries: StateFlow<Int> = _sessionBlockedQueries.asStateFlow()

  private val _sessionForwardedQueries = MutableStateFlow(0)
  val sessionForwardedQueries: StateFlow<Int> = _sessionForwardedQueries.asStateFlow()

  val activeMatcher: AtomicReference<BlockListMatcher> =
    AtomicReference(
      BlockListMatcher(
        platforms = emptyList(),
        enabledCategories = emptySet(),
        disabledPlatformIds = emptySet(),
        customRules = emptyList(),
      )
    )

  fun setVpnState(state: VpnProtectionState, message: String? = null) {
    _vpnState.value = state
    _vpnStatusMessage.value = message
  }

  fun setAccessibilityActive(active: Boolean) {
    _accessibilityActive.value = active
  }

  fun incrementBlockedQueries() {
    _sessionBlockedQueries.value = _sessionBlockedQueries.value + 1
  }

  fun incrementForwardedQueries() {
    _sessionForwardedQueries.value = _sessionForwardedQueries.value + 1
  }

  fun resetSessionCounters() {
    _sessionBlockedQueries.value = 0
    _sessionForwardedQueries.value = 0
  }
}
