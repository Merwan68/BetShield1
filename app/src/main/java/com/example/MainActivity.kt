package com.example

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.BetShieldAccessibilityService
import com.example.service.BetShieldVpnService
import com.example.service.ServiceStatusBridge
import com.example.ui.BetShieldViewModel
import com.example.ui.i18n.AppStrings
import com.example.ui.navigation.BetShieldAppShell
import com.example.ui.theme.BetShieldTheme

class MainActivity : ComponentActivity() {

  private val viewModel: BetShieldViewModel by viewModels {
    BetShieldViewModel.Factory((application as BetShieldApplication).container.protectionRepository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val openEmergencyInitially = intent?.getBooleanExtra(EXTRA_OPEN_EMERGENCY, false) == true

    setContent {
      val context = LocalContext.current
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val domainTestResult by viewModel.domainTestResult.collectAsStateWithLifecycle()
      val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

      val vpnPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
          if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
            viewModel.setProtectionDesired(true)
          } else {
            viewModel.setProtectionDesired(false)
          }
        }

      LaunchedEffect(Unit) {
        ServiceStatusBridge.setAccessibilityActive(isAccessibilityServiceEnabled())
      }

      val copy = AppStrings.forLanguage(uiState.settings.language)

      BetShieldTheme(themeMode = uiState.settings.themeMode) {
        BetShieldAppShell(
          state = uiState,
          copy = copy,
          viewModel = viewModel,
          domainTestResult = domainTestResult,
          feedbackMessage = feedbackMessage,
          openEmergencyInitially = openEmergencyInitially,
          onToggleVpn = { enable ->
            if (enable) {
              val prepareIntent = VpnService.prepare(context)
              if (prepareIntent != null) {
                vpnPermissionLauncher.launch(prepareIntent)
              } else {
                startVpnService()
                viewModel.setProtectionDesired(true)
              }
            } else {
              stopVpnService()
              viewModel.setProtectionDesired(false)
            }
          },
          onOpenAccessibilitySettings = {
            runCatching {
              context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              )
            }
          },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    ServiceStatusBridge.setAccessibilityActive(isAccessibilityServiceEnabled())
    viewModel.refreshInstalledApps()
  }

  private fun startVpnService() {
    val intent =
      Intent(this, BetShieldVpnService::class.java).apply {
        action = BetShieldVpnService.ACTION_START
      }
    ContextCompat.startForegroundService(this, intent)
  }

  private fun stopVpnService() {
    val intent =
      Intent(this, BetShieldVpnService::class.java).apply {
        action = BetShieldVpnService.ACTION_STOP
      }
    startService(intent)
  }

  private fun isAccessibilityServiceEnabled(): Boolean {
    val expected = ComponentName(this, BetShieldAccessibilityService::class.java).flattenToString()
    val enabledServices =
      Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
      if (splitter.next().equals(expected, ignoreCase = true)) {
        return true
      }
    }
    return false
  }

  companion object {
    const val EXTRA_OPEN_EMERGENCY = "extra_open_emergency"
  }
}
