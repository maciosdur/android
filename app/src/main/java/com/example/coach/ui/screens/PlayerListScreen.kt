package com.example.coach.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.coach.data.Player

@Composable
fun PlayerListScreen(
    players: List<Player>,
    onAddPlayerClick: () -> Unit,
    onDeletePlayer: (Player) -> Unit,
    onEditPlayer: (Player) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlayerClick) {
                Text("+")
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(players, key = { it.id }) { player ->
                PlayerItem(
                    player = player,
                    onDeleteClick = { onDeletePlayer(player) },
                    onEditClick = { onEditPlayer(player) }
                )
            }
        }
    }
}

@Composable
fun PlayerItem(player: Player, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = player.avatarResId),
                contentDescription = "Avatar gracza",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${player.firstName} ${player.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rocznik: ${player.birthYear}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Player")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Player")
            }
        }
    }
}