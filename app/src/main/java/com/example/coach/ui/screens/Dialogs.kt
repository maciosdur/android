package com.example.coach.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coach.data.Exercise
import com.example.coach.data.Player

@Composable
fun PlayerManagementDialog(
    allPlayers: List<Player>,
    playersInPlan: List<Player>,
    onPlayerToggled: (player: Player, isInPlan: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val playersInPlanIds = remember(playersInPlan) { playersInPlan.map { it.id }.toSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zarządzaj zawodnikami w planie") },
        text = {
            LazyColumn {
                items(allPlayers, key = { it.id }) { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerToggled(player, player.id !in playersInPlanIds) }
                    ) {
                        Checkbox(
                            checked = player.id in playersInPlanIds,
                            onCheckedChange = { isChecked -> onPlayerToggled(player, isChecked) }
                        )
                        Text(text = "${player.firstName} ${player.lastName}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

@Composable
fun ExerciseManagementDialog(
    allExercises: List<Exercise>,
    exercisesInPlan: List<Exercise>,
    onExerciseToggled: (exercise: Exercise, isInPlan: Boolean) -> Unit,
    onAddNewExercise: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newExerciseName by remember { mutableStateOf("") }
    val exercisesInPlanIds = remember(exercisesInPlan) { exercisesInPlan.map { it.id }.toSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zarządzaj ćwiczeniami") },
        text = {
            Column {
                OutlinedTextField(
                    value = newExerciseName,
                    onValueChange = { newExerciseName = it },
                    label = { Text("Nowa nazwa ćwiczenia") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (newExerciseName.isNotBlank()) {
                            onAddNewExercise(newExerciseName)
                            newExerciseName = ""
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Dodaj do biblioteki i planu")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Zaznacz, aby dodać/usunąć z planu:")
                LazyColumn {
                    items(allExercises, key = { it.id }) { exercise ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseToggled(exercise, exercise.id !in exercisesInPlanIds) }
                        ) {
                            Checkbox(
                                checked = exercise.id in exercisesInPlanIds,
                                onCheckedChange = { isChecked -> onExerciseToggled(exercise, isChecked) }
                            )
                            Text(text = exercise.name, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}