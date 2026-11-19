package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
  @Query("SELECT * FROM custom_rules ORDER BY createdAtMillis DESC")
  fun observeAllRules(): Flow<List<CustomRuleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRule(rule: CustomRuleEntity)

  @Query("UPDATE custom_rules SET enabled = :enabled WHERE id = :id")
  suspend fun setRuleEnabled(id: Long, enabled: Boolean)

  @Query("DELETE FROM custom_rules WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM custom_rules WHERE value = :value AND ruleType = :ruleType")
  suspend fun deleteByValueAndType(value: String, ruleType: String)
}
