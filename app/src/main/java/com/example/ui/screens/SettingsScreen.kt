package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode
import com.example.data.repository.DatabaseSyncState
import com.example.ui.BetShieldUiState
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.JetBrainsMonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  state: BetShieldUiState,
  copy: LocalizedCopy,
  onThemeChange: (AppThemeMode) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onNotificationsChange: (Boolean) -> Unit,
  onCoolingOffChange: (Int) -> Unit,
  onUpdateDatabase: (String) -> Unit,
  onSaveRemoteUrl: (String) -> Unit,
  onResetBundledDatabase: () -> Unit,
  onRevisitOnboarding: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var remoteUrl by rememberSaveable(state.settings.remoteDatabaseUrl) {
    mutableStateOf(state.settings.remoteDatabaseUrl)
  }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      onNotificationsChange(granted)
    }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text(copy.privacyLimitationsTitle) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "• Zero HTTPS Interception: BetShield only inspects local UDP port 53 DNS requests inside VpnService. It never installs root certificates or decrypts web pages.",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• Zero Surveillance: App Guard listens solely for foreground package switches and disables content retrieval (canRetrieveWindowContent=false).",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• DNS Limitations: System Private DNS (DNS-over-TLS), browser DNS-over-HTTPS (DoH), or another active VPN can bypass local DNS filtering. Disable Private DNS in Android Settings for reliable web filtering.",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "• Local-Only Storage: No passwords, banking details, or gambling credentials are ever collected or transmitted.",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showPrivacyDialog = false }) { Text("Got It") }
      },
    )
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text = copy.settingsTitle,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // Theme & Language
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(text = copy.themeLabel, style = MaterialTheme.typography.titleMedium)
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              AppThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = state.settings.themeMode == mode,
                  onClick = { onThemeChange(mode) },
                  shape = SegmentedButtonDefaults.itemShape(index = idx, count = AppThemeMode.entries.size),
                  modifier = Modifier.testTag("theme_${mode.key.lowercase()}"),
                ) {
                  Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
              }
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(text = copy.languageLabel, style = MaterialTheme.typography.titleMedium)
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              AppLanguage.entries.forEachIndexed { idx, lang ->
                SegmentedButton(
                  selected = state.settings.language == lang,
                  onClick = { onLanguageChange(lang) },
                  shape = SegmentedButtonDefaults.itemShape(index = idx, count = AppLanguage.entries.size),
                  modifier = Modifier.testTag("lang_${lang.code}"),
                ) {
                  Text(lang.displayName)
                }
              }
            }
          }
        }
      }

      // Notifications & Cooling-off duration
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                  Text(text = copy.notificationsLabel, style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = "Alert when a site or app is blocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = state.settings.notificationsEnabled,
                onCheckedChange = { enable ->
                  if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted =
                      ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                      ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                      return@Switch
                    }
                  }
                  onNotificationsChange(enable)
                },
                modifier = Modifier.testTag("notifications_switch"),
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(text = "Cooling-Off Pause Duration", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(30, 60, 120, 180).forEach { seconds ->
                FilterChip(
                  selected = state.settings.coolingOffSeconds == seconds,
                  onClick = { onCoolingOffChange(seconds) },
                  label = { Text("${seconds}s") },
                )
              }
            }
          }
        }
      }

      // Database Update Settings
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(text = copy.databaseSection, style = MaterialTheme.typography.titleMedium)
            }

            val payload = state.databasePayload
            if (payload != null) {
              val verifiedCount = payload.platforms.count { it.verifiedDomain }
              Text(
                text =
                  "Version ${payload.version} • ${payload.platforms.size} platforms ($verifiedCount domain-verified) • Updated ${payload.lastUpdated.take(10)}",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (state.settings.lastRemoteSyncMillis > 0L) {
              val formatted =
                SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                  .format(Date(state.settings.lastRemoteSyncMillis))
              Text(
                text = "Last remote sync attempt: $formatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            OutlinedTextField(
              value = remoteUrl,
              onValueChange = { remoteUrl = it },
              label = { Text("HTTPS Registry Update URL") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  onSaveRemoteUrl(remoteUrl)
                  onUpdateDatabase(remoteUrl)
                },
                enabled = state.syncState !is DatabaseSyncState.Updating,
                modifier = Modifier.weight(1f).testTag("update_database_button"),
              ) {
                if (state.syncState is DatabaseSyncState.Updating) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                } else {
                  Text(copy.updateDatabaseNow)
                }
              }
              OutlinedButton(onClick = onResetBundledDatabase) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Reset")
              }
            }
          }
        }
      }

      // Privacy, Limitations & About
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(text = copy.privacyLimitationsTitle, style = MaterialTheme.typography.titleMedium)
            }
            Text(
              text =
                "BetShield operates locally without root, HTTPS MITM, or analytics trackers. Review technical limits and permissions anytime.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { showPrivacyDialog = true },
                modifier = Modifier.testTag("privacy_limits_button"),
              ) {
                Text("View Details")
              }
              OutlinedButton(onClick = onRevisitOnboarding) {
                Text("View Onboarding")
              }
            }
          }
        }
      }

      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = MaterialTheme.shapes.medium,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text(
                text = "About BetShield v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Take Back Control. Block the Bet. • MIT Open Source • Ethiopia & International Support",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
