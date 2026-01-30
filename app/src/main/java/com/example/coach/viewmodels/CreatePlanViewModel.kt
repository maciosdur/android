package com.example.coach.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coach.data.Exercise
import com.example.coach.data.ExerciseRepository
import com.example.coach.data.PlanEntry
import com.example.coach.data.Player
import com.example.coach.data.PlayerRepository
import com.example.coach.data.TrainingPlan
import com.example.coach.data.TrainingPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerColumn(
    val player: Player,
    val entries: Map<Long, GridCell>
)

data class GridCell(
    val sets: String = "",
    val reps: String = "",
    val weight: String = ""
)

data class CreatePlanUiState(
    val plan: TrainingPlan = TrainingPlan(name = "", date = System.currentTimeMillis()),
    val exercisesInPlan: List<Exercise> = emptyList(),
    val playerColumns: List<PlayerColumn> = emptyList(),
    val allPlayers: List<Player> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val isFinished: Boolean = false
)

class CreatePlanViewModel(
    private val planId: Long?,
    private val planRepository: TrainingPlanRepository,
    private val playerRepository: PlayerRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlanUiState())
    val uiState: StateFlow<CreatePlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val allPlayers = playerRepository.getAllPlayers().first()
            val allExercises = exerciseRepository.getAllExercises().first()
            _uiState.update {
                it.copy(allPlayers = allPlayers, allExercises = allExercises)
            }

            if (planId != null) {
                loadPlanForEditing(planId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadPlanForEditing(planId: Long) {
        val plan = planRepository.getPlanById(planId) ?: return
        val entries = planRepository.getEntriesForPlan(planId).first()

        val exercisesInPlan = entries.mapNotNull { entry ->
            _uiState.value.allExercises.find { it.id == entry.exerciseId }
        }.distinctBy { it.id }

        val playerColumns = entries.groupBy { it.playerId }.map { (playerId, playerEntries) ->
            val player = _uiState.value.allPlayers.find { it.id == playerId }!!
            val cellMap = playerEntries.associate { it.exerciseId to GridCell(it.sets, it.reps, it.weight) }
            PlayerColumn(player = player, entries = cellMap)
        }

        _uiState.update {
            it.copy(plan = plan, exercisesInPlan = exercisesInPlan, playerColumns = playerColumns, isLoading = false)
        }
    }

    fun onPlanNameChange(name: String) {
        _uiState.update { it.copy(plan = it.plan.copy(name = name)) }
    }

    // --- Exercise Management ---
    fun addNewExerciseToLibrary(name: String, andToggleInPlan: Boolean = false) {
        viewModelScope.launch {
            exerciseRepository.addExercise(Exercise(name = name))
            if (andToggleInPlan) {
                val addedExercise = exerciseRepository.getAllExercises().first().last { it.name == name }
                toggleExerciseInPlan(addedExercise, true)
            }
        }
    }

    fun toggleExerciseInPlan(exercise: Exercise, isInPlan: Boolean) {
        if (isInPlan) {
            viewModelScope.launch {
                val updatedColumns = _uiState.value.playerColumns.map { column ->
                    val newEntries = column.entries.toMutableMap()
                    val lastEntry = planRepository.findLastEntry(column.player.id, exercise.id)
                    if (lastEntry != null) {
                        newEntries[exercise.id] = GridCell(lastEntry.sets, lastEntry.reps, lastEntry.weight)
                    }
                    column.copy(entries = newEntries)
                }
                _uiState.update {
                    if (it.exercisesInPlan.any { ex -> ex.id == exercise.id }) return@update it // Avoid duplicates
                    it.copy(exercisesInPlan = it.exercisesInPlan + exercise, playerColumns = updatedColumns)
                }
            }
        } else {
            _uiState.update {
                it.copy(exercisesInPlan = it.exercisesInPlan.filterNot { ex -> ex.id == exercise.id })
            }
        }
    }

    // --- Player Management ---
    fun togglePlayerInPlan(player: Player, isInPlan: Boolean) {
        if (isInPlan) {
            viewModelScope.launch {
                val newEntries = mutableMapOf<Long, GridCell>()
                _uiState.value.exercisesInPlan.forEach { exercise ->
                    val lastEntry = planRepository.findLastEntry(player.id, exercise.id)
                    if (lastEntry != null) {
                        newEntries[exercise.id] = GridCell(lastEntry.sets, lastEntry.reps, lastEntry.weight)
                    }
                }
                val newColumn = PlayerColumn(player = player, entries = newEntries)
                _uiState.update {
                    if (it.playerColumns.any { c -> c.player.id == player.id }) return@update it
                    it.copy(playerColumns = it.playerColumns + newColumn)
                }
            }
        } else {
            _uiState.update {
                it.copy(playerColumns = it.playerColumns.filterNot { col -> col.player.id == player.id })
            }
        }
    }
    
    fun removePlayerColumnFromPlan(index: Int) {
        _uiState.update {
            val updatedColumns = it.playerColumns.toMutableList().apply { removeAt(index) }
            it.copy(playerColumns = updatedColumns)
        }
    }

    fun removeExerciseFromPlan(exercise: Exercise) {
        _uiState.update {
            it.copy(exercisesInPlan = it.exercisesInPlan.filterNot { ex -> ex.id == exercise.id })
        }
    }

    // --- Grid Management ---
    fun updateCell(columnIndex: Int, exerciseId: Long, newCell: GridCell) {
        _uiState.update {
            val updatedColumns = it.playerColumns.toMutableList()
            val columnToUpdate = updatedColumns[columnIndex]
            val updatedEntries = columnToUpdate.entries.toMutableMap()
            updatedEntries[exerciseId] = newCell
            updatedColumns[columnIndex] = columnToUpdate.copy(entries = updatedEntries)
            it.copy(playerColumns = updatedColumns)
        }
    }

    fun savePlan() {
        viewModelScope.launch {
            val currentPlan = _uiState.value.plan
            val planIdToSave = if (currentPlan.id != 0L) {
                planRepository.updatePlan(currentPlan)
                currentPlan.id
            } else {
                planRepository.addPlan(currentPlan)
            }

            val entriesToSave = mutableListOf<PlanEntry>()
            _uiState.value.playerColumns.forEach { column ->
                _uiState.value.exercisesInPlan.forEach { exercise ->
                    val cell = column.entries[exercise.id] ?: GridCell()
                    entriesToSave.add(
                        PlanEntry(planIdToSave, column.player.id, exercise.id, cell.sets, cell.reps, cell.weight)
                    )
                }
            }
            planRepository.savePlanEntries(entriesToSave)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}