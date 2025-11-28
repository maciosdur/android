package com.example.coach.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trainingPlan: TrainingPlan): Long

    @Query("SELECT * FROM training_plans ORDER BY date DESC")
    fun getAllTrainingPlans(): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): TrainingPlan?

    @Delete
    suspend fun delete(plan: TrainingPlan)
}