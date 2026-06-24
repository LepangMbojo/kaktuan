package com.example.kaktuan.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityBiodataBinding
import com.example.kaktuan.model.User
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.supabase.SupabaseDatabaseHelper
import com.example.kaktuan.ui.home.HomeActivity
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import androidx.activity.OnBackPressedCallback
import android.view.View

class BiodataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiodataBinding
    private lateinit var databaseHelper: SupabaseDatabaseHelper

    private var isEditMode = false
    private var selectedImageUri: Uri? = null
    private var existingProfilePicUrl: String = ""

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivProfileSetup.imageTintList = null
            binding.ivProfileSetup.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiodataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = SupabaseDatabaseHelper()
        isEditMode = intent.getBooleanExtra("IS_EDIT", false)

        setupClickListeners()

        if (isEditMode) {
            muatDataLama()
        }
    }

    private fun setupClickListeners() {
        val isNewUser = intent.getBooleanExtra("IS_NEW_USER", false)

        if (isNewUser) {
            binding.btnBack.visibility = View.INVISIBLE

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Toast.makeText(this@BiodataActivity, "Silakan lengkapi biodata Anda untuk melanjutkan.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            binding.btnBack.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener { finish() }
        }

        binding.btnChangePhoto.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnSimpan.setOnClickListener { simpanBiodata() }
    }

    private fun muatDataLama() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Memuat data..."

        databaseHelper.getUserProfile(uid) { user ->
            if (user != null) {
                binding.etNama.setText(user.name)
                binding.etUmur.setText(user.age.toString())
                binding.etTinggi.setText(user.height.toString())
                binding.etBerat.setText(user.weight.toString())

                if (user.gender == "Laki-laki") binding.rbLakiLaki.isChecked = true
                else if (user.gender == "Perempuan") binding.rbPerempuan.isChecked = true

                binding.etPenyakit.setText(user.healthConditions.joinToString(", "))

                existingProfilePicUrl = user.profilePictureUrl ?: ""

                if (existingProfilePicUrl.isEmpty()) {
                    val metadata = session.user?.userMetadata
                    existingProfilePicUrl = metadata?.get("avatar_url")?.toString()?.replace("\"", "")
                        ?: metadata?.get("picture")?.toString()?.replace("\"", "")
                                ?: ""
                }

                if (existingProfilePicUrl.isNotEmpty()) {
                    binding.ivProfileSetup.imageTintList = null
                    Glide.with(this)
                        .load(existingProfilePicUrl)
                        .circleCrop()
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user) // Tambahkan error handling
                        .into(binding.ivProfileSetup)
                }

                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "Simpan Profil"
            } else {
                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "Simpan Profil"
                Toast.makeText(this, "Gagal memuat profil lama", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun simpanBiodata() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return
        val email = session?.user?.email ?: ""

        val name = binding.etNama.text.toString().trim()
        val ageStr = binding.etUmur.text.toString().trim()
        val heightStr = binding.etTinggi.text.toString().trim()
        val weightStr = binding.etBerat.text.toString().trim()
        val penyakitStr = binding.etPenyakit.text.toString().trim()

        if (name.isEmpty() || ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi nama, umur, tinggi, dan berat badan", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (binding.rgJenisKelamin.checkedRadioButtonId) {
            binding.rbLakiLaki.id -> "Laki-laki"
            binding.rbPerempuan.id -> "Perempuan"
            else -> {
                Toast.makeText(this, "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val age = ageStr.toIntOrNull() ?: 0
        val height = heightStr.toDoubleOrNull() ?: 0.0
        val weight = weightStr.toDoubleOrNull() ?: 0.0
        val healthConditions = if (penyakitStr.isNotEmpty()) penyakitStr.split(",").map { it.trim() } else emptyList()

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Menyimpan..."

        if (selectedImageUri != null) {
            uploadFotoDanSimpanData(uid, email, name, age, gender, height, weight, healthConditions)
        } else {
            simpanKeSupabase(uid, email, name, age, gender, height, weight, healthConditions, existingProfilePicUrl)
        }
    }

    private fun uploadFotoDanSimpanData(
        uid: String, email: String, name: String, age: Int, gender: String,
        height: Double, weight: Double, healthConditions: List<String>
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Konversi URI gambar menjadi ByteArray
                val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                val byteArray = inputStream?.readBytes()

                if (byteArray != null) {
                    val fileName = "${uid}_${UUID.randomUUID()}.jpg"
                    val bucket = SupabaseClient.client.storage["avatars"] // Mengarah ke bucket yang benar

                    // Upload ke Supabase
                    bucket.upload(fileName, byteArray)

                    // Dapatkan URL Publik
                    val newProfilePicUrl = bucket.publicUrl(fileName)

                    withContext(Dispatchers.Main) {
                        simpanKeSupabase(uid, email, name, age, gender, height, weight, healthConditions, newProfilePicUrl)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BiodataActivity, "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan Profil"
                }
            }
        }
    }

    private fun simpanKeSupabase(
        uid: String, email: String, name: String, age: Int, gender: String,
        height: Double, weight: Double, healthConditions: List<String>, profilePicUrl: String
    ) {
        val userProfile = User(
            uid = uid,
            email = email,
            name = name,
            age = age,
            gender = gender,
            weight = weight,
            height = height,
            healthConditions = healthConditions,
            profilePictureUrl = profilePicUrl,
            profileCompleted = true
        )

        databaseHelper.saveUserProfile(userProfile) { success, errorMessage ->
            if (success) {
                Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                if (isEditMode) {
                    finish()
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
            } else {
                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "Simpan Profil"
                Toast.makeText(this, "Gagal menyimpan biodata: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}