package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockEventType(val key: String) {
  DOMAIN("DOMAIN"),
  APP("APP"),
}

enum class BlockEventSource(val key: String) {
  VPN_DNS("VPN_DNS"),
  ACCESSIBILITY("ACCESSIBILITY"),
  FILTER_CHECK("FILTER_CHECK"),
}

@Entity(tableName = "block_events")
data class BlockEventEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val target: String,
  val type: String,
  val category: String,
  val source: String,
  val timestampMillis: Long = System.currentTimeMillis(),
)
