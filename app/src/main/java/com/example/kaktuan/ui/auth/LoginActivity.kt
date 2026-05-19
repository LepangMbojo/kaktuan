package com.example.kaktuan.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityLoginBinding
import com.example.kaktuan.ui.home.HomeActivity
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

    private val RC_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Auth
        auth = FirebaseAuth.getInstance()

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

                Toast.makeText(
                    this,
                    "Email dan Password wajib diisi",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

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

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    // =========================
    // EMAIL LOGIN FUNCTION
    // =========================

    private fun loginUser(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) {

                if (it.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this, HomeActivity::class.java)
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Login gagal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // =========================
    // GOOGLE SIGN IN
    // =========================

    private fun signInGoogle() {

        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // =========================
    // GOOGLE RESULT
    // =========================

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {

                val account = task.getResult(ApiException::class.java)

                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {

                Toast.makeText(
                    this,
                    "Google Sign In gagal",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================
    // FIREBASE GOOGLE AUTH
    // =========================

    private fun firebaseAuthWithGoogle(idToken: String) {

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) {

                if (it.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login Google berhasil",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(
                        Intent(this, HomeActivity::class.java)
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Firebase Google Login gagal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}