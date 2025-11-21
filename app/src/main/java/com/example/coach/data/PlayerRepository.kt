package com.example.coach.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {

    fun getAllPlayers(): Flow<List<Player>> {
        return playerDao.getAll()
    }

    suspend fun getPlayerById(id: String): Player? {
        return playerDao.getById(id)
    }

    suspend fun addPlayer(player: Player) {
        playerDao.insert(player)
    }

    suspend fun updatePlayer(player: Player) {
        playerDao.update(player)
    }

    suspend fun deletePlayer(player: Player) {
        playerDao.delete(player)
    }
}