package com.example.kaktuan.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanHistory(
    @SerialName("id")
    val scanId: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("product_name")
    val productName: String = "",

    @SerialName("ocr_text")
    val ocrText: String = "",

    @SerialName("recommendation")
    val recommendation: String = "",

    @SerialName("health_score")
    val healthScore: Int? = 0,

    @SerialName("analisis_kesehatan")
    val analisisKesehatan: kotlinx.serialization.json.JsonObject? = null,

    @SerialName("photo_url") // <-- Tambahkan ini
    val photoUrl: String? = null,

    @SerialName("timestamp")
    val timestamp: Long = 0
)