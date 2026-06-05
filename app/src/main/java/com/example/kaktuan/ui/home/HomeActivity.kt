package com.example.kaktuan.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment // Tambahkan import Fragment
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ActivityHomeBinding
import com.example.kaktuan.ui.profile.ProfileActivity // Import ProfileFragment yang baru dibuat
import com.example.kaktuan.ui.scan.ScanActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bottom Navigation Click
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    // TODO: Nanti buat HomeFragment() seperti ProfileFragment
                    true
                }
                R.id.nav_scan -> {
                    // ScanActivity tetap menggunakan Intent karena butuh layar penuh untuk kamera
                    startActivity(Intent(this, ScanActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, "History", Toast.LENGTH_SHORT).show()
                    // TODO: Nanti buat HistoryFragment()
                    true
                }
                R.id.nav_profile -> {
                    // Panggil fungsi untuk memuat ProfileFragment ke layar
                    replaceFragment(ProfileActivity())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi khusus untuk membongkar-pasang Fragment di dalam FrameLayout
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameContainer, fragment)
            .commit()
    }
}