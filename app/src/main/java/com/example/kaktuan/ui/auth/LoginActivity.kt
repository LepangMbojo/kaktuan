package com.example.kaktuan.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityLoginBinding
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.example.kaktuan.ui.home.HomeActivity
import com.example.kaktuan.ui.profile.BiodataActivity // Diubah menjadi BiodataActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestoreHelper: FirestoreHelper

    // =========================
    // LAUNCHER GOOGLE SIGN-IN BARU
    // =========================
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign In gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase & Firestore Helper
        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        // =========================
        // GOOGLE SIGN IN CONFIG
        // =========================

        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // =========================
        // EMAIL LOGIN
        // =========================

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnLogin.isEnabled = false // Mencegah klik ganda
                loginUser(email, password)
            }
        }

        // =========================
        // GOOGLE LOGIN BUTTON
        // =========================

        binding.btnGoogleLogin.setOnClickListener {
            signInGoogle()
        }

        // =========================
        // REGISTER
        // =========================

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // =========================
    // EMAIL LOGIN FUNCTION
    // =========================

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        cekProfil(uid)
                    }
                } else {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "Login gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // =========================
    // GOOGLE SIGN IN
    // =========================

    private fun signInGoogle() {
        // Memanggil launcher baru, bukan startActivityForResult lagi
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // =========================
    // FIREBASE GOOGLE AUTH
    // =========================

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        cekProfil(uid)
                    }
                } else {
                    Toast.makeText(this, "Firebase Google Login gagal", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // =========================
    // CEK PROFIL FIRESTORE
    // =========================

    private fun cekProfil(uid: String) {
        Toast.makeText(this, "Mengecek data profil...", Toast.LENGTH_SHORT).show()

        firestoreHelper.checkUserExists(uid) { isExists ->
            if (isExists) {
                // Profil sudah ada, ke halaman utama
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // Profil kosong, ke pengisian biodata (Sudah diubah ke BiodataActivity)
                startActivity(Intent(this, BiodataActivity::class.java))
            }
            finish() // Menutup halaman login
        }
    }
}