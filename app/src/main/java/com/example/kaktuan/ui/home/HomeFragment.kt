package com.example.kaktuan.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kaktuan.BuildConfig
import com.example.kaktuan.api.Content
import com.example.kaktuan.api.GeminiRequest
import com.example.kaktuan.api.GeminiResponse
import com.example.kaktuan.api.GeminiRetrofitClient
import com.example.kaktuan.api.Part
import com.example.kaktuan.databinding.FragmentHomeBinding
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        muatDataUser()
    }

    private fun muatDataUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestoreHelper.getUserProfile(uid) { user ->
                if (user != null) {
                    // 1. Tampilkan Nama Panggilan
                    val namaPanggilan = user.name.split(" ").firstOrNull() ?: user.name
                    binding.tvNamaUser.text = namaPanggilan

                    if (user.name.isNotEmpty()) {
                        binding.tvInisialMini.text = user.name.take(1).uppercase()
                    }

                    // 2. Buat Prompt untuk Saran Makanan berdasarkan Biodata
                    // 2. Buat Prompt untuk Saran Makanan & Kesehatan berdasarkan Biodata
                    val infoFisik = "Gender: ${user.gender}, Umur: ${user.age}, Berat: ${user.weight}kg, Tinggi: ${user.height}cm."
                    val penyakit = if (user.healthConditions.isNotEmpty()) {
                        "Riwayat penyakit: ${user.healthConditions.joinToString(", ")}."
                    } else {
                        "Kondisi sehat."
                    }

                    val promptSaran = """
                        Bertindaklah sebagai ahli kesehatan dan gizi profesional.
                        Profil pasien: $infoFisik $penyakit
                        
                        Berikan 2 tip harian yang sangat singkat, spesifik, dan memotivasi untuk pasien ini:
                        1. 🍎 Nutrisi: (1 kalimat saran makanan/minuman)
                        2. 🏃‍♂️ Kesehatan: (1 kalimat saran gaya hidup/aktivitas fisik)
                        
                        Dilarang menggunakan format markdown (seperti tanda bintang tebal). Langsung tuliskan teksnya.
                    """.trimIndent()

                    // 3. Tarik Saran dari Gemini
                    tarikSaranGemini(promptSaran)
                } else {
                    binding.tvNamaUser.text = "Pengguna"
                    binding.tvTipTitle.text = "💡 Tip Kesehatan"
                    binding.tvTipContent.text = "Lengkapi biodata profilmu untuk mendapatkan rekomendasi makanan yang dipersonalisasi!"
                }
            }
        }
    }

    private fun tarikSaranGemini(prompt: String) {
        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(prompt))))
        )

        GeminiRetrofitClient.instance
            .generateContent(BuildConfig.GEMINI_API_KEY, request)
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (_binding != null) {
                        val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                        // Ubah judul kartu di sini
                        binding.tvTipTitle.text = "💡 Tip Kesehatan & Nutrisi"
                        binding.tvTipContent.text = result ?: "Perbanyak minum air putih dan luangkan waktu 15 menit untuk jalan kaki hari ini!"
                    }
                }
                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    if (_binding != null) {
                        Log.e("HomeFragment", "Gagal memuat saran Gemini", t)
                        binding.tvTipTitle.text = "💡 Tip Kesehatan"
                        binding.tvTipContent.text = "Tetap jaga kesehatan dan perhatikan komposisi makanan yang kamu konsumsi."
                    }
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}