package com.example.coach.data

import com.example.coach.R

object AvatarRepository {
    private val avatars = listOf(
        R.drawable.img1,
        R.drawable.img2,
        R.drawable.img3,
        R.drawable.img4,
        R.drawable.img5,
    )

    fun getRandomAvatar(): Int {
        return avatars.random()
    }
}