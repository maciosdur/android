package com.example.coach.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "plan_entries",
    primaryKeys = ["planId", "playerId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanEntry(
    val planId: Long,
    val playerId: String,
    val exerciseId: Long,
    val sets: String = "",
    val reps: String = "",
    val weight: String = ""
)