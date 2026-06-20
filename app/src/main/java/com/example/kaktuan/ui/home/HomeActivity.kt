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
import com.example.kaktuan.ui.scan.ScanFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Tampilkan Dashboard secara otomatis
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        // 2. Satu-satunya Listener untuk navigasi
        binding.bottomNavigation.setOnItemSelectedListener { item ->

            // Pilih fragmen berdasarkan ID
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_scan -> ScanFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }

            // Jalankan transaksi dengan animasi fade
            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.frameContainer, selectedFragment)
                    .commit()
                true
            } else {
                false
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