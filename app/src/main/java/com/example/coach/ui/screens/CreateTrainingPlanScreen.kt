package com.example.coach.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coach.viewmodels.CreatePlanUiState
import com.example.coach.viewmodels.CreatePlanViewModel
import com.example.coach.viewmodels.GridCell
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
        val assignedPlayerIds = uiState.playerColumns.flatMap { it.players.map { p -> p.id } }.toSet()
        val unassignedPlayers = uiState.allPlayers.filter { it.id !in assignedPlayerIds }
        PlayerSelectionDialog(
            allPlayers = unassignedPlayers,
            onConfirm = { selectedPlayers ->
                viewModel.addPlayerColumn(selectedPlayers)
                showPlayerDialog = false
            },
            onDismiss = { showPlayerDialog = false }
        )
    }

    if (showExerciseDialog) {
        ExerciseSelectionDialog(
            historicalExercises = uiState.allExercises,
            onExerciseSelected = {
                viewModel.addExerciseToPlan(it)
                showExerciseDialog = false
            },
            onAddNewExercise = {
                viewModel.addNewExerciseToLibrary(it)
            },
            onDismiss = { showExerciseDialog = false }
        )
    }

    Scaffold {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(it).padding(16.dp)) {
                OutlinedTextField(
                    value = uiState.plan.name,
                    onValueChange = { viewModel.onPlanNameChange(it) },
                    label = { Text("Nazwa planu") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.padding(vertical = 16.dp)) {
                    Button(onClick = { showExerciseDialog = true }) { Text("Dodaj ćwiczenie") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { showPlayerDialog = true }) { Text("Dodaj zawodnika") }
                }

                val scrollState = rememberScrollState()
                LazyColumn(Modifier.horizontalScroll(scrollState)) {
                    // Header Row
                    item {
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.width(200.dp)) // Exercise name column
                            uiState.playerColumns.forEachIndexed { colIndex, col ->
                                Column(
                                    modifier = Modifier.width(240.dp).padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val names = col.players.joinToString(", ") { p -> p.firstName }
                                    Text(text = names)
                                    IconButton(onClick = { viewModel.removePlayerColumn(colIndex) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Usuń kolumnę")
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(uiState.exercisesInPlan, key = { _, item -> item.id }) { rowIndex, exercise ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.width(200.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(exercise.name, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                IconButton(onClick = { viewModel.removeExerciseFromPlan(exercise) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Usuń ćwiczenie")
                                }
                            }
                            uiState.playerColumns.forEachIndexed { colIndex, col ->
                                val cell = col.entries[exercise.id] ?: GridCell()
                                Row(modifier = Modifier.width(240.dp)) {
                                    OutlinedTextField(
                                        value = cell.sets,
                                        onValueChange = { viewModel.updateCell(colIndex, exercise.id, cell.copy(sets = it)) },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        label = { Text("Serie") }
                                    )
                                    OutlinedTextField(
                                        value = cell.reps,
                                        onValueChange = { viewModel.updateCell(colIndex, exercise.id, cell.copy(reps = it)) },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        label = { Text("Pow.") }
                                    )
                                    OutlinedTextField(
                                        value = cell.weight,
                                        onValueChange = { viewModel.updateCell(colIndex, exercise.id, cell.copy(weight = it)) },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        label = { Text("Ciężar") }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.savePlan() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.plan.name.isNotBlank() && uiState.exercisesInPlan.isNotEmpty() && uiState.playerColumns.isNotEmpty()
                ) { Text("Zapisz plan") }
            }
        }
    }
}