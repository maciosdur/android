package com.example.coach.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PlayerRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("player_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlayers(players: List<Player>) {
        val json = gson.toJson(players)
        sharedPreferences.edit().putString("players_list", json).apply()
    }

    fun loadPlayers(): List<Player> {
        val json = sharedPreferences.getString("players_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<Player>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
}