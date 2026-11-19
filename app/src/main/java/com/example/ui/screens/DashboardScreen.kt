package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.service.VpnProtectionState
import com.example.ui.BetShieldUiState
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.DeepNavyElevated
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProtectionGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  state: BetShieldUiState,
  copy: LocalizedCopy,
  onToggleVpn: (Boolean) -> Unit,
  onOpenEmergency: () -> Unit,
  onOpenProtection: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isActive = state.vpnState == VpnProtectionState.ACTIVE
  val isPaused = state.vpnState == VpnProtectionState.PAUSED
  val heroAccent by
    animateColorAsState(
      targetValue =
        when (state.vpnState) {
          VpnProtectionState.ACTIVE -> ProtectionGreen
          VpnProtectionState.PAUSED -> WarningAmber
          else -> PrimaryBlue
        },
      label = "heroAccent",
    )

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = copy.appName,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = copy.tagline,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("dashboard_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = copy.navSettings,
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      }

      item {
        HeroShieldCard(
          isActive = isActive,
          isPaused = isPaused,
          vpnState = state.vpnState,
          statusMessage = state.vpnStatusMessage,
          accessibilityActive = state.accessibilityActive,
          accentColor = heroAccent,
          copy = copy,
          onToggleVpn = { onToggleVpn(!isActive) },
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MetricTile(
              title = copy.blockedWebsitesLabel,
              value = state.activeDomainCount.toString(),
              icon = Icons.Filled.Language,
              tint = PrimaryBlue,
              modifier = Modifier.weight(1f).clickable { onOpenProtection() },
              tag = "metric_blocked_domains",
            )
            MetricTile(
              title = copy.restrictedAppsLabel,
              value = state.activePackageCount.toString(),
              icon = Icons.Filled.Apps,
              tint = ProtectionGreen,
              modifier = Modifier.weight(1f).clickable { onOpenProtection() },
              tag = "metric_restricted_apps",
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MetricTile(
              title = copy.totalBlocksLabel,
              value = state.totalRecordedBlocks.toString(),
              icon = Icons.Filled.Block,
              tint = WarningAmber,
              modifier = Modifier.weight(1f),
              tag = "metric_total_blocks",
            )
            MetricTile(
              title = copy.activeCategoriesLabel,
              value = "${state.settings.enabledCategories.size}/4",
              icon = Icons.Filled.Category,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f).clickable { onOpenProtection() },
              tag = "metric_active_categories",
            )
          }
        }
      }

      item {
        EmergencyBannerCard(
          title = copy.emergencyTitle,
          subtitle = copy.emergencySubtitle,
          actionLabel = copy.emergencyAction,
          onClick = onOpenEmergency,
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = copy.recentActivityTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (state.databasePayload != null) {
            Text(
              text = "DB v${state.databasePayload.version}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      if (state.recentEvents.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = ProtectionGreen,
                modifier = Modifier.size(32.dp),
              )
              Text(
                text = copy.noEventsYet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(state.recentEvents.take(6), key = { it.id }) { event ->
          RecentEventRow(
            target = event.target,
            type = event.type,
            source = event.source,
            timestampMillis = event.timestampMillis,
          )
        }
      }
    }
  }
}

@Composable
private fun HeroShieldCard(
  isActive: Boolean,
  isPaused: Boolean,
  vpnState: VpnProtectionState,
  statusMessage: String?,
  accessibilityActive: Boolean,
  accentColor: Color,
  copy: LocalizedCopy,
  onToggleVpn: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.88f,
      targetValue = 1.18f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "pulseScale",
    )

  Card(
    shape = MaterialTheme.shapes.large,
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.linearGradient(
              colors = listOf(DeepNavy, DeepNavyElevated, accentColor.copy(alpha = 0.72f))
            )
          )
          .padding(20.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              color = accentColor.copy(alpha = 0.24f),
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text =
                  when (vpnState) {
                    VpnProtectionState.ACTIVE -> copy.protectionActive
                    VpnProtectionState.PAUSED -> copy.protectionPaused
                    else -> copy.protectionOff
                  },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              )
            }
            Text(
              text =
                if (isActive) "Local DNS filtering is guarding this device."
                else "Tap below to start local gambling DNS filtering.",
              color = Color.White.copy(alpha = 0.9f),
              style = MaterialTheme.typography.bodyMedium,
            )
          }

          Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val radius = (size.minDimension / 2f) * (if (isActive) pulseScale else 0.92f)
              drawCircle(
                color = accentColor.copy(alpha = if (isActive) 0.30f else 0.16f),
                radius = radius.coerceAtMost(size.minDimension / 2f),
              )
            }
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.9f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Protection shield status",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
          }
        }

        if (!statusMessage.isNullOrBlank()) {
          Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFDE68A),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text =
              if (accessibilityActive) "App Guard: Active"
              else "App Guard: Optional (Not enabled)",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f),
          )
          if (isPaused) {
            Text(
              text = "Paused manually",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFFCD34D),
            )
          }
        }

        Button(
          onClick = onToggleVpn,
          modifier =
            Modifier.fillMaxWidth().height(50.dp).testTag("toggle_protection_button"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (isActive) Color.White.copy(alpha = 0.16f) else ProtectionGreen,
              contentColor = Color.White,
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Icon(
            imageVector = if (isActive) Icons.Filled.PauseCircle else Icons.Filled.PlayArrow,
            contentDescription = null,
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = if (isActive) copy.pauseProtection else copy.activateProtection,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  title: String,
  value: String,
  icon: ImageVector,
  tint: Color,
  tag: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.testTag(tag),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
      }
      Text(
        text = value,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun EmergencyBannerCard(
  title: String,
  subtitle: String,
  actionLabel: String,
  onClick: () -> Unit,
) {
  Card(
    modifier =
      Modifier.fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = onClick)
        .testTag("dashboard_emergency_card"),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
      ),
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Favorite,
          contentDescription = null,
          tint = Color.White,
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
        )
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun RecentEventRow(
  target: String,
  type: String,
  source: String,
  timestampMillis: Long,
) {
  val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.small,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = target,
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$type • $source",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = formatter.format(Date(timestampMillis)),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
