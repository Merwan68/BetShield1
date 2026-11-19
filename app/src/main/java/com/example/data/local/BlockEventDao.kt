package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BlockEventEntity
import kotlinx.coroutines.flow.Flow

data class TargetCountProjection(
  val target: String,
  val type: String,
  val count: Int,
)

@Dao
interface BlockEventDao {
  @Query("SELECT * FROM block_events ORDER BY timestampMillis DESC LIMIT 200")
  fun observeRecentEvents(): Flow<List<BlockEventEntity>>

  @Query("SELECT * FROM block_events WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
  fun observeEventsSince(sinceMillis: Long): Flow<List<BlockEventEntity>>

  @Query("SELECT COUNT(*) FROM block_events")
  fun observeTotalCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM block_events WHERE timestampMillis >= :sinceMillis")
  fun observeCountSince(sinceMillis: Long): Flow<Int>

  @Query(
    """
    SELECT target, type, COUNT(*) as count
    FROM block_events
    GROUP BY target, type
    ORDER BY count DESC
    LIMIT 10
    """
  )
  fun observeTopBlockedTargets(): Flow<List<TargetCountProjection>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: BlockEventEntity)

  @Query("DELETE FROM block_events")
  suspend fun clearAll()
}
