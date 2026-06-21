package com.example.kaktuan.supabase

import android.util.Log
import com.example.kaktuan.model.ScanHistory
import com.example.kaktuan.model.User
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseDatabaseHelper {

    private val supabase = SupabaseClient.client

    // =========================
    // CEK APAKAH PROFIL SUDAH ADA
    // =========================
    fun checkUserExists(uid: String, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Mencari data di tabel 'users' berdasarkan id
                val result = supabase.postgrest["users"]
                    .select { filter { eq("id", uid) } }
                    .data

                withContext(Dispatchers.Main) {
                    onResult(result != "[]") // Jika bukan array kosong, berarti ada
                }
            } catch (e: Exception) {
                Log.e("SupabaseDB", "Error mengecek profil: ", e)
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // =========================
    // SIMPAN PROFIL BARU KE SUPABASE
    // =========================
    fun saveUserProfile(user: User, onComplete: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Upsert: Update jika sudah ada, Insert jika baru
                supabase.postgrest["users"].upsert(user)

                withContext(Dispatchers.Main) {
                    Log.d("SupabaseDB", "Profil berhasil disimpan")
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e("SupabaseDB", "Gagal menyimpan profil: ", e)
                withContext(Dispatchers.Main) { onComplete(false, e.message) }
            }
        }
    }

    // =========================
    // AMBIL PROFIL USER
    // =========================
    fun getUserProfile(uid: String, onComplete: (User?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = supabase.postgrest["users"]
                    .select { filter { eq("id", uid) } }
                    .decodeSingleOrNull<User>()

                withContext(Dispatchers.Main) { onComplete(user) }
            } catch (e: Exception) {
                Log.e("SupabaseDB", "Gagal mengambil profil: ", e)
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    // =========================
    // AMBIL RIWAYAT SCAN USER
    // =========================
    fun getScanHistory(uid: String, onComplete: (List<ScanHistory>?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Supabase menggunakan tabel terpisah 'history' dengan foreign key 'user_id'
                val list = supabase.postgrest["history"]
                    .select { filter { eq("user_id", uid) } }
                    .decodeList<ScanHistory>()

                withContext(Dispatchers.Main) { onComplete(list) }
            } catch (e: Exception) {
                Log.e("SupabaseDB", "Gagal mengambil riwayat scan: ", e)
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }
}