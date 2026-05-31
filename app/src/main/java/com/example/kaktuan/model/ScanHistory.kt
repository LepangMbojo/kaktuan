package com.example.kaktuan.model

data class ScanHistory(
    val scanId: String = "",
    val userId: String = "",

    val productName: String = "",
    val ocrText: String = "",

    val recommendation: String = "",

    val healthScore: Int = 0,

    val timestamp: Long = 0
)