package com.example.kaktuan.model

data class User(
    val uid: String = "",
    val email: String = "",

    val name: String = "",
    val age: Int = 0,
    val gender: String = "",

    val weight: Double = 0.0,
    val height: Double = 0.0,

    val healthConditions: List<String> = emptyList(),

    val profileCompleted: Boolean = false
)