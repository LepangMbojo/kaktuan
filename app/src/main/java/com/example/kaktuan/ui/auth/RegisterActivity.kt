package com.example.kaktuan.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kaktuan.databinding.ActivityRegisterBinding
import com.example.kaktuan.supabase.SupabaseAuthHelper
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authHelper: SupabaseAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = SupabaseAuthHelper()

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showPopup("Perhatian", "Semua kolom formulir wajib diisi!", false)
            } else if (password.length < 6) {
                showPopup("Perhatian", "Password harus terdiri dari minimal 6 karakter.", false)
            } else if (password != confirmPassword) {
                showPopup("Perhatian", "Konfirmasi password tidak cocok dengan password yang Anda masukkan.", false)
            } else {
                registerUser(email, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser(email: String, password: String) {
        setLoadingState(true)

        lifecycleScope.launch {
            val result = authHelper.registerWithEmail(email, password)

            result.onSuccess {
                setLoadingState(false)
                showPopup("Registrasi Berhasil", "Akun berhasil dibuat! Silakan login.", true) {
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                }
            }.onFailure { exception ->
                setLoadingState(false)

                // Logika baru untuk menyederhanakan pesan kesalahan registrasi
                val errorMessage = when {
                    exception.message?.contains("already registered") == true ->
                        "Email ini sudah terdaftar. Silakan gunakan email lain atau login."
                    exception.message?.contains("weak password") == true ->
                        "Password terlalu lemah. Gunakan kombinasi yang lebih kuat."
                    else -> "Registrasi gagal: ${exception.message}"
                }
                showPopup("Registrasi Gagal", errorMessage, false)
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Memproses..." else "Register"
    }

    // Fungsi Utama untuk memunculkan Pop-up Informasi
    private fun showPopup(title: String, message: String, isSuccess: Boolean, onOkClicked: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(false)

        // Pilih ikon berdasarkan status
        val icon = if (isSuccess) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert
        builder.setIcon(icon)

        builder.setPositiveButton("Mengerti") { dialog, _ ->
            dialog.dismiss()
            onOkClicked?.invoke()
        }
        builder.show()
    }
}