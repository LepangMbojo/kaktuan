package com.example.kaktuan.supabase

import com.example.kaktuan.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClient {
    // 1. Definisikan konfigurasi JSON yang "tahan banting"
    private val jsonConfig = Json {
        ignoreUnknownKeys = true   // Mengabaikan kolom baru di DB yang belum ada di model
        isLenient = true           // Lebih toleran terhadap format JSON yang tidak standar
        coerceInputValues = true   // PENTING: Mengubah null dari DB menjadi nilai default (misal 0)
    }

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)

        // 2. Pasang serializer ini ke dalam client
        defaultSerializer = KotlinXSerializer(jsonConfig)
    }
}