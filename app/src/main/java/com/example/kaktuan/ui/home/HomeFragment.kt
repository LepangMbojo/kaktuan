package com.example.kaktuan.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kaktuan.BuildConfig
import com.example.kaktuan.R
import com.example.kaktuan.api.Content
import com.example.kaktuan.api.GeminiRequest
import com.example.kaktuan.api.GeminiResponse
import com.example.kaktuan.api.GeminiRetrofitClient
import com.example.kaktuan.api.Part
import com.example.kaktuan.databinding.FragmentHomeBinding
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.supabase.SupabaseDatabaseHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.gotrue.auth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseHelper: SupabaseDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = SupabaseDatabaseHelper()

        setupAnimations()
        setupClickListeners()
        muatDataUser()
    }

    private fun setupAnimations() {
        val viewsToAnimate = listOf(
            binding.cardSearch,
            binding.cardDashboard,
            binding.labelMenu,
            binding.scrollMenu,
            binding.labelTips,
            binding.cardTipsBanner
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(100L * index)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupClickListeners() {
        binding.menuRiwayat.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_history
        }

        binding.menuStatistik.setOnClickListener {
            Toast.makeText(requireContext(), "Fitur Statistik Segera Hadir!", Toast.LENGTH_SHORT).show()
        }

        binding.menuRekomendasi.setOnClickListener {
            if (binding.rvSaranAI.visibility == View.VISIBLE) {
                binding.rvSaranAI.visibility = View.GONE
            } else {
                binding.rvSaranAI.visibility = View.VISIBLE
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, binding.rvSaranAI.bottom)
                }
            }
        }

        binding.filterHariIni.setOnClickListener { ubahFilterAktif(binding.filterHariIni) }
        binding.filterMinggu.setOnClickListener { ubahFilterAktif(binding.filterMinggu) }
        binding.filterSemua.setOnClickListener { ubahFilterAktif(binding.filterSemua) }
    }

    private fun ubahFilterAktif(viewAktif: TextView) {
        val daftarFilter = listOf(binding.filterHariIni, binding.filterMinggu, binding.filterSemua)

        daftarFilter.forEach { tv ->
            if (tv == viewAktif) {
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#38C6A5"))
                tv.setTextColor(Color.WHITE)
            } else {
                tv.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#334155"))
                tv.setTextColor(Color.parseColor("#94A3B8"))
            }
        }
    }

    private fun muatDataUser() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return

        muatStatistikKeamanan(uid)

        databaseHelper.getUserProfile(uid) { user ->
            if (_binding == null || user == null) return@getUserProfile

            val namaPanggilan = user.name.split(" ").firstOrNull() ?: user.name
            binding.tvNamaUser.text = namaPanggilan

            var fotoUrl = user.profilePictureUrl

            if (fotoUrl.isNullOrEmpty() || fotoUrl == "null") {
                val metadata = session.user?.userMetadata
                fotoUrl = metadata?.get("avatar_url")?.toString()?.replace("\"", "")
                    ?: metadata?.get("picture")?.toString()?.replace("\"", "")
            }

            // BLOK YANG SUDAH BERSIH
            if (!fotoUrl.isNullOrEmpty() && fotoUrl != "null") {
                binding.ivProfileHome.imageTintList = null

                Glide.with(this@HomeFragment) // Menggunakan referensi fragment yang eksplisit
                    .load(fotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.ivProfileHome)
            } else {
                binding.ivProfileHome.setImageResource(R.drawable.user)
            }

            val infoFisik = "Gender: ${user.gender}, Umur: ${user.age}, Berat: ${user.weight}kg, Tinggi: ${user.height}cm."
            val penyakit = if (user.healthConditions.isNotEmpty()) {
                "Riwayat penyakit: ${user.healthConditions.joinToString(", ")}."
            } else {
                "Kondisi sehat."
            }
            val promptSaran = """
                Bertindaklah sebagai ahli kesehatan dan gizi profesional.
                Profil pasien: $infoFisik $penyakit
                Berikan 1 tip harian saja yang sangat singkat (maksimal 2 kalimat), spesifik, dan memotivasi untuk pasien ini terkait makanan atau aktivitas fisik. Dilarang menggunakan format markdown.
            """.trimIndent()

            tarikSaranGemini(promptSaran)
        }
    }

    private fun muatStatistikKeamanan(uid: String) {
        databaseHelper.getScanHistory(uid) { listHistory ->
            if (_binding == null) return@getScanHistory

            if (listHistory != null && listHistory.isNotEmpty()) {
                val totalScan = listHistory.size
                var countAman = 0

                for (history in listHistory) {
                    if ((history.healthScore ?: 0) >= 70) {
                        countAman++
                    }
                }

                updateDiagramLingkaran(countAman, totalScan)
            } else {
                updateDiagramLingkaran(0, 0)
            }
        }
    }

    private fun updateDiagramLingkaran(countAman: Int, totalScan: Int) {
        if (totalScan > 0) {
            val healthScore = ((countAman.toDouble() / totalScan) * 100).toInt()

            binding.progressKeamanan.setProgressCompat(healthScore, true)
            binding.tvPersentase.text = "$healthScore%"

            when {
                healthScore >= 70 -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#38C6A5"))
                healthScore >= 40 -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#F39C12"))
                else -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#E74C3C"))
            }
        } else {
            binding.progressKeamanan.progress = 0
            binding.tvPersentase.text = "0%"
            binding.progressKeamanan.setIndicatorColor(Color.parseColor("#334155"))
        }
    }

    private fun tarikSaranGemini(prompt: String) {
        val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))

        GeminiRetrofitClient.instance.generateContent(BuildConfig.GEMINI_API_KEY, request)
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (_binding != null) {
                        val result = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: "Perbanyak minum air putih dan luangkan waktu 15 menit untuk jalan kaki hari ini!"
                        binding.rvSaranAI.adapter = AiAdapter(result)
                    }
                }
                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    if (_binding != null) {
                        Log.e("HomeFragment", "Gagal memuat saran Gemini", t)
                        binding.rvSaranAI.adapter = AiAdapter("Koneksi internet lambat. Tetap perhatikan komposisi makananmu ya!")
                    }
                }
            })
    }

    inner class AiAdapter(private val tip: String) : RecyclerView.Adapter<AiAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvContent: TextView = view.findViewById(R.id.tvAiContent)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saran_ai, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvContent.text = tip
        }
        override fun getItemCount() = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}