package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GamblingDatabaseLoader
import com.example.ui.i18n.AppStrings
import com.example.data.local.AppLanguage
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.BetShieldTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app name matches BetShield`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BetShield", appName)
  }

  @Test
  fun `bundled offline gambling database loads and contains Ethiopian platforms`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val loader = GamblingDatabaseLoader(context)
    val payload = loader.loadBundledDatabase()
    assertEquals("1.0.0", payload.version)
    assertTrue(payload.platforms.size >= 20)
    assertTrue(payload.platforms.any { it.region == "ET" && it.allDomains.contains("betika.et") })
  }

  @Test
  fun `onboarding screen renders and advances cleanly`() {
    var finished = false
    val copy = AppStrings.forLanguage(AppLanguage.ENGLISH)
    composeTestRule.setContent {
      BetShieldTheme {
        OnboardingScreen(copy = copy, onFinish = { finished = true })
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Take Back Control. Block the Bet.").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(finished)
  }
}
