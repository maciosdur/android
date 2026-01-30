package com.example.coach.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.coach.data.Exercise
import com.example.coach.data.ExerciseRepository
import com.example.coach.data.PlanEntry
import com.example.coach.data.Player
import com.example.coach.data.PlayerRepository
import com.example.coach.data.TrainingPlan
import com.example.coach.data.TrainingPlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CreatePlanViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var planRepository: TrainingPlanRepository
    private lateinit var playerRepository: PlayerRepository
    private lateinit var exerciseRepository: ExerciseRepository

    private lateinit var viewModel: CreatePlanViewModel

    private val player1 = Player(id = "1", firstName = "Jan", lastName = "Kowalski", birthYear = 1990, avatarResId = 0)
    private val exercise1 = Exercise(id = 10, name = "Przysiad")
    private val testPlan = TrainingPlan(id = 1, name = "Test Plan", date = System.currentTimeMillis())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepository = mockk(relaxed = true)
        playerRepository = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)

        coEvery { playerRepository.getAllPlayers() } returns flowOf(listOf(player1))
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(exercise1))
        coEvery { planRepository.findLastEntry(any(), any()) } returns null
    }

    @Test
    fun `init - when creating a new plan, state is loaded correctly`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf(player1), state.allPlayers)
        assertEquals(listOf(exercise1), state.allExercises)
        assertTrue(state.playerColumns.isEmpty())
        assertTrue(state.exercisesInPlan.isEmpty())
    }

    @Test
    fun `togglePlayerInPlan - when player is added, state updates correctly`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePlayerInPlan(player1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.playerColumns.size)
        assertEquals(player1.id, state.playerColumns.first().player.id)
    }

    @Test
    fun `togglePlayerInPlan - when player with history is added, last entry is fetched`() = runTest {
        val lastEntry = PlanEntry(1, player1.id, exercise1.id, "5", "5", "100kg")
        coEvery { planRepository.findLastEntry(player1.id, exercise1.id) } returns lastEntry
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        viewModel.toggleExerciseInPlan(exercise1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.togglePlayerInPlan(player1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        val column = viewModel.uiState.value.playerColumns.first()
        val cell = column.entries[exercise1.id]
        assertEquals("5", cell?.sets)
        assertEquals("5", cell?.reps)
        assertEquals("100kg", cell?.weight)
    }

    @Test
    fun `savePlan - when saving a new plan, repositories are called correctly`() = runTest {
        coEvery { planRepository.addPlan(any()) } returns 1L
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.togglePlayerInPlan(player1, true)
        viewModel.toggleExerciseInPlan(exercise1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { planRepository.addPlan(any()) }
        coVerify { planRepository.savePlanEntries(any()) }
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `togglePlayerInPlan - when player is removed, column disappears from state`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.togglePlayerInPlan(player1, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.playerColumns.size)

        viewModel.togglePlayerInPlan(player1, false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.playerColumns.isEmpty())
    }

    @Test
    fun `removeExerciseFromPlan - when exercise is removed, row disappears from state`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleExerciseInPlan(exercise1, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.exercisesInPlan.size)

        viewModel.removeExerciseFromPlan(exercise1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.exercisesInPlan.isEmpty())
    }

    @Test
    fun `updateCell - when a cell is updated, state reflects the change`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.togglePlayerInPlan(player1, true)
        viewModel.toggleExerciseInPlan(exercise1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        val newCellData = GridCell(sets = "3", reps = "12", weight = "80")
        viewModel.updateCell(0, exercise1.id, newCellData)
        testDispatcher.scheduler.advanceUntilIdle()

        val column = viewModel.uiState.value.playerColumns.first()
        val cell = column.entries[exercise1.id]
        assertNotNull(cell)
        assertEquals("3", cell?.sets)
        assertEquals("12", cell?.reps)
        assertEquals("80", cell?.weight)
    }

    @Test
    fun `init - when editing an existing plan, state is loaded correctly`() = runTest {
        val existingEntry = PlanEntry(testPlan.id, player1.id, exercise1.id, "5", "5", "100")
        coEvery { planRepository.getPlanById(testPlan.id) } returns testPlan
        coEvery { planRepository.getEntriesForPlan(testPlan.id) } returns flowOf(listOf(existingEntry))

        viewModel = CreatePlanViewModel(testPlan.id, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(testPlan.name, state.plan.name)
        assertEquals(1, state.exercisesInPlan.size)
        assertEquals(exercise1.id, state.exercisesInPlan.first().id)
        assertEquals(1, state.playerColumns.size)
        assertEquals(player1.id, state.playerColumns.first().player.id)
        val cell = state.playerColumns.first().entries[exercise1.id]
        assertEquals("5", cell?.sets)
    }
    
    @Test
    fun `togglePlayerInPlan - when duplicate player is added, column count does not change`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.togglePlayerInPlan(player1, true) // First add
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("After first add, column count should be 1", 1, viewModel.uiState.value.playerColumns.size)

        viewModel.togglePlayerInPlan(player1, true) // Second add
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("After second add, column count should still be 1", 1, viewModel.uiState.value.playerColumns.size)
    }


    @Test
    fun `toggleExerciseInPlan - when duplicate exercise is added, row count does not change`() = runTest {
        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleExerciseInPlan(exercise1, true) // First add
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("After first add, exercise count should be 1", 1, viewModel.uiState.value.exercisesInPlan.size)

        viewModel.toggleExerciseInPlan(exercise1, true) // Second add
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("After second add, exercise count should still be 1", 1, viewModel.uiState.value.exercisesInPlan.size)
    }

    @Test
    fun `addNewExerciseToLibrary - when called, repository is updated and exercise is added to plan`() = runTest {
        val newExerciseName = "Martwy Ciąg"
        val newExercise = Exercise(id = 11, name = newExerciseName)
        coEvery { exerciseRepository.addExercise(any()) } returns Unit
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(exercise1, newExercise)) // Mock the updated list

        viewModel = CreatePlanViewModel(null, planRepository, playerRepository, exerciseRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addNewExerciseToLibrary(newExerciseName, andToggleInPlan = true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { exerciseRepository.addExercise(any()) }
        val state = viewModel.uiState.value
        assertTrue("New exercise should be in the plan", state.exercisesInPlan.any { it.name == newExerciseName })
        assertEquals(1, state.exercisesInPlan.size)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}