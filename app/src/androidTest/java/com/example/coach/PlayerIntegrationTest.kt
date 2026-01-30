package com.example.coach

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.coach.data.AppDatabase
import com.example.coach.data.Player
import com.example.coach.data.PlayerRepository
import com.example.coach.ui.screens.AddPlayerScreen
import com.example.coach.ui.screens.PlayerListScreen
import com.example.coach.ui.theme.CoachTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var playerRepository: PlayerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playerRepository = PlayerRepository(db.playerDao())
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

                val players by playerRepository.getAllPlayers().collectAsState(initial = emptyList())
                val coroutineScope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "playerList") {
                    composable("playerList") {
                        PlayerListScreen(
                            players = players,
                            onAddPlayerClick = { navController.navigate("addPlayer") },
                            onDeletePlayer = { player ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    playerRepository.deletePlayer(player)
                                }
                            },
                            onEditPlayer = { player ->
                                navController.navigate("editPlayer/${player.id}")
                            }
                        )
                    }
                    composable("addPlayer") {
                        AddPlayerScreen { player ->
                            coroutineScope.launch(Dispatchers.Main) {
                                playerRepository.addPlayer(player)
                                navController.popBackStack()
                            }
                        }
                    }
                    composable(
                        route = "editPlayer/{playerId}",
                        arguments = listOf(navArgument("playerId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val playerId = backStackEntry.arguments?.getString("playerId")

                        val playerToEdit = players.find { it.id == playerId }

                        if (playerToEdit != null) {
                            AddPlayerScreen(player = playerToEdit) { updatedPlayer ->
                                coroutineScope.launch(Dispatchers.Main) {
                                    playerRepository.updatePlayer(updatedPlayer)
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
        }
        return navController
    }

    @Test
    fun addPlayer_and_verify_displays_in_list() {
        launchApp()

        composeTestRule.onNodeWithText("Jan Kowalski").assertDoesNotExist()

        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("Imię").performTextInput("Jan")
        composeTestRule.onNodeWithText("Nazwisko").performTextInput("Kowalski")
        composeTestRule.onNodeWithText("Rocznik urodzenia").performTextInput("1995")
        composeTestRule.onNodeWithText("Dodaj zawodnika").performClick()

        composeTestRule.onNodeWithText("Jan Kowalski").assertExists()
        composeTestRule.onNodeWithText("Rocznik: 1995").assertExists()
    }

    @Test
    fun editPlayer_and_verify_changes() {
        runBlocking {
            playerRepository.addPlayer(Player(firstName = "Adam", lastName = "Stary", birthYear = 1990, avatarResId = R.drawable.img1))
        }

        launchApp()

        composeTestRule.onNodeWithText("Adam Stary").assertExists()

        composeTestRule.onNodeWithContentDescription("Edit Player").performClick()

        composeTestRule.onNodeWithText("Adam").performTextClearance()
        composeTestRule.onNodeWithText("Imię").performTextInput("Piotr") // Zmieniamy też imię

        composeTestRule.onNodeWithText("Stary").performTextClearance()
        composeTestRule.onNodeWithText("Nazwisko").performTextInput("Nowy")

        composeTestRule.onNodeWithText("1990").performTextClearance()
        composeTestRule.onNodeWithText("Rocznik urodzenia").performTextInput("2000")

        composeTestRule.onNodeWithText("Zapisz zmiany").performClick()

        composeTestRule.onNodeWithText("Adam Stary").assertDoesNotExist()
        composeTestRule.onNodeWithText("Piotr Nowy").assertExists()
        composeTestRule.onNodeWithText("Rocznik: 2000").assertExists()
    }

    @Test
    fun deletePlayer_removes_from_list() {
        runBlocking {
            playerRepository.addPlayer(Player(firstName = "Do Usunięcia", lastName = "X", birthYear = 1999, avatarResId = R.drawable.img2))
        }

        launchApp()

        composeTestRule.onNodeWithText("Do Usunięcia X").assertExists()

        composeTestRule.onNodeWithContentDescription("Delete Player").performClick()

        composeTestRule.onNodeWithText("Do Usunięcia X").assertDoesNotExist()

        val playersInDb = runBlocking { playerRepository.getAllPlayers().first() }
        assertEquals(0, playersInDb.size)
    }

    @Test
    fun validation_empty_form_does_not_save() {
        val navController = launchApp()

        composeTestRule.onNodeWithText("+").performClick()

        composeTestRule.onNodeWithText("Dodaj zawodnika").performClick()

        composeTestRule.onNodeWithText("Imię").assertIsDisplayed()

        val playersInDb = runBlocking { playerRepository.getAllPlayers().first() }
        assertEquals(0, playersInDb.size)

        composeTestRule.onNodeWithText("+").assertDoesNotExist()
    }
}