package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BetShieldTheme
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProtectionGreen

class BlockedInterceptActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"
    val packageNameBlocked = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()

    setContent {
      BetShieldTheme {
        Scaffold { innerPadding ->
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(DeepNavy, Color(0xFF1E293B))))
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(80.dp)
                    .clip(CircleShape)
                    .background(ProtectionGreen.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.Shield,
                  contentDescription = "Blocked by BetShield",
                  tint = ProtectionGreen,
                  modifier = Modifier.size(44.dp),
                )
              }

              Text(
                text = "Blocked by BetShield",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
              )

              Text(
                text =
                  "$appName ($packageNameBlocked) was closed by your voluntary App Guard rule. Pause, breathe, and protect your progress.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(8.dp))

              Button(
                onClick = {
                  val openEmergency =
                    Intent(this@BlockedInterceptActivity, MainActivity::class.java).apply {
                      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                      putExtra(MainActivity.EXTRA_OPEN_EMERGENCY, true)
                    }
                  startActivity(openEmergency)
                  finish()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(52.dp),
              ) {
                Text("Open Emergency Support")
              }

              OutlinedButton(
                onClick = { finish() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("Return Safely")
              }
            }
          }
        }
      }
    }
  }

  companion object {
    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_PACKAGE_NAME = "extra_package_name"
  }
}
