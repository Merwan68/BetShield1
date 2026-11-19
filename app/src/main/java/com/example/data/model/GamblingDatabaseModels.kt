package com.example.data.model

import com.squareup.moshi.JsonClass

enum class PlatformCategory(val storageKey: String) {
  ETHIOPIAN_BETTING("ETHIOPIAN_BETTING"),
  INTERNATIONAL_SPORTSBOOK("INTERNATIONAL_SPORTSBOOK"),
  ONLINE_CASINO("ONLINE_CASINO"),
  GAMBLING_APP("GAMBLING_APP");

  companion object {
    fun fromKey(key: String): PlatformCategory {
      return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) }
        ?: INTERNATIONAL_SPORTSBOOK
    }
  }
}

@JsonClass(generateAdapter = true)
data class GamblingDatabasePayload(
  val version: String,
  val lastUpdated: String,
  val source: String,
  val platforms: List<GamblingPlatformEntry>,
)

@JsonClass(generateAdapter = true)
data class GamblingPlatformEntry(
  val id: String,
  val name: String,
  val category: String,
  val region: String,
  val domains: List<String>,
  val domainAliases: List<String> = emptyList(),
  val packageIds: List<String> = emptyList(),
  val verifiedDomain: Boolean = true,
  val verifiedPackage: Boolean = false,
  val verificationNote: String = "",
) {
  val parsedCategory: PlatformCategory
    get() = PlatformCategory.fromKey(category)

  val allDomains: List<String>
    get() = (domains + domainAliases).map { it.trim().lowercase() }.distinct()
}

data class InstalledAppSummary(
  val packageName: String,
  val appName: String,
  val isKnownGamblingPackage: Boolean,
  val isBlockedByUser: Boolean,
  val matchedPlatformName: String? = null,
)
