package com.example.kaktuan.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kaktuan.ScanActivity
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bottom Navigation Click
        binding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {

                    Toast.makeText(
                        this,
                        "Home",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                R.id.nav_scan -> {

                    startActivity(
                        Intent(this, ScanActivity::class.java)
                    )

                    true
                }

                R.id.nav_history -> {

                    Toast.makeText(
                        this,
                        "History",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                R.id.nav_profile -> {

                    Toast.makeText(
                        this,
                        "Profile",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                else -> false
            }
        }
    }
}