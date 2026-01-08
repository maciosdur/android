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

    @Query("""
        SELECT pe.* FROM plan_entries pe
        INNER JOIN training_plans tp ON pe.planId = tp.id
        WHERE pe.playerId = :playerId AND pe.exerciseId = :exerciseId
        ORDER BY tp.date DESC
        LIMIT 1
    """)
    suspend fun findLastEntry(playerId: String, exerciseId: Long): PlanEntry?
}