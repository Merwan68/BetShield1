package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BlockEventEntity
import com.example.data.model.CustomRuleEntity

@Database(
  entities = [BlockEventEntity::class, CustomRuleEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class BetShieldDatabase : RoomDatabase() {
  abstract fun blockEventDao(): BlockEventDao

  abstract fun customRuleDao(): CustomRuleDao

  companion object {
    @Volatile private var instance: BetShieldDatabase? = null

    fun getInstance(context: Context): BetShieldDatabase {
      return instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                BetShieldDatabase::class.java,
                "betshield.db",
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { instance = it }
        }
    }
  }
}
