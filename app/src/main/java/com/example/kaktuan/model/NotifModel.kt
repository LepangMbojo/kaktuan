package com.example.kaktuan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotifModel(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",

    @SerialName("title")
    val title: String = "Info",

    @SerialName("message")
    val message: String = "",

    @SerialName("created_at")
    val time: String = "",

    @SerialName("is_read")
    val isRead: Boolean = false,

    // Jembatan opsional ke detail makanan
    @SerialName("scan_id")
    val scanId: String? = null
)