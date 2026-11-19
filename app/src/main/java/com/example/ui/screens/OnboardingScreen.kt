package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.i18n.LocalizedCopy
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProtectionGreen
import kotlinx.coroutines.launch

private data class OnboardingPageData(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val accentColor: Color,
  val bullets: List<String>,
)

@Composable
fun OnboardingScreen(
  copy: LocalizedCopy,
  onFinish: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pages =
    listOf(
      OnboardingPageData(
        title = "Take Back Control. Block the Bet.",
        subtitle =
          "BetShield is a voluntary digital-wellbeing shield designed for Ethiopia and international betting platforms.",
        icon = Icons.Filled.Shield,
        accentColor = ProtectionGreen,
        bullets =
          listOf(
            "Filters Ethiopian & global sportsbooks and online casinos offline.",
            "Lets you pause during impulsive urges with guided cooling-off support.",
            "Runs locally on your phone with zero accounts or cloud tracking.",
          ),
      ),
      OnboardingPageData(
        title = "How Filtering & App Guard Work",
        subtitle =
          "Two transparent Android layers work together only when you choose to enable them.",
        icon = Icons.Filled.Dns,
        accentColor = PrimaryBlue,
        bullets =
          listOf(
            "Website Shield uses Android VpnService to filter only local DNS lookups (UDP port 53).",
            "App Guard uses optional Accessibility window-change events to close selected betting apps.",
            "You choose every category, website, and installed app on your blocklist.",
          ),
      ),
      OnboardingPageData(
        title = "Privacy by Design",
        subtitle = "Your recovery belongs strictly to you. Nothing leaves your device.",
        icon = Icons.Filled.Lock,
        accentColor = ProtectionGreen,
        bullets =
          listOf(
            "Never intercepts HTTPS traffic or installs TLS certificates.",
            "Never reads screen text, passwords, SMS, or mobile-money PINs.",
            "Stores blocking counts and custom rules locally in encrypted app storage.",
          ),
      ),
      OnboardingPageData(
        title = "Honest Technical Limitations",
        subtitle =
          "Understanding how Android works helps you build stronger real-world safeguards.",
        icon = Icons.Filled.Info,
        accentColor = Color(0xFFD97706),
        bullets =
          listOf(
            "Private DNS (DoT) or browser DNS-over-HTTPS (DoH) can bypass local VPN DNS filtering.",
            "Android permits only one active VPN app at a time.",
            "Only verified package IDs or apps you select are blocked; unverified entries are labeled.",
          ),
      ),
    )

  val pagerState = rememberPagerState(pageCount = { pages.size })
  val coroutineScope = rememberCoroutineScope()

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = copy.appName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          TextButton(
            onClick = onFinish,
            modifier = Modifier.testTag("onboarding_skip_button"),
          ) {
            Text(copy.onboardingSkip)
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { pageIndex ->
          val page = pages[pageIndex]
          Column(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            if (pageIndex == 0) {
              Image(
                painter = painterResource(id = R.drawable.img_shield_hero),
                contentDescription = "BetShield digital wellbeing shield illustration",
                modifier =
                  Modifier.fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop,
              )
            } else {
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                      Brush.linearGradient(
                        listOf(DeepNavy, page.accentColor.copy(alpha = 0.85f))
                      )
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = page.icon,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(48.dp),
                )
              }
            }

            Text(
              text = page.title,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
              text = page.subtitle,
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
              colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = MaterialTheme.shapes.medium,
              elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                page.bullets.forEach { bullet ->
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                  ) {
                    Box(
                      modifier =
                        Modifier.padding(top = 6.dp)
                          .size(8.dp)
                          .clip(CircleShape)
                          .background(page.accentColor)
                    )
                    Text(
                      text = bullet,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat(pages.size) { index ->
              val selected = pagerState.currentPage == index
              Box(
                modifier =
                  Modifier.padding(horizontal = 4.dp)
                    .height(8.dp)
                    .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline
                    )
              )
            }
          }

          val isLast = pagerState.currentPage == pages.lastIndex
          Button(
            onClick = {
              if (isLast) {
                onFinish()
              } else {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
              }
            },
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .testTag(if (isLast) "onboarding_start_button" else "onboarding_next_button"),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isLast) ProtectionGreen else PrimaryBlue,
                contentColor = Color.White,
              ),
            shape = MaterialTheme.shapes.medium,
          ) {
            Text(
              text = if (isLast) copy.onboardingStart else copy.onboardingNext,
              style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}
