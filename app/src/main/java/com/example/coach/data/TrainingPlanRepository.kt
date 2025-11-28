package com.example.coach.data

import kotlinx.coroutines.flow.Flow

class TrainingPlanRepository(private val trainingPlanDao: TrainingPlanDao, private val planEntryDao: PlanEntryDao) {

    fun getAllPlans(): Flow<List<TrainingPlan>> {
        return trainingPlanDao.getAllTrainingPlans()
    }

    suspend fun getPlanById(planId: Long): TrainingPlan? {
        return trainingPlanDao.getPlanById(planId)
    }

    fun getEntriesForPlan(planId: Long): Flow<List<PlanEntry>> {
        return planEntryDao.getEntriesForPlan(planId)
    }

    suspend fun addPlan(plan: TrainingPlan): Long {
        return trainingPlanDao.insert(plan)
    }

    suspend fun deletePlan(plan: TrainingPlan) {
        trainingPlanDao.delete(plan)
    }

    suspend fun savePlanEntries(entries: List<PlanEntry>) {
        planEntryDao.insertAll(entries)
    }
}