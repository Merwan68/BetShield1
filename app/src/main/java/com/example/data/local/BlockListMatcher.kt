package com.example.data.local

import com.example.data.model.CustomRuleEntity
import com.example.data.model.CustomRuleType
import com.example.data.model.GamblingPlatformEntry
import com.example.data.model.PlatformCategory

data class BlockMatchResult(
  val matchedTarget: String,
  val category: String,
  val platformName: String,
  val isCustomRule: Boolean,
)

class BlockListMatcher(
  platforms: List<GamblingPlatformEntry>,
  enabledCategories: Set<PlatformCategory>,
  disabledPlatformIds: Set<String>,
  customRules: List<CustomRuleEntity>,
) {
  private val domainToPlatform: Map<String, BlockMatchResult>
  private val packageToPlatform: Map<String, BlockMatchResult>

  val activeDomainCount: Int
    get() = domainToPlatform.size

  val activePackageCount: Int
    get() = packageToPlatform.size

  init {
    val domains = HashMap<String, BlockMatchResult>()
    val packages = HashMap<String, BlockMatchResult>()

    for (platform in platforms) {
      if (platform.parsedCategory !in enabledCategories) continue
      if (platform.id in disabledPlatformIds) continue

      for (rawDomain in platform.allDomains) {
        val normalized = normalizeDomain(rawDomain) ?: continue
        domains[normalized] =
          BlockMatchResult(
            matchedTarget = normalized,
            category = platform.category,
            platformName = platform.name,
            isCustomRule = false,
          )
      }

      for (pkg in platform.packageIds) {
        val normalizedPkg = pkg.trim()
        if (normalizedPkg.isNotEmpty()) {
          packages[normalizedPkg] =
            BlockMatchResult(
              matchedTarget = normalizedPkg,
              category = platform.category,
              platformName = platform.name,
              isCustomRule = false,
            )
        }
      }
    }

    for (rule in customRules) {
      if (!rule.enabled) continue
      when (rule.ruleType) {
        CustomRuleType.DOMAIN.key -> {
          val normalized = normalizeDomain(rule.value) ?: continue
          domains[normalized] =
            BlockMatchResult(
              matchedTarget = normalized,
              category = "CUSTOM_DOMAIN",
              platformName = rule.label.ifBlank { normalized },
              isCustomRule = true,
            )
        }
        CustomRuleType.APP.key -> {
          val normalizedPkg = rule.value.trim()
          if (normalizedPkg.isNotEmpty()) {
            packages[normalizedPkg] =
              BlockMatchResult(
                matchedTarget = normalizedPkg,
                category = "CUSTOM_APP",
                platformName = rule.label.ifBlank { normalizedPkg },
                isCustomRule = true,
              )
          }
        }
      }
    }

    domainToPlatform = domains
    packageToPlatform = packages
  }

  /**
   * Matches exact domain or any parent domain (e.g. "sub.m.betika.et" matches "betika.et").
   */
  fun matchDomain(queryHost: String): BlockMatchResult? {
    val normalized = normalizeDomain(queryHost) ?: return null
    var candidate = normalized
    while (true) {
      domainToPlatform[candidate]?.let { return it }
      val dotIndex = candidate.indexOf('.')
      if (dotIndex < 0 || dotIndex == candidate.lastIndex) break
      candidate = candidate.substring(dotIndex + 1)
    }
    return null
  }

  fun matchPackage(packageName: String): BlockMatchResult? {
    return packageToPlatform[packageName.trim()]
  }

  companion object {
    private val DOMAIN_REGEX =
      Regex("^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$")

    fun normalizeDomain(input: String): String? {
      var cleaned = input.trim().lowercase()
      if (cleaned.isEmpty()) return null
      cleaned = cleaned.removePrefix("https://").removePrefix("http://")
      val slashIdx = cleaned.indexOf('/')
      if (slashIdx >= 0) cleaned = cleaned.substring(0, slashIdx)
      val portIdx = cleaned.indexOf(':')
      if (portIdx >= 0) cleaned = cleaned.substring(0, portIdx)
      cleaned = cleaned.trimEnd('.')
      if (cleaned.startsWith("*.")) cleaned = cleaned.removePrefix("*.")
      return if (DOMAIN_REGEX.matches(cleaned)) cleaned else null
    }
  }
}
