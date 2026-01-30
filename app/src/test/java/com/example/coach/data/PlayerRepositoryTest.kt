package com.example.coach.data

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class PlayerRepositoryTest {

    private lateinit var playerDao: PlayerDao

    private lateinit var playerRepository: PlayerRepository


    @Before
    fun setup() {
        playerDao = mockk(relaxed = true)
        playerRepository = PlayerRepository(playerDao)
    }

    @Test
    fun `addPlayer - when called, it should call insert on the DAO`() = runTest {
        val testPlayer = Player(id = "1", firstName = "Test", lastName = "Player", birthYear = 2000, avatarResId = 0)

        playerRepository.addPlayer(testPlayer)

        coVerify(exactly = 1) { playerDao.insert(testPlayer) }
    }

    @Test
    fun `updatePlayer - when called, it should call update on the DAO`() = runTest {
        val testPlayer = Player(id = "1", firstName = "Test", lastName = "Player", birthYear = 2000, avatarResId = 0)

        playerRepository.updatePlayer(testPlayer)

        coVerify(exactly = 1) { playerDao.update(testPlayer) }
    }

    @Test
    fun `deletePlayer - when called, it should call delete on the DAO`() = runTest {
        val testPlayer = Player(id = "1", firstName = "Test", lastName = "Player", birthYear = 2000, avatarResId = 0)

        playerRepository.deletePlayer(testPlayer)

        coVerify(exactly = 1) { playerDao.delete(testPlayer) }
    }

    @Test
    fun `getAllPlayers - when called, it should call getAll on the DAO`() = runTest {
        playerRepository.getAllPlayers()

        coVerify(exactly = 1) { playerDao.getAll() }
    }
}