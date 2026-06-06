package com.example.kaktuan.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kaktuan.databinding.ActivityProfileBinding // Pastikan nama import ini sesuai
import com.example.kaktuan.firebase.firestore.FirestoreHelper
import com.example.kaktuan.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : Fragment() {

    // Setup ViewBinding untuk Fragment
    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestoreHelper: FirestoreHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestoreHelper = FirestoreHelper()

        // 1. Panggil fungsi untuk memuat data saat fragment dibuka
        muatDataProfil()

        // 2. Logika untuk Tombol Logout
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()

            // Pindah ke halaman Login dan hapus sisa halaman sebelumnya
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun muatDataProfil() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Tampilkan tulisan loading sementara
            binding.tvNamaProfil.text = "Memuat data..."

            firestoreHelper.getUserProfile(uid) { user ->
                if (user != null) {
                    // Ganti placeholder dengan data asli dari database
                    binding.tvNamaProfil.text = user.name
                    binding.tvEmailProfil.text = user.email
                    binding.tvUmurProfil.text = "${user.age} Tahun"
                    binding.tvGenderProfil.text = user.gender
                    binding.tvTinggiProfil.text = "${user.height} cm"
                    binding.tvBeratProfil.text = "${user.weight} kg"

                    // Inisial foto profil dinamis (mengambil huruf pertama nama)
                    if (user.name.isNotEmpty()) {
                        binding.tvInisialProfil.text = user.name.take(1).uppercase()
                    }

                    // Menampilkan kondisi kesehatan
                    if (user.healthConditions.isNotEmpty()) {
                        binding.tvPenyakitProfil.text = user.healthConditions.joinToString(", ")
                    } else {
                        binding.tvPenyakitProfil.text = "Tidak ada riwayat penyakit mendasar."
                    }
                } else {
                    binding.tvNamaProfil.text = "Gagal memuat"
                    Toast.makeText(requireContext(), "Gagal menarik data dari server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Wajib ada di Fragment untuk menghindari kebocoran memori (Memory Leak)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}