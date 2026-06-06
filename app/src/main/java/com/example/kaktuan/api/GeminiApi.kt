package com.example.kaktuan.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {

    @POST(
        "v1beta/models/gemini-2.5-flash:generateContent"
    )
    fun generateContent(

        @Query("key")
        apiKey: String,

        @Body
        request: GeminiRequest

    ): Call<GeminiResponse>
}