package com.example.kaktuan.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.databinding.ActivityRegisterBinding
import com.example.kaktuan.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Register Button
        binding.btnRegister.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Validation
            if (email.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()
            ) {

                Toast.makeText(
                    this,
                    "Semua field wajib diisi",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (password.length < 6) {

                Toast.makeText(
                    this,
                    "Password minimal 6 karakter",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (password != confirmPassword) {

                Toast.makeText(
                    this,
                    "Password tidak cocok",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                registerUser(email, password)
            }
        }

        // Back to Login
        binding.tvLogin.setOnClickListener {

            finish()
        }
    }

    // REGISTER FUNCTION

    private fun registerUser(email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {

                if (it.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Register berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Pindah ke Home
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        it.exception?.message ?: "Register gagal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}