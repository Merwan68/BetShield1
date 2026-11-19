package com.example.data.local

import android.content.Context
import com.example.data.model.GamblingDatabasePayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GamblingDatabaseLoader(private val context: Context) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val adapter = moshi.adapter(GamblingDatabasePayload::class.java)

  private val overrideFile: File
    get() = File(context.filesDir, OVERRIDE_FILE_NAME)

  suspend fun loadActiveDatabase(): GamblingDatabasePayload =
    withContext(Dispatchers.IO) {
      if (overrideFile.exists()) {
        runCatching {
            val raw = overrideFile.readText()
            parseAndValidate(raw)
          }
          .getOrNull()
          ?.let { return@withContext it }
      }
      loadBundledDatabase()
    }

  fun loadBundledDatabase(): GamblingDatabasePayload {
    val json =
      context.assets.open(BUNDLED_ASSET_NAME).bufferedReader().use { it.readText() }
    return requireNotNull(parseAndValidate(json)) {
      "Bundled gambling_database.json failed validation"
    }
  }

  suspend fun saveRemoteOverride(rawJson: String): Result<GamblingDatabasePayload> =
    withContext(Dispatchers.IO) {
      runCatching {
        val parsed =
          parseAndValidate(rawJson)
            ?: throw IllegalArgumentException("Invalid gambling database JSON schema")
        val tempFile = File(context.filesDir, "$OVERRIDE_FILE_NAME.tmp")
        tempFile.writeText(rawJson)
        if (!tempFile.renameTo(overrideFile)) {
          overrideFile.writeText(rawJson)
          tempFile.delete()
        }
        parsed
      }
    }

  suspend fun resetToBundled(): GamblingDatabasePayload =
    withContext(Dispatchers.IO) {
      if (overrideFile.exists()) {
        overrideFile.delete()
      }
      loadBundledDatabase()
    }

  fun parseAndValidate(rawJson: String): GamblingDatabasePayload? {
    val parsed = adapter.fromJson(rawJson) ?: return null
    if (parsed.version.isBlank() || parsed.platforms.isEmpty()) return null
    return parsed
  }

  companion object {
    const val BUNDLED_ASSET_NAME = "gambling_database.json"
    const val OVERRIDE_FILE_NAME = "gambling_database_override.json"
  }
}
