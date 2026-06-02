package com.example.kaktuan.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.databinding.ActivityProfileBinding // Pastikan import binding ini benar
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.example.kaktuan.model.User
import com.example.kaktuan.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        binding.btnSimpan.setOnClickListener {
            simpanBiodata()
        }
    }

    private fun simpanBiodata() {
        // Ambil UID dan Email dari user yang sedang login
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""

        // Ambil teks dari kolom input
        val name = binding.etNama.text.toString().trim()
        val ageStr = binding.etUmur.text.toString().trim()
        val heightStr = binding.etTinggi.text.toString().trim()
        val weightStr = binding.etBerat.text.toString().trim()
        val penyakitStr = binding.etPenyakit.text.toString().trim()

        // 1. Validasi Input Kosong
        if (name.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi nama, umur, tinggi, dan berat badan", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Mendapatkan Jenis Kelamin dari RadioGroup
        val gender = when (binding.rgJenisKelamin.checkedRadioButtonId) {
            binding.rbLakiLaki.id -> "Laki-laki"
            binding.rbPerempuan.id -> "Perempuan"
            else -> {
                Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 3. Konversi tipe data String ke Int/Double sesuai Data Class
        val age = ageStr.toIntOrNull() ?: 0
        val height = heightStr.toDoubleOrNull() ?: 0.0
        val weight = weightStr.toDoubleOrNull() ?: 0.0

        // 4. Memecah penyakit menjadi List<String> (jika dikosongkan, kirim list kosong)
        val healthConditions = if (penyakitStr.isNotEmpty()) {
            penyakitStr.split(",").map { it.trim() }
        } else {
            emptyList()
        }

        // 5. Bungkus semua data ke dalam Objek User
        val userProfile = User(
            uid = uid,
            email = email,
            name = name,
            age = age,
            gender = gender,
            weight = weight,
            height = height,
            healthConditions = healthConditions,
            profileCompleted = true // Menandakan user sudah mengisi form
        )

        // Nonaktifkan tombol sementara agar tidak diklik dua kali
        binding.btnSimpan.isEnabled = false
        Toast.makeText(this, "Menyimpan data...", Toast.LENGTH_SHORT).show()

        // 6. Kirim ke Firestore
        firestoreHelper.saveUserProfile(userProfile) { success, errorMessage ->
            if (success) {
                Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()

                // Lempar ke HomeActivity
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                binding.btnSimpan.isEnabled = true
                Toast.makeText(this, "Gagal menyimpan: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}