package com.example.kaktuan.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityHomeBinding

// PASTIKAN IMPORT INI BENAR (Mengarah ke Fragment, bukan Activity)
import com.example.kaktuan.ui.history.HistoryFragment
import com.example.kaktuan.ui.profile.ProfileFragment
import com.example.kaktuan.ui.scan.ScanActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. TAMPILKAN DASHBOARD SECARA OTOMATIS SAAT APLIKASI DIBUKA
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            // Pastikan menu navigasi bawah juga menyorot tab Home
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        // 2. LOGIKA KLIK BOTTOM NAVIGATION
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Panggil HomeFragment, bukan Toast
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_scan -> {
                    // Scan tetap pakai Intent karena membuka kamera
                    startActivity(Intent(this, ScanActivity::class.java))
                    false // Return false agar highlight menu tetap di tab sebelumnya
                }
                R.id.nav_history -> {
                    // Panggil HistoryFragment, bukan Toast
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.nav_profile -> {
                    // Panggil ProfileFragment (Pastikan namanya Fragment, bukan Activity)
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi untuk menukar Fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}