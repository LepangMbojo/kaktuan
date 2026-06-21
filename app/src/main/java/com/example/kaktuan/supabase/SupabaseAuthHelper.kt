package com.example.kaktuan.supabase

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseAuthHelper {

    private val supabase = SupabaseClient.client

    // 1. REGISTER EMAIL
    suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. LOGIN EMAIL
    suspend fun loginWithEmail(email: String, password: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                val uid = supabase.auth.currentUserOrNull()?.id ?: throw Exception("UID tidak ditemukan")
                Result.success(uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. LOGIN GOOGLE
    suspend fun loginWithGoogleIdToken(idToken: String): Result<String> {
        return try {
            withContext(Dispatchers.IO) {
                supabase.auth.signInWith(IDToken) {
                    provider = Google
                    this.idToken = idToken
                }
                val uid = supabase.auth.currentUserOrNull()?.id ?: throw Exception("UID tidak ditemukan")
                Result.success(uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. CEK PROFIL EXIST
    suspend fun checkProfileExists(uid: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Mengecek apakah ada data di tabel 'users' dengan id yang sama
                val result = supabase.postgrest["users"]
                    .select { filter { eq("id", uid) } }
                    .data

                // Jika hasilnya bukan array kosong "[]", berarti profil sudah ada
                result != "[]"
            }
        } catch (e: Exception) {
            false
        }
    }
}