package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CustomRuleType(val key: String) {
  DOMAIN("DOMAIN"),
  APP("APP"),
}

@Entity(
  tableName = "custom_rules",
  indices = [Index(value = ["value", "ruleType"], unique = true)],
)
data class CustomRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val value: String,
  val label: String,
  val ruleType: String,
  val enabled: Boolean = true,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
