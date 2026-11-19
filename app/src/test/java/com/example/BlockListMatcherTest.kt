package com.example

import com.example.data.local.BlockListMatcher
import com.example.data.model.CustomRuleEntity
import com.example.data.model.CustomRuleType
import com.example.data.model.GamblingPlatformEntry
import com.example.data.model.PlatformCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BlockListMatcherTest {

  private val samplePlatforms =
    listOf(
      GamblingPlatformEntry(
        id = "et_betika",
        name = "Betika Ethiopia",
        category = PlatformCategory.ETHIOPIAN_BETTING.storageKey,
        region = "ET",
        domains = listOf("betika.et"),
        domainAliases = listOf("betika.com"),
        packageIds = emptyList(),
        verifiedDomain = true,
        verifiedPackage = false,
      ),
      GamblingPlatformEntry(
        id = "intl_1xbet",
        name = "1xBet",
        category = PlatformCategory.INTERNATIONAL_SPORTSBOOK.storageKey,
        region = "INTL",
        domains = listOf("1xbet.com"),
        packageIds = listOf("org.xbet.client1"),
        verifiedDomain = true,
        verifiedPackage = true,
      ),
    )

  @Test
  fun `matches exact domain and subdomains`() {
    val matcher =
      BlockListMatcher(
        platforms = samplePlatforms,
        enabledCategories = PlatformCategory.entries.toSet(),
        disabledPlatformIds = emptySet(),
        customRules = emptyList(),
      )

    assertNotNull(matcher.matchDomain("betika.et"))
    assertNotNull(matcher.matchDomain("m.betika.et"))
    assertNotNull(matcher.matchDomain("api.mobile.betika.com."))
    assertNull(matcher.matchDomain("wikipedia.org"))
  }

  @Test
  fun `respects category toggles and disabled platforms`() {
    val matcher =
      BlockListMatcher(
        platforms = samplePlatforms,
        enabledCategories = setOf(PlatformCategory.INTERNATIONAL_SPORTSBOOK),
        disabledPlatformIds = setOf("intl_1xbet"),
        customRules = emptyList(),
      )

    assertNull(matcher.matchDomain("betika.et"))
    assertNull(matcher.matchDomain("1xbet.com"))
    assertNull(matcher.matchPackage("org.xbet.client1"))
  }

  @Test
  fun `supports custom domain and custom package rules`() {
    val customRules =
      listOf(
        CustomRuleEntity(
          id = 1,
          value = "https://custombet.et/sports",
          label = "Custom Bet",
          ruleType = CustomRuleType.DOMAIN.key,
          enabled = true,
        ),
        CustomRuleEntity(
          id = 2,
          value = "com.custom.betting",
          label = "Custom App",
          ruleType = CustomRuleType.APP.key,
          enabled = true,
        ),
      )

    val matcher =
      BlockListMatcher(
        platforms = samplePlatforms,
        enabledCategories = PlatformCategory.entries.toSet(),
        disabledPlatformIds = emptySet(),
        customRules = customRules,
      )

    val domainMatch = matcher.matchDomain("m.custombet.et")
    assertNotNull(domainMatch)
    assertEquals("custombet.et", domainMatch?.matchedTarget)

    val appMatch = matcher.matchPackage("com.custom.betting")
    assertNotNull(appMatch)
    assertEquals("Custom App", appMatch?.platformName)
  }

  @Test
  fun `rejects invalid domains during normalization`() {
    assertNull(BlockListMatcher.normalizeDomain(""))
    assertNull(BlockListMatcher.normalizeDomain("not a domain"))
    assertNull(BlockListMatcher.normalizeDomain("localhost"))
    assertEquals("betika.et", BlockListMatcher.normalizeDomain("https://BETIKA.ET/live"))
  }
}
