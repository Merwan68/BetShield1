package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.BetShieldApplication
import com.example.BlockedInterceptActivity
import com.example.data.model.BlockEventSource
import com.example.data.model.BlockEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Privacy-preserving AccessibilityService that listens ONLY to window state package changes
 * (never retrieves window text or keystrokes) and redirects away from user-blocked gambling apps.
 */
class BetShieldAccessibilityService : AccessibilityService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var lastInterceptedPkg: String? = null
  private var lastInterceptTimestamp: Long = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    ServiceStatusBridge.setAccessibilityActive(true)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val pkg = event.packageName?.toString()?.trim() ?: return
    if (pkg == packageName || pkg == "com.android.systemui") return

    val match = ServiceStatusBridge.activeMatcher.get().matchPackage(pkg) ?: return

    val now = System.currentTimeMillis()
    if (pkg == lastInterceptedPkg && now - lastInterceptTimestamp < 2500L) return
    lastInterceptedPkg = pkg
    lastInterceptTimestamp = now

    performGlobalAction(GLOBAL_ACTION_HOME)
    val interceptIntent =
      Intent(this, BlockedInterceptActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra(BlockedInterceptActivity.EXTRA_APP_NAME, match.platformName)
        putExtra(BlockedInterceptActivity.EXTRA_PACKAGE_NAME, pkg)
      }
    startActivity(interceptIntent)

    serviceScope.launch {
      val app = applicationContext as? BetShieldApplication ?: return@launch
      val settings = app.container.preferencesRepository.settingsFlow.first()
      app.container.protectionRepository.recordBlockEvent(
        target = pkg,
        type = BlockEventType.APP,
        category = match.category,
        source = BlockEventSource.ACCESSIBILITY,
      )
      if (settings.notificationsEnabled) {
        ProtectionNotificationHelper.showBlockAlert(
          this@BetShieldAccessibilityService,
          "Betting app blocked",
          "${match.platformName} was closed to protect your streak.",
        )
      }
    }
  }

  override fun onInterrupt() {
    ServiceStatusBridge.setAccessibilityActive(false)
  }

  override fun onDestroy() {
    ServiceStatusBridge.setAccessibilityActive(false)
    serviceScope.cancel()
    super.onDestroy()
  }
}
