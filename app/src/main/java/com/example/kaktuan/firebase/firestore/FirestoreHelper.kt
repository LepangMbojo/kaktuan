package com.example.kaktuan.firebase.firestore

import android.util.Log
import com.example.kaktuan.model.User // Pastikan Anda sudah membuat Data Class User
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    fun getDatabase() = db

    // =========================
    // CEK APAKAH PROFIL SUDAH ADA
    // =========================
    fun checkUserExists(uid: String, onResult: (Boolean) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Mengembalikan nilai true jika dokumen ada, false jika kosong
                if (document != null && document.exists()) {
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Error mengecek profil: ", e)
                onResult(false) // Jika gagal koneksi, anggap saja belum ada agar aman
            }
    }

    // =========================
    // SIMPAN PROFIL BARU KE FIRESTORE
    // =========================
    fun saveUserProfile(user: User, onComplete: (Boolean, String?) -> Unit) {
        if (user.uid.isEmpty()) {
            onComplete(false, "Terjadi kesalahan: UID tidak ditemukan.")
            return
        }

        // Firestore otomatis akan mengubah Data Class User menjadi format JSON
        db.collection("users").document(user.uid).set(user)
            .addOnSuccessListener {
                Log.d("FirestoreHelper", "Profil berhasil disimpan untuk UID: ${user.uid}")
                onComplete(true, null) // Parameter pertama true (sukses), error message null
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Gagal menyimpan profil: ", e)
                onComplete(false, e.message)
            }
    }

    fun getUserProfile(uid: String, onComplete: (User?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Otomatis mengubah JSON dari Firestore menjadi Objek Data Class User
                    val user = document.toObject(User::class.java)
                    onComplete(user)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreHelper", "Gagal mengambil profil: ", e)
                onComplete(null)
            }
    }
}

