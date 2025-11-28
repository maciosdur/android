package com.example.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import com.example.coach.data.AppDatabase
import com.example.coach.data.ExerciseRepository
import com.example.coach.data.Player
import com.example.coach.data.PlayerRepository
import com.example.coach.data.TrainingPlanRepository
import com.example.coach.ui.screens.AddPlayerScreen
import com.example.coach.ui.screens.CreateTrainingPlanScreen
import com.example.coach.ui.screens.PlayerListScreen
import com.example.coach.ui.screens.TrainingPlanListScreen
import com.example.coach.ui.theme.CoachTheme
import com.example.coach.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

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
    val db = remember { AppDatabase.getDatabase(context) }

    // Repositories
    val playerRepository = remember { PlayerRepository(db.playerDao()) }
    val exerciseRepository = remember { ExerciseRepository(db.exerciseDao()) }
    val trainingPlanRepository = remember { TrainingPlanRepository(db.trainingPlanDao(), db.planEntryDao()) }

    val coroutineScope = rememberCoroutineScope()

    val players by playerRepository.getAllPlayers().collectAsState(initial = emptyList())
    val trainingPlans by trainingPlanRepository.getAllPlans().collectAsState(initial = emptyList())

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
                            onDeletePlayer = { player: Player ->
                                coroutineScope.launch {
                                    playerRepository.deletePlayer(player)
                                }
                            },
                            onEditPlayer = { player: Player ->
                                playerNavController.navigate("editPlayer/${player.id}")
                            }
                        )
                    }
                    composable("addPlayer") {
                        AddPlayerScreen {
                            coroutineScope.launch {
                                playerRepository.addPlayer(it)
                                playerNavController.popBackStack()
                            }
                        }
                    }
                    composable(
                        route = "editPlayer/{playerId}",
                        arguments = listOf(navArgument("playerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val playerId = backStackEntry.arguments?.getString("playerId")
                        val player = players.find { it.id == playerId }
                        player?.let {
                            AddPlayerScreen(it) { updatedPlayer ->
                                coroutineScope.launch {
                                    playerRepository.updatePlayer(updatedPlayer)
                                    playerNavController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
            composable(Screen.TrainingPlans.route) {
                val planNavController = rememberNavController()
                NavHost(planNavController, startDestination = "planList") {
                    composable("planList") {
                        TrainingPlanListScreen(
                            trainingPlans = trainingPlans,
                            onAddPlanClick = { planNavController.navigate("createPlan") },
                            onDeletePlan = {
                                coroutineScope.launch {
                                    trainingPlanRepository.deletePlan(it)
                                }
                            },
                            onEditPlan = { plan ->
                                planNavController.navigate("editPlan/${plan.id}")
                            }
                        )
                    }
                    composable("createPlan") {
                        val factory = remember { ViewModelFactory(trainingPlanRepository, playerRepository, exerciseRepository) }
                        CreateTrainingPlanScreen(factory = factory) {
                            planNavController.popBackStack()
                        }
                    }
                    composable(
                        route = "editPlan/{planId}",
                        arguments = listOf(navArgument("planId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val planId = backStackEntry.arguments?.getLong("planId")
                        val factory = remember(planId) { ViewModelFactory(trainingPlanRepository, playerRepository, exerciseRepository, planId) }
                        CreateTrainingPlanScreen(factory = factory) {
                            planNavController.popBackStack()
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
        // Note: Preview will not show database data
        // PlayerApp()
    }
}
