package com.example.kaktuan.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

        binding.cardScanShortcut.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_scan
        }

        binding.tvLihatSemua.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_history
        }

        muatDataUser()
    }

    private fun muatDataUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            muatStatistikRiwayat(uid)

            firestoreHelper.getUserProfile(uid) { user ->
                if (_binding == null) return@getUserProfile
                if (user != null) {
                    val namaPanggilan = user.name.split(" ").firstOrNull() ?: user.name
                    binding.tvNamaUser.text = namaPanggilan

                    val fotoUrl = user.profilePictureUrl
                    if (!fotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
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

    private fun muatStatistikRiwayat(uid: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).collection("history")
            .get()
            .addOnSuccessListener { documents ->
                if (_binding == null) return@addOnSuccessListener

                val totalScan = documents.size()
                var countAman = 0
                val listRiwayat = mutableListOf<RiwayatItem>()

                // Algoritma untuk mencari makanan paling sering dikonsumsi
                val frequencyMap = HashMap<String, Int>()

                for (document in documents) {
                    val analisisKesehatan = document.get("analisis_kesehatan") as? Map<*, *>
                    val analysis = analisisKesehatan?.get("analysis") as? Map<*, *>
                    val isSafe = analysis?.get("is_safe") as? Boolean ?: true

                    val productName = document.getString("product_name") ?: "Produk Ter-scan"

                    // Hitung kemunculan setiap makanan
                    frequencyMap[productName] = frequencyMap.getOrDefault(productName, 0) + 1

                    if (isSafe) countAman++

                    if (listRiwayat.size < 5) {
                        listRiwayat.add(RiwayatItem(productName, isSafe))
                    }
                }

                // Cari makanan dengan nilai frekuensi tertinggi
                val mostConsumed = if (frequencyMap.isNotEmpty()) {
                    frequencyMap.maxByOrNull { it.value }?.key
                } else {
                    "Belum ada data"
                }

                // Tampilkan makanan paling sering dikonsumsi ke UI
                binding.tvMostConsumed.text = mostConsumed

                updateIndikatorGizi(countAman, totalScan)
                binding.rvTerakhirDipindai.adapter = RiwayatAdapter(listRiwayat)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Gagal memuat statistik database", e)
            }
    }

    private fun updateIndikatorGizi(countAman: Int, totalScan: Int) {
        val cardIndikator = binding.layoutIndikator.getChildAt(0) ?: return
        val progressRing = cardIndikator.findViewById<CircularProgressIndicator>(R.id.progressRing)
        val tvRingLabel = cardIndikator.findViewById<TextView>(R.id.tvRingLabel)
        val tvRingTitle = cardIndikator.findViewById<TextView>(R.id.tvRingTitle)

        tvRingTitle.text = "Skor Aman"

        if (totalScan > 0) {
            val healthScore = ((countAman.toDouble() / totalScan) * 100).toInt()
            progressRing.progress = healthScore
            tvRingLabel.text = "$healthScore%"

            when {
                healthScore >= 70 -> progressRing.setIndicatorColor(Color.parseColor("#38C6A5"))
                healthScore >= 40 -> progressRing.setIndicatorColor(Color.parseColor("#F39C12"))
                else -> progressRing.setIndicatorColor(Color.parseColor("#E74C3C"))
            }
        } else {
            progressRing.progress = 0
            tvRingLabel.text = "0%"
            progressRing.setIndicatorColor(Color.parseColor("#BDC3C7"))
        }

        binding.layoutIndikator.getChildAt(1)?.visibility = View.INVISIBLE
        binding.layoutIndikator.getChildAt(2)?.visibility = View.INVISIBLE
    }

    data class RiwayatItem(val nama: String, val isSafe: Boolean)

    inner class RiwayatAdapter(private val list: List<RiwayatItem>) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvProductName)
            val tvStatus: TextView = view.findViewById(R.id.tvProductStatus)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_riwayat_scan, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.nama
            if (item.isSafe) {
                holder.tvStatus.text = "Aman"
                holder.tvStatus.setTextColor(Color.parseColor("#245F58"))
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D1E8E5"))
            } else {
                holder.tvStatus.text = "Bahaya"
                holder.tvStatus.setTextColor(Color.parseColor("#E74C3C"))
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FADBD8"))
            }
        }
        override fun getItemCount() = list.size
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