package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BlockMatchResult
import com.example.data.model.CustomRuleType
import com.example.data.model.GamblingPlatformEntry
import com.example.data.model.InstalledAppSummary
import com.example.data.model.PlatformCategory
import com.example.service.VpnProtectionState
import com.example.ui.BetShieldUiState
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ProtectionGreen
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProtectionCenterScreen(
  state: BetShieldUiState,
  copy: LocalizedCopy,
  domainTestResult: Pair<String, BlockMatchResult?>?,
  onToggleVpn: (Boolean) -> Unit,
  onOpenAccessibilitySettings: () -> Unit,
  onToggleCategory: (PlatformCategory, Boolean) -> Unit,
  onSetPlatformEnabled: (String, Boolean) -> Unit,
  onAddCustomDomain: (String) -> Unit,
  onTestDomain: (String) -> Unit,
  onToggleCustomRule: (Long, Boolean) -> Unit,
  onDeleteCustomRule: (Long) -> Unit,
  onToggleInstalledApp: (InstalledAppSummary, Boolean) -> Unit,
  onAddManualPackage: (String, String) -> Unit,
  onRefreshApps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable { mutableIntStateOf(0) }
  var domainInput by rememberSaveable { mutableStateOf("") }
  var testDomainInput by rememberSaveable { mutableStateOf("") }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var manualPackageInput by rememberSaveable { mutableStateOf("") }
  var manualAppLabel by rememberSaveable { mutableStateOf("") }

  val isVpnActive = state.vpnState == VpnProtectionState.ACTIVE
  val platforms = state.databasePayload?.platforms.orEmpty()
  val customDomainRules =
    remember(state.customRules) {
      state.customRules.filter { it.ruleType == CustomRuleType.DOMAIN.key }
    }
  val customAppRules =
    remember(state.customRules) {
      state.customRules.filter { it.ruleType == CustomRuleType.APP.key }
    }

  val filteredPlatforms =
    remember(platforms, searchQuery) {
      if (searchQuery.isBlank()) platforms
      else {
        val q = searchQuery.trim().lowercase()
        platforms.filter { entry ->
          entry.name.lowercase().contains(q) || entry.allDomains.any { it.contains(q) }
        }
      }
    }

  val filteredInstalledApps =
    remember(state.installedApps, searchQuery) {
      if (searchQuery.isBlank()) state.installedApps
      else {
        val q = searchQuery.trim().lowercase()
        state.installedApps.filter {
          it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text = copy.protectionCenterTitle,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // Protection Controls Summary
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Dns,
                  contentDescription = null,
                  tint = if (isVpnActive) ProtectionGreen else MaterialTheme.colorScheme.primary,
                )
                Column {
                  Text(
                    text = "Website DNS Shield",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${state.activeDomainCount} domains active • Local VPN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Switch(
                checked = isVpnActive,
                onCheckedChange = onToggleVpn,
                modifier = Modifier.testTag("protection_vpn_switch"),
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Security,
                  contentDescription = null,
                  tint =
                    if (state.accessibilityActive) ProtectionGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                  Text(
                    text = copy.appGuardTitle,
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text =
                      if (state.accessibilityActive) copy.appGuardEnabled
                      else copy.appGuardDisabled,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              OutlinedButton(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.testTag("open_accessibility_button"),
              ) {
                Text(if (state.accessibilityActive) "Active" else "Setup")
              }
            }
          }
        }
      }

      // Category Chips
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = copy.categoriesSection,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PlatformCategory.entries.forEach { category ->
              val selected = category in state.settings.enabledCategories
              FilterChip(
                selected = selected,
                onClick = { onToggleCategory(category, !selected) },
                label = { Text(categoryDisplayName(category)) },
                modifier = Modifier.testTag("category_chip_${category.storageKey}"),
              )
            }
          }
        }
      }

      // Segmented Switch: Websites vs Apps
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("tab_websites"),
          ) {
            Text(copy.websitesTab, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          SegmentedButton(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("tab_apps"),
          ) {
            Text(copy.appsTab, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      // Search box
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.fillMaxWidth().testTag("protection_search_input"),
          placeholder = {
            Text(
              if (selectedTab == 0) "Search Ethiopian or global domains..."
              else "Search installed apps or package IDs..."
            )
          },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
        )
      }

      if (selectedTab == 0) {
        // Add Custom Domain Card
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(text = copy.addCustomDomain, style = MaterialTheme.typography.titleMedium)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = domainInput,
                  onValueChange = { domainInput = it },
                  placeholder = { Text(copy.domainHint) },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("custom_domain_input"),
                )
                Button(
                  onClick = {
                    onAddCustomDomain(domainInput)
                    domainInput = ""
                  },
                  modifier = Modifier.height(54.dp).testTag("add_custom_domain_button"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null)
                  Spacer(modifier = Modifier.size(4.dp))
                  Text(copy.addButton)
                }
              }
            }
          }
        }

        // Domain Filter Verifier Card
        item {
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              ),
            shape = MaterialTheme.shapes.medium,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(text = copy.testFilterTitle, style = MaterialTheme.typography.titleMedium)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = testDomainInput,
                  onValueChange = { testDomainInput = it },
                  placeholder = { Text(copy.testFilterHint) },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("test_domain_input"),
                )
                OutlinedButton(
                  onClick = { onTestDomain(testDomainInput) },
                  modifier = Modifier.height(54.dp).testTag("test_domain_button"),
                ) {
                  Text(copy.testButton)
                }
              }
              if (domainTestResult != null) {
                val (testedHost, match) = domainTestResult
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector =
                      if (match != null) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (match != null) ProtectionGreen else WarningAmber,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text =
                      if (match != null)
                        "BLOCKED: $testedHost matched ${match.platformName} (${match.matchedTarget})"
                      else "NOT BLOCKED: $testedHost is not in active rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }

        // Custom domain rules list
        if (customDomainRules.isNotEmpty()) {
          item {
            Text(
              text = "Custom Blocked Domains (${customDomainRules.size})",
              style = MaterialTheme.typography.titleMedium,
            )
          }
          items(customDomainRules, key = { "rule_${it.id}" }) { rule ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.small,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = rule.value,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Custom domain rule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = rule.enabled,
                  onCheckedChange = { onToggleCustomRule(rule.id, it) },
                )
                IconButton(onClick = { onDeleteCustomRule(rule.id) }) {
                  Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete custom domain",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }

        // Gambling Database Platforms
        item {
          Text(
            text = "Registry Platforms (${filteredPlatforms.size})",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        items(filteredPlatforms, key = { it.id }) { platform ->
          PlatformDomainCard(
            platform = platform,
            categoryEnabled = platform.parsedCategory in state.settings.enabledCategories,
            platformEnabled = platform.id !in state.settings.disabledPlatformIds,
            onTogglePlatform = { enabled -> onSetPlatformEnabled(platform.id, enabled) },
          )
        }
      } else {
        // Apps Tab: Manual package entry + Installed apps discovery
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "Block App by Package ID",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text =
                  "For sideloaded APKs not listed below, enter the exact Android package ID. BetShield never invents unverified package names.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedTextField(
                value = manualPackageInput,
                onValueChange = { manualPackageInput = it },
                placeholder = { Text("e.g. com.example.bettingapp") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("manual_package_input"),
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedTextField(
                  value = manualAppLabel,
                  onValueChange = { manualAppLabel = it },
                  placeholder = { Text("Display label (optional)") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                Button(
                  onClick = {
                    onAddManualPackage(manualPackageInput, manualAppLabel)
                    manualPackageInput = ""
                    manualAppLabel = ""
                  },
                  modifier = Modifier.height(54.dp).testTag("add_manual_package_button"),
                ) {
                  Text("Add App")
                }
              }
            }
          }
        }

        if (customAppRules.isNotEmpty()) {
          item {
            Text(
              text = "Selected & Custom Blocked Apps (${customAppRules.size})",
              style = MaterialTheme.typography.titleMedium,
            )
          }
          items(customAppRules, key = { "app_rule_${it.id}" }) { rule ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.small,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = rule.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  Text(
                    text = rule.value,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = rule.enabled,
                  onCheckedChange = { onToggleCustomRule(rule.id, it) },
                )
                IconButton(onClick = { onDeleteCustomRule(rule.id) }) {
                  Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove blocked app",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Installed Launcher Apps (${filteredInstalledApps.size})",
              style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onRefreshApps) {
              Icon(Icons.Filled.Refresh, contentDescription = "Refresh installed apps")
            }
          }
        }

        items(filteredInstalledApps, key = { it.packageName }) { app ->
          InstalledAppRow(
            app = app,
            onToggle = { block -> onToggleInstalledApp(app, block) },
          )
        }
      }
    }
  }
}

@Composable
private fun PlatformDomainCard(
  platform: GamblingPlatformEntry,
  categoryEnabled: Boolean,
  platformEnabled: Boolean,
  onTogglePlatform: (Boolean) -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = platform.name,
              style = MaterialTheme.typography.titleMedium,
            )
            Surface(
              color =
                if (platform.region == "ET") ProtectionGreen.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small,
            ) {
              Text(
                text = platform.region,
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (platform.region == "ET") ProtectionGreen
                  else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
          Text(
            text = platform.allDomains.take(4).joinToString(" • "),
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = platformEnabled && categoryEnabled,
          enabled = categoryEnabled,
          onCheckedChange = onTogglePlatform,
        )
      }

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector =
            if (platform.verifiedDomain) Icons.Filled.Verified else Icons.Filled.Warning,
          contentDescription = null,
          tint = if (platform.verifiedDomain) ProtectionGreen else WarningAmber,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text =
            buildString {
              append(if (platform.verifiedDomain) "Verified domain" else "Unverified domain")
              append(" • ")
              append(
                if (platform.verifiedPackage && platform.packageIds.isNotEmpty())
                  "Verified app ID (${platform.packageIds.first()})"
                else "App package unverified"
              )
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun InstalledAppRow(
  app: InstalledAppSummary,
  onToggle: (Boolean) -> Unit,
) {
  val checked = app.isBlockedByUser || app.isKnownGamblingPackage
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = app.appName,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = app.packageName,
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (app.matchedPlatformName != null) {
          Text(
            text = "Matched verified gambling registry: ${app.matchedPlatformName}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFDC2626),
          )
        }
      }
      Switch(
        checked = checked,
        enabled = !app.isKnownGamblingPackage,
        onCheckedChange = onToggle,
      )
    }
  }
}

fun categoryDisplayName(category: PlatformCategory): String =
  when (category) {
    PlatformCategory.ETHIOPIAN_BETTING -> "Ethiopian Betting"
    PlatformCategory.INTERNATIONAL_SPORTSBOOK -> "International Sportsbooks"
    PlatformCategory.ONLINE_CASINO -> "Online Casinos"
    PlatformCategory.GAMBLING_APP -> "Gambling Apps"
  }
