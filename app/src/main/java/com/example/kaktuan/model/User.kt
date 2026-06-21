package com.example.kaktuan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // <-- WAJIB ADA
data class User(
    @SerialName("id")
    val uid: String = "",

    val email: String = "",

    @SerialName("display_name")
    val name: String = "",

    val age: Int = 0,
    val gender: String = "",
    val height: Double = 0.0,
    val weight: Double = 0.0,

    @SerialName("health_conditions")
    val healthConditions: List<String> = emptyList(),

    @SerialName("profile_picture_url")
    val profilePictureUrl: String? = null,

    @SerialName("profile_completed")
    val profileCompleted: Boolean = false
)