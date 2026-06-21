package com.example.kaktuan.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.kaktuan.R
import com.example.kaktuan.databinding.FragmentProfileBinding
import com.example.kaktuan.supabase.SupabaseClient
import com.example.kaktuan.supabase.SupabaseDatabaseHelper
import com.example.kaktuan.ui.auth.LoginActivity
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseHelper: SupabaseDatabaseHelper

    // Fitur untuk membuka Galeri dan memilih gambar
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            uploadFotoProfil(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = SupabaseDatabaseHelper()

        setupAnimations()
        setupClickListeners()
        muatDataProfil()
    }

    private fun setupAnimations() {
        val viewsToAnimate = listOf(
            binding.cardAvatar,
            binding.tvNamaProfil,
            binding.tvEmailProfil,
            binding.cardFisik,
            binding.cardKesehatan,
            binding.btnEditProfil,
            binding.btnLogout
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(100L * index)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupClickListeners() {
        // Klik foto profil untuk mengganti foto
        binding.cardAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*") // Hanya tampilkan file gambar
        }

        binding.btnEditProfil.setOnClickListener {
            val intent = Intent(requireActivity(), BiodataActivity::class.java)
            intent.putExtra("IS_EDIT", true)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    SupabaseClient.client.auth.signOut()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireActivity(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.btnLogout.isEnabled = true
                        Toast.makeText(requireContext(), "Gagal keluar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun muatDataProfil() {
        val session = SupabaseClient.client.auth.currentSessionOrNull()
        val uid = session?.user?.id ?: return

        binding.tvNamaProfil.text = "Memuat data..."

        databaseHelper.getUserProfile(uid) { user ->
            if (_binding == null) return@getUserProfile

            if (user != null) {
                binding.tvNamaProfil.text = user.name
                binding.tvEmailProfil.text = session.user?.email ?: user.email
                binding.tvUmurProfil.text = "${user.age} Tahun"
                binding.tvGenderProfil.text = user.gender
                binding.tvTinggiProfil.text = "${user.height} cm"
                binding.tvBeratProfil.text = "${user.weight} kg"

                if (user.healthConditions.isNotEmpty()) {
                    binding.tvPenyakitProfil.text = user.healthConditions.joinToString(", ")
                    binding.tvPenyakitProfil.setTextColor(resources.getColor(R.color.black, null))
                } else {
                    binding.tvPenyakitProfil.text = "Tidak ada riwayat penyakit mendasar."
                }

                var finalPhotoUrl = user.profilePictureUrl

                if (finalPhotoUrl.isNullOrEmpty() || finalPhotoUrl == "null") {
                    val metadata = session.user?.userMetadata
                    finalPhotoUrl = metadata?.get("avatar_url")?.toString()?.replace("\"", "")
                        ?: metadata?.get("picture")?.toString()?.replace("\"", "")
                }

                tampilkanFotoKeUI(finalPhotoUrl, user.name)
            } else {
                binding.tvNamaProfil.text = "Profil Tidak Ditemukan"
                Toast.makeText(requireContext(), "Gagal menarik data dari server", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi terpisah untuk mengunggah foto ke Supabase Storage
    private fun uploadFotoProfil(uri: Uri) {
        val uid = SupabaseClient.client.auth.currentSessionOrNull()?.user?.id ?: return

        Toast.makeText(requireContext(), "Mengunggah foto...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Ubah file gambar dari Galeri menjadi Byte Array
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: return@launch
                inputStream.close()

                // 2. Upload ke Supabase Storage (Tambahkan timestamp agar URL unik & Glide otomatis refresh)
                val fileName = "${uid}_${System.currentTimeMillis()}.jpg"
                SupabaseClient.client.storage.from("avatars").upload(fileName, bytes)

                // 3. Dapatkan URL Publik
                val publicUrl = SupabaseClient.client.storage.from("avatars").publicUrl(fileName)

                // 4. Update data URL tersebut ke tabel 'users'
                val updateData = buildJsonObject {
                    // Pastikan "profile_picture_url" sesuai dengan nama kolom di DB Anda
                    put("profile_picture_url", publicUrl)
                }
                SupabaseClient.client.postgrest["users"].update(updateData) {
                    filter { eq("id", uid) }
                }

                // 5. Update UI di layar
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Foto profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    tampilkanFotoKeUI(publicUrl, binding.tvNamaProfil.text.toString())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal mengunggah foto: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Fungsi untuk me-render gambar menggunakan Glide (bisa dipanggil berkali-kali)
    private fun tampilkanFotoKeUI(photoUrl: String?, userName: String) {
        if (_binding == null) return

        if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
            binding.ivProfilePicture.visibility = View.VISIBLE
            binding.tvInisialProfil.visibility = View.GONE
            binding.ivProfilePicture.imageTintList = null
            Glide.with(this@ProfileFragment)
                .load(photoUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Matikan cache disk sementara
                .skipMemoryCache(true) // Matikan cache memori sementara
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .into(binding.ivProfilePicture)
        } else {
            binding.ivProfilePicture.visibility = View.GONE
            binding.tvInisialProfil.visibility = View.VISIBLE

            if (userName.isNotEmpty()) {
                binding.tvInisialProfil.text = userName.take(1).uppercase()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}