package com.example.kaktuan.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kaktuan.BuildConfig
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

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    supabaseGoogleLogin(token)
                } ?: showPopup("Peringatan", "Token Google Kosong", false)
            } catch (e: ApiException) {
                showPopup("Gagal", "Google Sign In batal atau gagal.", false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authHelper = SupabaseAuthHelper()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showPopup("Perhatian", "Email dan Password wajib diisi!", false)
            } else {
                loginUser(email, password)
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        setLoadingState(true)

        lifecycleScope.launch {
            val result = authHelper.loginWithEmail(email, password)

            result.onSuccess { uid ->
                setLoadingState(false)
                showPopup("Berhasil", "Login sukses! Memuat data Anda...", true) {
                    cekProfil(uid)
                }
            }.onFailure { exception ->
                setLoadingState(false)
                showPopup("Login Gagal", exception.message ?: "Terjadi kesalahan", false)
            }
        }
    }

    private fun supabaseGoogleLogin(idToken: String) {
        setLoadingState(true)
        lifecycleScope.launch {
            val result = authHelper.loginWithGoogleIdToken(idToken)

            result.onSuccess { uid ->
                setLoadingState(false)
                showPopup("Berhasil", "Google Login sukses!", true) {
                    cekProfil(uid)
                }
            }.onFailure { exception ->
                setLoadingState(false)
                showPopup("Login Gagal", exception.message ?: "Terjadi kesalahan", false)
            }
        }
    }

    private fun cekProfil(uid: String) {
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

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogleLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "Loading..." else "Login"
    }

    // Fungsi Utama untuk memunculkan Pop-up Informasi
    private fun showPopup(title: String, message: String, isSuccess: Boolean, onOkClicked: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(false) // Tidak bisa ditutup dengan mengetuk layar luar

        // Pilih ikon berdasarkan status
        val icon = if (isSuccess) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert
        builder.setIcon(icon)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            onOkClicked?.invoke() // Jalankan perintah navigasi jika ada
        }
        builder.show()
    }
}