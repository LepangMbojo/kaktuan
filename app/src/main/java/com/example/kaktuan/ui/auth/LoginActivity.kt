package com.example.kaktuan.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kaktuan.BuildConfig
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityLoginBinding
import com.example.kaktuan.supabase.SupabaseAuthHelper
import com.example.kaktuan.ui.home.HomeActivity
import com.example.kaktuan.ui.profile.BiodataActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authHelper: SupabaseAuthHelper

    // =========================
    // LAUNCHER GOOGLE SIGN-IN
    // =========================
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    supabaseGoogleLogin(token)
                } ?: Toast.makeText(this, "Token Google Kosong", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign In gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Helper Supabase
        authHelper = SupabaseAuthHelper()

        // =========================
        // GOOGLE SIGN IN CONFIG
        // =========================
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // =========================
        // KLIK TOMBOL LOGIN EMAIL
        // =========================
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        // =========================
        // KLIK TOMBOL GOOGLE
        // =========================
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // =========================
        // KLIK TOMBOL REGISTER
        // =========================
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Loading..."

        lifecycleScope.launch {
            val result = authHelper.loginWithEmail(email, password)

            result.onSuccess { uid ->
                cekProfil(uid)
            }.onFailure { exception ->
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = "Login"
                Toast.makeText(this@LoginActivity, "Login gagal: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun supabaseGoogleLogin(idToken: String) {
        lifecycleScope.launch {
            val result = authHelper.loginWithGoogleIdToken(idToken)

            result.onSuccess { uid ->
                cekProfil(uid)
            }.onFailure { exception ->
                Toast.makeText(this@LoginActivity, "Google Login gagal: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cekProfil(uid: String) {
        Toast.makeText(this, "Mengecek data profil...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val isExists = authHelper.checkProfileExists(uid)

            if (isExists) {
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
            } else {
                startActivity(Intent(this@LoginActivity, BiodataActivity::class.java))
            }
            finish()
        }
    }
}