package com.example.coach

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coach.data.*
import com.example.coach.ui.screens.CreateTrainingPlanScreen
import com.example.coach.ui.screens.TrainingPlanListScreen
import com.example.coach.ui.theme.CoachTheme
import com.example.coach.viewmodels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingPlanIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var trainingPlanRepository: TrainingPlanRepository
    private lateinit var playerRepository: PlayerRepository
    private lateinit var exerciseRepository: ExerciseRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        playerRepository = PlayerRepository(db.playerDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        trainingPlanRepository = TrainingPlanRepository(db.trainingPlanDao(), db.planEntryDao())

    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun launchApp(): TestNavHostController {
        lateinit var navController: TestNavHostController

        composeTestRule.setContent {
            CoachTheme {
                navController = TestNavHostController(LocalContext.current)
                navController.navigatorProvider.addNavigator(ComposeNavigator())

                NavHost(navController = navController, startDestination = "planList") {
                    composable("planList") {
                        val plans = trainingPlanRepository.getAllPlans().collectAsState(initial = emptyList()).value
                        TrainingPlanListScreen(
                            trainingPlans = plans,
                            onAddPlanClick = { navController.navigate("createPlan") },
                            onDeletePlan = { plan ->
                                runBlocking { trainingPlanRepository.deletePlan(plan) }
                            },
                            onEditPlan = { plan -> navController.navigate("editPlan/${plan.id}") }
                        )
                    }
                    composable("createPlan") {
                        val factory = ViewModelFactory(trainingPlanRepository, playerRepository, exerciseRepository)
                        CreateTrainingPlanScreen(factory = factory) {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                navController.popBackStack()
                            }
                        }
                    }
                    composable(
                        route = "editPlan/{planId}",
                        arguments = listOf(navArgument("planId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val planId = backStackEntry.arguments?.getLong("planId")
                        val factory = ViewModelFactory(trainingPlanRepository, playerRepository, exerciseRepository, planId)
                        CreateTrainingPlanScreen(factory = factory) {
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
        return navController
    }

    private fun addBasePlayer() = runBlocking {
        playerRepository.addPlayer(
            Player(firstName = "Adam", lastName = "Małysz", birthYear = 1977, avatarResId = R.drawable.img1)
        )
    }

    @Test
    fun createTrainingPlan_flow() {
        addBasePlayer()
        val navController = launchApp()

        composeTestRule.onNodeWithText("+").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Nazwa planu").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Nazwa planu").performTextInput("Plan Skoczka")

        composeTestRule.onNodeWithText("Dodaj ćwiczenie").performClick()
        composeTestRule.onNodeWithText("Nowa nazwa ćwiczenia").performTextInput("Przysiady")
        composeTestRule.onNodeWithText("Dodaj do biblioteki i planu").performClick()
        composeTestRule.onNodeWithText("Zamknij").performClick()

        composeTestRule.onNodeWithText("Dodaj zawodnika").performClick()
        composeTestRule.onNodeWithText("Adam Małysz").performClick()
        composeTestRule.onNodeWithText("Zamknij").performClick()

        composeTestRule.onNodeWithContentDescription("Zapisz plan").performClick()
        composeTestRule.onNodeWithText("Plan Skoczka").assertExists()
    }

    @Test
    fun validate_save_button_visibility() {
        addBasePlayer()

        val navController = launchApp()

        composeTestRule.onNodeWithText("+").performClick()

        composeTestRule.onNodeWithText("Nazwa planu").performTextInput("Testowy Plan")
        composeTestRule.onNodeWithContentDescription("Zapisz plan").assertDoesNotExist()

        composeTestRule.onNodeWithText("Dodaj ćwiczenie").performClick()
        composeTestRule.onNodeWithText("Nowa nazwa ćwiczenia").performTextInput("Bieg")
        composeTestRule.onNodeWithText("Dodaj do biblioteki i planu").performClick()
        composeTestRule.onNodeWithText("Zamknij").performClick()
        composeTestRule.onNodeWithContentDescription("Zapisz plan").assertDoesNotExist()

        composeTestRule.onNodeWithText("Dodaj zawodnika").performClick()
        composeTestRule.onNodeWithText("Adam Małysz").performClick()
        composeTestRule.onNodeWithText("Zamknij").performClick()

        composeTestRule.onNodeWithContentDescription("Zapisz plan").assertIsDisplayed()
    }

    @Test
    fun edit_existing_training_plan() {
        runBlocking {
            val planId = trainingPlanRepository.addPlan(TrainingPlan(name = "Stary Plan", date = System.currentTimeMillis()))
            exerciseRepository.addExercise(Exercise(name = "Pompki"))
            val exercisesList = exerciseRepository.getAllExercises().first()
            val exerciseId = exercisesList.last().id

            // Dodajemy unikalnego gracza dla tego testu
            playerRepository.addPlayer(Player(firstName = "Test", lastName = "Gracz", birthYear = 2000, avatarResId = R.drawable.img1))
            val player = playerRepository.getAllPlayers().first().first()

            trainingPlanRepository.savePlanEntries(listOf(
                PlanEntry(planId, player.id, exerciseId, "10", "10", "0")
            ))
        }

        val navController = launchApp()

        composeTestRule.onNodeWithText("Stary Plan").assertExists()
        composeTestRule.onNodeWithContentDescription("Edit Plan").performClick()

        composeTestRule.onNodeWithText("Stary Plan").performTextClearance()
        composeTestRule.onNodeWithText("Nazwa planu").performTextInput("Zaktualizowany Plan")
        composeTestRule.onNodeWithContentDescription("Zapisz plan").performClick()

        composeTestRule.onNodeWithText("Zaktualizowany Plan").assertExists()
        composeTestRule.onNodeWithText("Stary Plan").assertDoesNotExist()
    }

    @Test
    fun delete_training_plan() {
        runBlocking {
            trainingPlanRepository.addPlan(TrainingPlan(name = "Plan do usunięcia", date = System.currentTimeMillis()))
        }


        composeTestRule.onNodeWithText("Plan do usunięcia").assertExists()
        composeTestRule.onNodeWithContentDescription("Delete Plan").performClick()
        composeTestRule.onNodeWithText("Plan do usunięcia").assertDoesNotExist()
    }

    @Test
    fun updatePlayerName_reflects_in_existing_training_plan() {
        var playerId: String? = null

        runBlocking {
            val player = Player(firstName = "Jan", lastName = "Kowalski", birthYear = 1990, avatarResId = R.drawable.img1)
            playerRepository.addPlayer(player)
            val savedPlayer = playerRepository.getAllPlayers().first().first()
            playerId = savedPlayer.id

            val planId = trainingPlanRepository.addPlan(TrainingPlan(name = "Plan Testowy", date = System.currentTimeMillis()))
            exerciseRepository.addExercise(Exercise(name = "Bieg"))
            val exerciseId = exerciseRepository.getAllExercises().first().last().id

            trainingPlanRepository.savePlanEntries(listOf(
                PlanEntry(planId, savedPlayer.id, exerciseId, "10", "10", "0")
            ))
        }

        val navController = launchApp()

        composeTestRule.onNodeWithContentDescription("Edit Plan").performClick()
        composeTestRule.onNodeWithText("Jan").assertExists()

        composeTestRule.onNodeWithContentDescription("Zapisz plan").performClick()

        runBlocking {
            val originalPlayer = playerRepository.getAllPlayers().first().first { it.id == playerId }
            val updatedPlayer = originalPlayer.copy(firstName = "PIOTR")
            playerRepository.updatePlayer(updatedPlayer)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Edit Plan").performClick()

        composeTestRule.onNodeWithText("PIOTR").assertExists()
        composeTestRule.onNodeWithText("Jan").assertDoesNotExist()
    }

    @Test
    fun deletePlayer_removes_them_from_existing_training_plan() {
        var playerId: String? = null

        runBlocking {
            val player = Player(firstName = "Adam", lastName = "Małysz", birthYear = 1977, avatarResId = R.drawable.img1)
            playerRepository.addPlayer(player)
            val savedPlayer = playerRepository.getAllPlayers().first().first()
            playerId = savedPlayer.id

            val planId = trainingPlanRepository.addPlan(TrainingPlan(name = "Plan Skoczka", date = System.currentTimeMillis()))
            exerciseRepository.addExercise(Exercise(name = "Wybicie"))
            val exerciseId = exerciseRepository.getAllExercises().first().last().id

            trainingPlanRepository.savePlanEntries(listOf(
                PlanEntry(planId, savedPlayer.id, exerciseId, "10", "10", "0")
            ))
        }

        val navController = launchApp()

        composeTestRule.onNodeWithContentDescription("Edit Plan").performClick()
        composeTestRule.onNodeWithText("Adam").assertExists()

        composeTestRule.onNodeWithContentDescription("Zapisz plan").performClick()

        runBlocking {
            val playerToDelete = playerRepository.getAllPlayers().first().first { it.id == playerId }
            playerRepository.deletePlayer(playerToDelete)
        }

        composeTestRule.onNodeWithContentDescription("Edit Plan").performClick()
        composeTestRule.onNodeWithText("Adam").assertDoesNotExist()
    }
}