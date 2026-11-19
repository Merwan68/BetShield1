package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProtectionGreen
import kotlinx.coroutines.delay

private data class SupportResourceLink(
  val title: String,
  val description: String,
  val url: String,
)

@Composable
fun EmergencySupportScreen(
  coolingOffSeconds: Int,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var remainingSeconds by rememberSaveable(coolingOffSeconds) { mutableIntStateOf(coolingOffSeconds) }
  var timerRunning by rememberSaveable { mutableStateOf(true) }
  var promptIndex by rememberSaveable { mutableIntStateOf(0) }

  val motivationalMessages =
    remember {
      listOf(
        "Urges peak and fade like waves. Give yourself 60 calm seconds before acting.",
        "Every bet you skip protects your earnings, your peace of mind, and your family goals.",
        "The odds are built so the house wins long-term. Keeping your Birr in your pocket is the real win.",
        "Step away from your screen, drink a glass of water, or message a trusted friend.",
      )
    }

  val resources =
    remember {
      listOf(
        SupportResourceLink(
          title = "Gambling Therapy (Free Global Support)",
          description = "Confidential multilingual online support groups and live chat.",
          url = "https://www.gamblingtherapy.org/",
        ),
        SupportResourceLink(
          title = "Gamblers Anonymous",
          description = "Peer recovery fellowship and daily reflection tools.",
          url = "https://www.gamblersanonymous.org/",
        ),
        SupportResourceLink(
          title = "BeGambleAware Help & Guidance",
          description = "Practical strategies for managing urges and self-exclusion.",
          url = "https://www.begambleaware.org/",
        ),
      )
    }

  LaunchedEffect(timerRunning, remainingSeconds) {
    if (timerRunning && remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
    }
  }

  val progress =
    if (coolingOffSeconds > 0) remainingSeconds.toFloat() / coolingOffSeconds.toFloat() else 0f
  val animatedSweep by animateFloatAsState(targetValue = progress * 360f, animationSpec = tween(400), label = "sweep")

  val breathPhase =
    when ((remainingSeconds % 12)) {
      in 8..11 -> "Inhale slowly..."
      in 4..7 -> "Hold gently..."
      else -> "Exhale & release..."
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          IconButton(onClick = onBack, modifier = Modifier.testTag("emergency_back_button")) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
            )
          }
          Text(
            text = "Emergency Pause & Support",
            style = MaterialTheme.typography.headlineSmall,
          )
        }
      }

      // Cooling-off timer card
      item {
        Card(
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF1E3A8A))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text =
                if (remainingSeconds > 0) breathPhase
                else "Cooling-off pause complete. You stayed in control.",
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              textAlign = TextAlign.Center,
            )

            Box(modifier = Modifier.size(156.dp), contentAlignment = Alignment.Center) {
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                  color = Color.White.copy(alpha = 0.14f),
                  style = Stroke(width = 12.dp.toPx()),
                )
                drawArc(
                  color = ProtectionGreen,
                  startAngle = -90f,
                  sweepAngle = animatedSweep,
                  useCenter = false,
                  style = Stroke(width = 12.dp.toPx()),
                )
              }
              Text(
                text = "${remainingSeconds}s",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(
                onClick = { timerRunning = !timerRunning },
                colors = ButtonDefaults.buttonColors(containerColor = ProtectionGreen),
                modifier = Modifier.testTag("emergency_timer_toggle"),
              ) {
                Icon(
                  imageVector = if (timerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (timerRunning) "Pause" else "Resume")
              }
              OutlinedButton(
                onClick = {
                  remainingSeconds = coolingOffSeconds
                  timerRunning = true
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text("Reset")
              }
            }
          }
        }
      }

      // Motivational Grounding Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.SelfImprovement,
                contentDescription = null,
                tint = PrimaryBlue,
              )
              Text(
                text = "Grounding Reminder",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            Text(
              text = motivationalMessages[promptIndex % motivationalMessages.size],
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedButton(
              onClick = { promptIndex = (promptIndex + 1) % motivationalMessages.size },
              modifier = Modifier.testTag("next_motivation_button"),
            ) {
              Text("Another Reminder")
            }
          }
        }
      }

      // Community & Family Guidance (Ethiopia context)
      item {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = null,
              tint = ProtectionGreen,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Reach Out to Someone You Trust",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text =
                  "Talking honestly with a family member, close friend, or local community counselor in Ethiopia breaks the cycle of secrecy that feeds impulsive betting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        Text(
          text = "External Support Resources",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      items(resources) { resource ->
        Card(
          modifier =
            Modifier.fillMaxWidth().clickable {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.url)))
              }
            },
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(text = resource.title, style = MaterialTheme.typography.titleMedium)
              Text(
                text = resource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = "Open external resource",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}
