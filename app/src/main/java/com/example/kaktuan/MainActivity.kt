package com.example.kaktuan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.ui.auth.LoginActivity
import com.example.kaktuan.ui.home.HomeActivity
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val currentSession = SupabaseClient.client.auth.currentSessionOrNull()

            if (currentSession != null) {
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
            } else {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }

            finish()
        }
    }
}