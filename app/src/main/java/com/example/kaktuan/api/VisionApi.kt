package com.example.kaktuan.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// --- DATA MODELS ---
data class VisionRequest(val requests: List<AnnotateRequest>)
data class AnnotateRequest(val image: ImageSource, val features: List<Feature>)
data class ImageSource(val content: String)
data class Feature(val type: String = "TEXT_DETECTION")

data class VisionResponse(val responses: List<TextAnnotationResponse>)
data class TextAnnotationResponse(val fullTextAnnotation: FullTextAnnotation?)
data class FullTextAnnotation(val text: String)

// --- RETROFIT INTERFACE ---
interface VisionApiService {
    @POST("v1/images:annotate")
    fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: VisionRequest
    ): Call<VisionResponse>
}

// --- RETROFIT CLIENT OBJECT ---
object RetrofitClient {
    private const val BASE_URL = "https://vision.googleapis.com/"

    val instance: VisionApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(VisionApiService::class.java)
    }
}
