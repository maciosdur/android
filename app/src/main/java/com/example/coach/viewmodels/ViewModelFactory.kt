package com.example.coach.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.coach.data.ExerciseRepository
import com.example.coach.data.PlayerRepository
import com.example.coach.data.TrainingPlanRepository

class ViewModelFactory(
    private val planRepository: TrainingPlanRepository,
    private val playerRepository: PlayerRepository,
    private val exerciseRepository: ExerciseRepository,
    private val planId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePlanViewModel(planId, planRepository, playerRepository, exerciseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}