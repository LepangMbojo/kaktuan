package com.example.kaktuan.ui.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.kaktuan.R
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Sembunyikan Navbar
        val navContainer = requireActivity().findViewById<View>(R.id.navContainer)
        navContainer?.visibility = View.GONE

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
                val scan = SupabaseClient.client.postgrest["history"]
                    .select {
                        filter {
                            eq("id", currentScanId)
                            eq("user_id", uid)
                        }
                    }.decodeSingleOrNull<ScanHistory>()

                withContext(Dispatchers.Main) {
                    if (scan != null) {
                        // 1. Teks Dasar & Waktu
                        binding.etDetailNama.setText(scan.productName)
                        val score = scan.healthScore ?: 0
                        binding.tvDetailScore.setText(score.toString())
                        binding.tvDetailScore.text = score.toString()

                        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val date = Date(scan.timestamp)
                        binding.tvDetailWaktu.text = sdf.format(date)

                        // 2. Foto
                        binding.ivDetailPhoto.visibility = View.VISIBLE
                        if (!scan.photoUrl.isNullOrEmpty()) {
                            Glide.with(this@DetailScanFragment)
                                .load(scan.photoUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .centerCrop()
                                .into(binding.ivDetailPhoto)
                        } else {
                            binding.ivDetailPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                        }

                        // 3. Warna Status Lencana
                        if (score < 40) {
                            binding.cvStatusIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                            binding.tvDetailScore.setTextColor(Color.parseColor("#EF4444"))
                            binding.cvDetailBadgeStatus.setCardBackgroundColor(Color.parseColor("#EF4444"))
                            binding.tvDetailStatusText.text = "Bahaya"
                        } else {
                            binding.cvStatusIconBg.setCardBackgroundColor(Color.parseColor("#E6F4F1"))
                            binding.tvDetailScore.setTextColor(Color.parseColor("#38C6A5"))
                            binding.cvDetailBadgeStatus.setCardBackgroundColor(Color.parseColor("#38C6A5"))
                            binding.tvDetailStatusText.text = "Aman"
                        }

                        // ==========================================
                        // 4. PARSING KOTLINX.SERIALIZATION JSON
                        // ==========================================
                        val jsonObj = scan.analisisKesehatan
                        if (jsonObj != null) {
                            try {
                                // -- A. Kesimpulan & Peringatan --
                                val analysisObj = jsonObj["analysis"]?.jsonObject
                                if (analysisObj != null) {
                                    binding.tvKesimpulan.text = analysisObj["conclusion"]?.jsonPrimitive?.contentOrNull ?: "Kesimpulan tidak ditemukan."

                                    val warningsArray = analysisObj["specific_warnings"]?.jsonArray
                                    if (warningsArray != null && warningsArray.isNotEmpty()) {
                                        val warningsList = warningsArray.map { "• ${it.jsonPrimitive.contentOrNull}" }
                                        binding.tvPeringatan.text = warningsList.joinToString("\n\n")
                                    } else {
                                        binding.tvPeringatan.text = "Tidak ada peringatan khusus."
                                    }
                                } else {
                                    binding.tvKesimpulan.text = scan.recommendation
                                    binding.tvPeringatan.text = "Data analisis tidak lengkap."
                                }

                                // -- B. Nilai Gizi --
                                val nutritionObj = jsonObj["nutrition"]?.jsonObject
                                if (nutritionObj != null) {
                                    // Fungsi bantuan untuk mengambil nilai & unit
                                    fun getNutrient(key: String): String {
                                        val nutObj = nutritionObj[key]?.jsonObject
                                        val value = nutObj?.get("value")?.jsonPrimitive?.contentOrNull
                                        val unit = nutObj?.get("unit")?.jsonPrimitive?.contentOrNull
                                        return if (value != null && unit != null) "$value $unit" else "-"
                                    }

                                    binding.tvGiziEnergi.text = getNutrient("total_energy")
                                    binding.tvGiziGula.text = getNutrient("sugar")
                                    binding.tvGiziLemak.text = getNutrient("fat")
                                    binding.tvGiziKarbo.text = getNutrient("carbohydrate")
                                    binding.tvGiziProtein.text = getNutrient("protein")
                                    binding.tvGiziGaram.text = getNutrient("sodium")
                                }

                                // -- C. Komposisi --
                                val ingredientsArray = jsonObj["ingredients"]?.jsonArray
                                if (ingredientsArray != null && ingredientsArray.isNotEmpty()) {
                                    val ingredientsList = ingredientsArray.mapNotNull { it.jsonPrimitive.contentOrNull }
                                    binding.tvKomposisi.text = ingredientsList.joinToString(", ")
                                } else {
                                    binding.tvKomposisi.text = "Data komposisi tidak tersedia."
                                }

                            } catch (e: Exception) {
                                binding.tvKesimpulan.text = scan.recommendation
                                binding.tvPeringatan.text = "Gagal memproses data analisis."
                            }
                        } else {
                            binding.tvKesimpulan.text = scan.recommendation ?: "Belum ada analisis."
                            binding.tvPeringatan.text = "-"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat detail: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun simpanPerubahan() {
        val newName = binding.etDetailNama.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(context, "Nama produk tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpanChanges.isEnabled = false
        binding.btnSimpanChanges.text = "Menyimpan..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val updateData = buildJsonObject {
                    put("product_name", newName)
                }

                SupabaseClient.client.postgrest["history"].update(updateData) {
                    filter { eq("id", currentScanId) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Nama produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSimpanChanges.isEnabled = true
                    binding.btnSimpanChanges.text = "Simpan Perubahan Nama"
                    Toast.makeText(context, "Gagal memperbarui: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // Munculkan kembali Navbar
        val navContainer = requireActivity().findViewById<View>(R.id.navContainer)
        navContainer?.visibility = View.VISIBLE
    }
}