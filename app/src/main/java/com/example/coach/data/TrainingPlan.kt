package com.example.coach.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_plans",
    indices = [Index(value = ["name"], unique = true)]
)
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val date: Long = System.currentTimeMillis()
)