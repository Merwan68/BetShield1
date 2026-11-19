package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockEventEntity
import com.example.ui.BetShieldUiState
import com.example.ui.StatBarBucket
import com.example.ui.StatsTimeframe
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProtectionGreen

@Composable
fun StatisticsScreen(
  state: BetShieldUiState,
  copy: LocalizedCopy,
  computeBuckets: (List<BlockEventEntity>, StatsTimeframe) -> List<StatBarBucket>,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var timeframe by rememberSaveable { mutableStateOf(StatsTimeframe.WEEKLY) }
  var showClearDialog by remember { mutableStateOf(false) }

  val buckets =
    remember(state.recentEvents, timeframe) { computeBuckets(state.recentEvents, timeframe) }
  val periodTotal = remember(buckets) { buckets.sumOf { it.count } }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text("Clear blocking history?") },
      text = {
        Text(
          "This removes locally recorded blocking events from this device. Your custom domains and app rules stay intact."
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onClearHistory()
            showClearDialog = false
          }
        ) {
          Text("Clear", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = copy.statisticsTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (state.recentEvents.isNotEmpty()) {
            TextButton(
              onClick = { showClearDialog = true },
              modifier = Modifier.testTag("clear_stats_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(4.dp))
              Text(copy.clearHistory)
            }
          }
        }
      }

      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          val options =
            listOf(
              StatsTimeframe.DAILY to copy.dailyLabel,
              StatsTimeframe.WEEKLY to copy.weeklyLabel,
              StatsTimeframe.MONTHLY to copy.monthlyLabel,
            )
          options.forEachIndexed { idx, (tf, label) ->
            SegmentedButton(
              selected = timeframe == tf,
              onClick = { timeframe = tf },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = options.size),
              modifier = Modifier.testTag("stats_tab_${tf.name.lowercase()}"),
            ) {
              Text(label)
            }
          }
        }
      }

      // Bar Chart Card
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
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom,
            ) {
              Column {
                Text(
                  text = "Period Events",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = periodTotal.toString(),
                  fontFamily = JetBrainsMonoFamily,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.displaySmall,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "DNS Session",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${state.sessionBlockedQueries} blocked / ${state.sessionForwardedQueries} allowed",
                  style = MaterialTheme.typography.labelSmall,
                  color = ProtectionGreen,
                )
              }
            }

            StatsBarChart(buckets = buckets, modifier = Modifier.fillMaxWidth().height(150.dp))
          }
        }
      }

      // Honest measurement disclaimer
      item {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Filled.Info,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = copy.statsHonestyNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Top Blocked Targets
      item {
        Text(
          text = copy.topBlockedTitle,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (state.topBlockedTargets.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(20.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = "No blocked domains or apps recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(state.topBlockedTargets, key = { "${it.type}_${it.target}" }) { item ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.small,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.target,
                  fontFamily = JetBrainsMonoFamily,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = item.type,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = "${item.count}×",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlue,
              )
            }
          }
        }
      }

      if (state.recentEvents.isNotEmpty()) {
        item {
          Text(
            text = "Protection Activity History (${state.recentEvents.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }
        items(state.recentEvents.take(40), key = { it.id }) { event ->
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
private fun StatsBarChart(
  buckets: List<StatBarBucket>,
  modifier: Modifier = Modifier,
) {
  val maxCount = (buckets.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
  val barColor = PrimaryBlue
  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
      if (buckets.isEmpty()) return@Canvas
      val count = buckets.size
      val slotWidth = size.width / count
      val barWidth = (slotWidth * 0.48f).coerceAtMost(44.dp.toPx())
      val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

      buckets.forEachIndexed { index, bucket ->
        val left = index * slotWidth + (slotWidth - barWidth) / 2f
        drawRoundRect(
          color = trackColor,
          topLeft = Offset(left, 0f),
          size = Size(barWidth, size.height),
          cornerRadius = corner,
        )
        if (bucket.count > 0) {
          val ratio = bucket.count.toFloat() / maxCount.toFloat()
          val barHeight = (size.height * ratio).coerceAtLeast(8.dp.toPx())
          drawRoundRect(
            color = barColor,
            topLeft = Offset(left, size.height - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = corner,
          )
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
      buckets.forEach { bucket ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = bucket.count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = bucket.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
