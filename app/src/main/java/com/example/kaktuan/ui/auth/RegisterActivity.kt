package com.example.kaktuan.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
                Toast.makeText(this, "Semua field wajib diisi", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(email, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser(email: String, password: String) {
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Loading..."

        lifecycleScope.launch {
            val result = authHelper.registerWithEmail(email, password)

            result.onSuccess {
                Toast.makeText(this@RegisterActivity, "Register berhasil, silakan login", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                finish()
            }.onFailure { exception ->
                binding.btnRegister.isEnabled = true
                binding.btnRegister.text = "Register"
                Toast.makeText(this@RegisterActivity, "Register gagal: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}