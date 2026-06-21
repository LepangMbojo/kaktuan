package com.example.kaktuan.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.kaktuan.databinding.FragmentDetailScanBinding
import com.example.kaktuan.model.ScanHistory
import com.example.kaktuan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailScanFragment : Fragment() {
    private var _binding: FragmentDetailScanBinding? = null
    private val binding get() = _binding!!
    private var currentScanId: String = ""

    companion object {
        fun newInstance(scanId: String) = DetailScanFragment().apply {
            arguments = Bundle().apply { putString("scan_id", scanId) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentScanId = arguments?.getString("scan_id") ?: ""

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSimpanChanges.setOnClickListener { simpanPerubahan() }

        muatDetailScan()
    }

    private fun muatDetailScan() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Mencari data history spesifik berdasarkan ID scan dan ID user
                val scan = SupabaseClient.client.postgrest["history"]
                    .select {
                        filter {
                            eq("id", currentScanId)
                            eq("user_id", uid)
                        }
                    }.decodeSingleOrNull<ScanHistory>()

                withContext(Dispatchers.Main) {
                    if (scan != null) {
                        binding.etDetailNama.setText(scan.productName)

                        // GANTI BAGIAN INI:
                        val score = scan.healthScore ?: 0 // Jika null, gunakan 0
                        binding.etDetailSkor.setText(score.toString())
                        binding.tvDetailScore.text = score.toString()
                        binding.tvDetailRekomendasi.text = scan.recommendation

                        // Logika warna status
                        if (score < 40) { // Sekarang 'score' adalah Int, aman dari null
                            binding.tvDetailIcon.text = "😨"
                            binding.cvStatusIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                            binding.tvDetailScore.setTextColor(Color.parseColor("#EF4444"))
                        } else {
                            binding.tvDetailIcon.text = "😊"
                            binding.cvStatusIconBg.setCardBackgroundColor(Color.parseColor("#E6F4F1"))
                            binding.tvDetailScore.setTextColor(Color.parseColor("#38C6A5"))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun simpanPerubahan() {
        val newName = binding.etDetailNama.text.toString().trim()
        val newScoreText = binding.etDetailSkor.text.toString()
        val newScore = newScoreText.toIntOrNull() ?: 0

        if (newName.isEmpty()) {
            Toast.makeText(context, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpanChanges.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Membangun data JSON spesifik untuk kolom yang ingin diubah
                val updateData = buildJsonObject {
                    put("product_name", newName)
                    put("health_score", newScore)
                }

                // Memperbarui data di tabel history
                SupabaseClient.client.postgrest["history"].update(updateData) {
                    filter { eq("id", currentScanId) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSimpanChanges.isEnabled = true
                    Toast.makeText(context, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}