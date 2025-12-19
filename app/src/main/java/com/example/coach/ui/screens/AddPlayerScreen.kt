package com.example.coach.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.coach.data.AvatarRepository
import com.example.coach.data.Player

@Composable
fun AddPlayerScreen(player: Player? = null, onPlayerAction: (Player) -> Unit) {
    var firstName by remember { mutableStateOf(player?.firstName ?: "") }
    var lastName by remember { mutableStateOf(player?.lastName ?: "") }
    var birthYear by remember { mutableStateOf(player?.birthYear?.toString() ?: "") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Imię") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Nazwisko") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = birthYear,
            onValueChange = { birthYear = it },
            label = { Text("Rocznik urodzenia") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val year = birthYear.toIntOrNull()
                if (firstName.isNotBlank() && lastName.isNotBlank() && year != null) {
                    val playerToSave = if (player != null) {
                        // Editing existing player, keep the old avatar
                        player.copy(
                            firstName = firstName,
                            lastName = lastName,
                            birthYear = year
                        )
                    } else {
                        // Creating new player, assign a random avatar
                        Player(
                            firstName = firstName,
                            lastName = lastName,
                            birthYear = year,
                            avatarResId = AvatarRepository.getRandomAvatar()
                        )
                    }
                    onPlayerAction(playerToSave)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (player == null) "Dodaj zawodnika" else "Zapisz zmiany")
        }
    }
}
