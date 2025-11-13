package com.example.coach

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coach.data.Player
import com.example.coach.data.PlayerRepository
import com.example.coach.ui.screens.AddPlayerScreen
import com.example.coach.ui.screens.PlayerListScreen
import com.example.coach.ui.theme.CoachTheme

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Players : Screen("players", "Players", { Icon(Icons.Default.People, contentDescription = null) })
    object TrainingPlans : Screen("training_plans", "Training Plans", { Icon(Icons.Default.FitnessCenter, contentDescription = null) })
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoachTheme {
                PlayerApp()
            }
        }
    }
}

@Composable
fun PlayerApp() {
    val context = LocalContext.current
    val playerRepository = remember { PlayerRepository(context) }

    val players = remember { mutableStateListOf<Player>() }


    LaunchedEffect(context) {
        val migrationPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
        val needsMigration = !migrationPrefs.getBoolean("player_id_migrated_v2", false)

        if (needsMigration) {
            val oldPlayers = playerRepository.loadPlayers()
            val newPlayers = oldPlayers.map { Player(firstName = it.firstName, lastName = it.lastName, birthYear = it.birthYear) }
            playerRepository.savePlayers(newPlayers)
            players.addAll(newPlayers)

            migrationPrefs.edit().putBoolean("player_id_migrated_v2", true).apply()
        } else {
            players.addAll(playerRepository.loadPlayers())
        }
    }

    val navController = rememberNavController()
    val items = listOf(Screen.Players, Screen.TrainingPlans)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Players.route, Modifier.padding(innerPadding)) {
            composable(Screen.Players.route) {
                val playerNavController = rememberNavController()
                NavHost(playerNavController, startDestination = "playerList") {
                    composable("playerList") {
                        PlayerListScreen(
                            players = players,
                            onAddPlayerClick = { playerNavController.navigate("addPlayer") },
                            onDeletePlayer = {
                                players.remove(it)
                                playerRepository.savePlayers(players)
                            },
                            onEditPlayer = { playerNavController.navigate("editPlayer/$it") }
                        )
                    }
                    composable("addPlayer") {
                        AddPlayerScreen {
                            players.add(it)
                            playerRepository.savePlayers(players)
                            playerNavController.popBackStack()
                        }
                    }
                    composable(
                        route = "editPlayer/{index}",
                        arguments = listOf(navArgument("index") { type = NavType.IntType })
                    ) {
                        val index = it.arguments?.getInt("index") ?: -1
                        AddPlayerScreen(players[index]) {
                            players[index] = it
                            playerRepository.savePlayers(players)
                            playerNavController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CoachTheme {
        PlayerApp()
    }
}