package com.example.coach.data

import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAll()
    }

    suspend fun addExercise(exercise: Exercise) {
        exerciseDao.insert(exercise)
    }
}