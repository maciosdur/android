package com.example.coach.data

import java.util.UUID

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val birthYear: Int
)