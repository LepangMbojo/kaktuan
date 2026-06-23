package com.example.kaktuan.ui.notifikasi

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kaktuan.databinding.ActivityNotificationBinding
import com.example.kaktuan.model.NotifModel
import com.example.kaktuan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.example.kaktuan.R
import com.example.kaktuan.ui.history.DetailScanFragment

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)

        tarikDataNotifikasiAsli()
    }

    private fun tarikDataNotifikasiAsli() {
        val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id

        if (uid == null) {
            Toast.makeText(this, "Sesi habis, silakan login ulang", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = SupabaseClient.client.postgrest["notifications"]
                    .select {
                        filter { eq("user_id", uid) }
                    }.decodeList<NotifModel>()

                val daftarNotifRapih = response.map { notifMentah ->
                    notifMentah.copy(time = formatWaktu(notifMentah.time))
                }.reversed()

                withContext(Dispatchers.Main) {
                    if (daftarNotifRapih.isEmpty()) {
                        Toast.makeText(this@NotificationActivity, "Belum ada notifikasi", Toast.LENGTH_SHORT).show()
                    } else {
                        // MODIFIKASI DI SINI: Terima lemparan klik dari Adapter
                        binding.rvNotifications.adapter = NotificationAdapter(daftarNotifRapih) { notifDiklik ->
                            if (!notifDiklik.isRead) {
                                tandaiNotifikasiTerbaca(notifDiklik.id)
                                // Tambahkan baris ini agar langsung buka detail setelah klik pertama!
                                jikaAdaDetailBukaHalaman(notifDiklik.scanId)
                            } else {
                                jikaAdaDetailBukaHalaman(notifDiklik.scanId)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("NotifActivity", "Gagal tarik notifikasi: ${e.message}")
            }
        }
    }

    // Fungsi baru untuk mengubah status di Supabase secara real-time
    private fun tandaiNotifikasiTerbaca(notifId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Eksekusi update kolom is_read menjadi true berdasarkan id notifikasinya
                SupabaseClient.client.postgrest["notifications"].update({
                    set("is_read", true)
                }) {
                    filter { eq("id", notifId) }
                }

                withContext(Dispatchers.Main) {
                    // Refresh data di layar agar titik merah langsung menghilang secara visual
                    tarikDataNotifikasiAsli()
                }
            } catch (e: Exception) {
                Log.e("NotifActivity", "Gagal mengubah status baca: ${e.message}")
            }
        }
    }

    // Fungsi opsional untuk mengarahkan pengguna ke halaman detail makanan
    // Di NotificationActivity.kt

    private fun jikaAdaDetailBukaHalaman(scanId: String?) {
        if (scanId.isNullOrEmpty()) return

        // MUNCULKAN FRAME LAYOUT-NYA
        binding.fragmentContainer.visibility = View.VISIBLE

        val fragment = DetailScanFragment.newInstance(scanId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun formatWaktu(waktuRaw: String): String {
        if (waktuRaw.isEmpty()) return "Baru saja"
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(waktuRaw)
            val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
            formatter.timeZone = TimeZone.getDefault()
            if (date != null) formatter.format(date) else "Baru saja"
        } catch (e: Exception) {
            "Baru saja"
        }
    }
}