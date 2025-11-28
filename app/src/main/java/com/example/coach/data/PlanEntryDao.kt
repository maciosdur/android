package com.example.coach.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanEntryDao {
    @Query("SELECT * FROM plan_entries WHERE planId = :planId")
    fun getEntriesForPlan(planId: Long): Flow<List<PlanEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PlanEntry>)
}