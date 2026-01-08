package com.example.coach.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coach.data.Exercise
import com.example.coach.viewmodels.CreatePlanViewModel
import com.example.coach.viewmodels.GridCell
import com.example.coach.viewmodels.PlayerColumn
import com.example.coach.viewmodels.ViewModelFactory

@Composable
fun CreateTrainingPlanScreen(
    factory: ViewModelFactory,
    onPlanSaved: () -> Unit
) {
    val viewModel: CreatePlanViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    var showPlayerDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onPlanSaved()
        }
    }

    if (showPlayerDialog) {
        PlayerManagementDialog(
            allPlayers = uiState.allPlayers,
            playersInPlan = uiState.playerColumns.map { it.player },
            onPlayerToggled = viewModel::togglePlayerInPlan,
            onDismiss = { showPlayerDialog = false }
        )
    }

    if (showExerciseDialog) {
        ExerciseManagementDialog(
            allExercises = uiState.allExercises,
            exercisesInPlan = uiState.exercisesInPlan,
            onExerciseToggled = viewModel::toggleExerciseInPlan,
            onAddNewExercise = { viewModel.addNewExerciseToLibrary(it, andToggleInPlan = true) },
            onDismiss = { showExerciseDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            // The FAB is only shown when the plan is ready to be saved
            if (uiState.plan.name.isNotBlank() && uiState.exercisesInPlan.isNotEmpty() && uiState.playerColumns.isNotEmpty()) {
                FloatingActionButton(onClick = viewModel::savePlan) {
                    Icon(Icons.Filled.Check, contentDescription = "Zapisz plan")
                }
            }
        }
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(it).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.plan.name,
                        onValueChange = viewModel::onPlanNameChange,
                        label = { Text("Nazwa planu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = { showExerciseDialog = true }) { Text("Dodaj ćwiczenie") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { showPlayerDialog = true }) { Text("Dodaj zawodnika") }
                    }
                }

                items(uiState.exercisesInPlan, key = { it.id }) { exercise ->
                    ExerciseInPlanCard(
                        exercise = exercise,
                        playerColumns = uiState.playerColumns,
                        onUpdateCell = viewModel::updateCell,
                        onRemoveExercise = { viewModel.removeExerciseFromPlan(exercise) },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Add space for the FAB
                }
            }
        }
    }
}

@Composable
private fun ExerciseInPlanCard(
    exercise: Exercise,
    playerColumns: List<PlayerColumn>,
    onUpdateCell: (colIndex: Int, exerciseId: Long, newCell: GridCell) -> Unit,
    onRemoveExercise: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = exercise.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemoveExercise) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń ćwiczenie z planu")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (playerColumns.isEmpty()) {
                Text("Dodaj zawodników, aby uzupełnić ich wyniki.", style = MaterialTheme.typography.bodyMedium)
            } else {
                playerColumns.forEachIndexed { colIndex, col ->
                    val cell = col.entries[exercise.id] ?: GridCell()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = col.player.firstName,
                            modifier = Modifier.weight(0.3f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = cell.sets,
                            onValueChange = { onUpdateCell(colIndex, exercise.id, cell.copy(sets = it)) },
                            modifier = Modifier.weight(0.2f).padding(horizontal = 2.dp),
                            label = { Text("S") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cell.reps,
                            onValueChange = { onUpdateCell(colIndex, exercise.id, cell.copy(reps = it)) },
                            modifier = Modifier.weight(0.2f).padding(horizontal = 2.dp),
                            label = { Text("P") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cell.weight,
                            onValueChange = { onUpdateCell(colIndex, exercise.id, cell.copy(weight = it)) },
                            modifier = Modifier.weight(0.3f).padding(horizontal = 2.dp),
                            label = { Text("C") },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}