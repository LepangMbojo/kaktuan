package com.example.kaktuan.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {

    @POST(
        "v1beta/models/gemini-3.1-flash-lite:generateContent"
    )

    fun generateContent(

        @Query("key")
        apiKey: String,

        @Body
        request: GeminiRequest

    ): Call<GeminiResponse>
}