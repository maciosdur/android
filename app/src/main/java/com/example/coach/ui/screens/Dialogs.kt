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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coach.data.Exercise
import com.example.coach.data.Player

@Composable
fun PlayerSelectionDialog(
    allPlayers: List<Player>,
    onConfirm: (List<Player>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPlayers = remember { mutableStateListOf<Player>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz zawodników") },
        text = {
            LazyColumn {
                items(allPlayers) { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { 
                             if (player in selectedPlayers) selectedPlayers.remove(player) else selectedPlayers.add(player)
                        }
                    ) {
                        Checkbox(
                            checked = player in selectedPlayers,
                            onCheckedChange = { isChecked ->
                                if (isChecked) selectedPlayers.add(player) else selectedPlayers.remove(player)
                            }
                        )
                        Text(text = "${player.firstName} ${player.lastName}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedPlayers.toList()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
fun ExerciseSelectionDialog(
    historicalExercises: List<Exercise>,
    onExerciseSelected: (Exercise) -> Unit, // Returns the full object
    onAddNewExercise: (String) -> Unit, // For creating a new one
    onDismiss: () -> Unit
) {
    var newExerciseName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wybierz lub dodaj ćwiczenie") },
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
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Dodaj nowe")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Lub wybierz z listy:")
                LazyColumn {
                    items(historicalExercises) { exercise ->
                        Text(
                            text = exercise.name,
                            modifier = Modifier.fillMaxWidth().clickable { onExerciseSelected(exercise) }.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}