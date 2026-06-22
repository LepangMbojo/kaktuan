package com.example.kaktuan.ui.home

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseHelper: SupabaseDatabaseHelper

    // Wadah penyimpan riwayat agar tidak perlu bolak-balik download dari internet saat filter diklik
    private var dataRiwayatGlobal: JSONArray = JSONArray()

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
            binding.cardBarChart,
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
        // Menyambungkan tombol filter dengan logika Pie Chart
        binding.filterHariIni.setOnClickListener {
            ubahFilterAktif(binding.filterHariIni)
            terapkanFilterPieChart("Hari Ini")
        }
        binding.filterMinggu.setOnClickListener {
            ubahFilterAktif(binding.filterMinggu)
            terapkanFilterPieChart("Minggu")
        }
        binding.filterSemua.setOnClickListener {
            ubahFilterAktif(binding.filterSemua)
            terapkanFilterPieChart("Semua")
        }
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

        // Panggil fungsi penarik riwayat tunggal
        muatDataRiwayatDashboard(uid)

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

            if (!fotoUrl.isNullOrEmpty() && fotoUrl != "null") {
                binding.ivProfileHome.imageTintList = null
                Glide.with(this@HomeFragment)
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

    // Fungsi tunggal untuk mengambil riwayat 1 kali saja
    private fun muatDataRiwayatDashboard(uid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // PERBAIKAN DI SINI: Gunakan .data untuk mengambil JSON string
                val response = SupabaseClient.client.postgrest["history"]
                    .select { filter { eq("user_id", uid) } }
                    .data

                // Jika data kosong, Supabase biasanya mengembalikan "[]"
                if (response.isNotBlank()) {
                    dataRiwayatGlobal = JSONArray(response)
                }

                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        gambarBarChartNative() // Gambar BarChart Mingguan
                        terapkanFilterPieChart("Semua") // Tampilkan PieChart awal
                        ubahFilterAktif(binding.filterSemua) // Set tombol awal
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Gagal muat data riwayat: ${e.message}")
            }
        }
    }
    // Logika perhitungan skor berdasarkan filter tombol
    private fun terapkanFilterPieChart(tipeFilter: String) {
        var totalSkor = 0
        var jumlahScan = 0
        val waktuSekarang = System.currentTimeMillis()

        for (i in 0 until dataRiwayatGlobal.length()) {
            val item = dataRiwayatGlobal.getJSONObject(i)
            val score = item.optInt("health_score", 0)
            val timestamp = item.optLong("timestamp", 0L)

            if (timestamp > 0) {
                var masukKriteria = false

                when (tipeFilter) {
                    "Hari Ini" -> {
                        if (DateUtils.isToday(timestamp)) masukKriteria = true
                    }
                    "Minggu" -> {
                        // Cek apakah data berada dalam 7 hari ke belakang
                        if (waktuSekarang - timestamp <= 7L * 24 * 60 * 60 * 1000) masukKriteria = true
                    }
                    "Semua" -> {
                        masukKriteria = true
                    }
                }

                if (masukKriteria) {
                    totalSkor += score
                    jumlahScan++
                }
            }
        }

        val rataRataSkor = if (jumlahScan > 0) totalSkor / jumlahScan else 0
        updateDiagramLingkaran(rataRataSkor)
    }

    private fun updateDiagramLingkaran(rataRataSkor: Int) {
        if (_binding == null) return

        binding.progressKeamanan.setProgressCompat(rataRataSkor, true)
        binding.tvPersentase.text = "$rataRataSkor%"

        when {
            rataRataSkor >= 70 -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#38C6A5"))
            rataRataSkor >= 40 -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#F39C12"))
            rataRataSkor > 0 -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#E74C3C"))
            else -> binding.progressKeamanan.setIndicatorColor(Color.parseColor("#334155")) // Warna abu-abu jika 0
        }
    }

    private fun gambarBarChartNative() {
        if (_binding == null) return

        val skorTotal = FloatArray(7) { 0f }
        val jumlahScan = IntArray(7) { 0 }
        val cal = Calendar.getInstance()

        // Bar Chart selalu menampilkan data 1 minggu terakhir, independen dari filter PieChart
        for (i in 0 until dataRiwayatGlobal.length()) {
            val item = dataRiwayatGlobal.getJSONObject(i)
            val time = item.optLong("timestamp", 0L)
            val score = item.optInt("health_score", 0)

            if (time > 0) {
                cal.timeInMillis = time
                var hariIndex = cal.get(Calendar.DAY_OF_WEEK) - 2
                if (hariIndex < 0) hariIndex = 6

                skorTotal[hariIndex] += score.toFloat()
                jumlahScan[hariIndex]++
            }
        }

        val rataRataSkor = IntArray(7)
        for (i in 0..6) {
            rataRataSkor[i] = if (jumlahScan[i] > 0) (skorTotal[i] / jumlahScan[i]).toInt() else 0
        }

        val daftarBalok = listOf(
            binding.barSenin, binding.barSelasa, binding.barRabu,
            binding.barKamis, binding.barJumat, binding.barSabtu, binding.barMinggu
        )
        val daftarTeks = listOf(
            binding.tvSkorSenin, binding.tvSkorSelasa, binding.tvSkorRabu,
            binding.tvSkorKamis, binding.tvSkorJumat, binding.tvSkorSabtu, binding.tvSkorMinggu
        )

        val tinggiMaxPx = (140 * resources.displayMetrics.density).toInt()

        for (i in 0..6) {
            val skor = rataRataSkor[i]
            daftarTeks[i].text = skor.toString()

            if (skor == 0) continue

            if (skor < 50) {
                daftarBalok[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E74C3C"))
                daftarTeks[i].setTextColor(Color.parseColor("#E74C3C"))
            } else {
                daftarBalok[i].backgroundTintList = ColorStateList.valueOf(Color.parseColor("#38C6A5"))
                daftarTeks[i].setTextColor(Color.parseColor("#94A3B8"))
            }

            val tinggiTujuanPx = (skor / 100f * tinggiMaxPx).toInt()

            ValueAnimator.ofInt(0, tinggiTujuanPx).apply {
                duration = 1000L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    val param = daftarBalok[i].layoutParams
                    param.height = animator.animatedValue as Int
                    daftarBalok[i].layoutParams = param
                }
                start()
            }
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

                        binding.rvSaranAI.visibility = View.VISIBLE
                        binding.rvSaranAI.adapter = AiAdapter(result)
                    }
                }
                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    if (_binding != null) {
                        Log.e("HomeFragment", "Gagal memuat saran Gemini", t)
                        binding.rvSaranAI.visibility = View.VISIBLE
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