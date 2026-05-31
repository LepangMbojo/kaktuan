package com.example.kaktuan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.ui.auth.LoginActivity
import com.example.kaktuan.ui.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {

            startActivity(
                Intent(this, HomeActivity::class.java)
            )

        } else {

            startActivity(
                Intent(this, LoginActivity::class.java)
            )

        }

        finish()
    }
}