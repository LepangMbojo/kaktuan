package com.example.kaktuan.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kaktuan.databinding.ItemHistoryBinding
import com.example.kaktuan.model.ScanHistory
import java.text.SimpleDateFormat
import java.util.*

// CATATAN PENTING: onEditClick telah dihapus dari parameter karena tombol pensil sudah tidak ada
class HistoryAdapter(
    private var listHistory: List<ScanHistory>,
    private val onItemClick: (ScanHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(history: ScanHistory) {

            val score = history.healthScore ?: 0

            binding.tvNamaProduk.text = history.productName
            binding.tvHealthScore.text = score.toString()

            // 1. Menampilkan Foto menggunakan Glide
            if (!history.photoUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(history.photoUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(binding.ivHistoryPhoto)
            } else {
                binding.ivHistoryPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // 2. Logika Lencana Status (Aman/Bahaya)
            if (score < 40) {
                // Tampilan Bahaya (Merah)
                binding.tvBadgeStatus.text = "Bahaya"
                binding.tvBadgeStatus.setTextColor(Color.parseColor("#EF4444"))
                binding.cvBadgeStatus.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                binding.tvHealthScore.setTextColor(Color.parseColor("#EF4444"))
            } else {
                // Tampilan Aman (Hijau)
                binding.tvBadgeStatus.text = "Aman"
                binding.tvBadgeStatus.setTextColor(Color.parseColor("#245F58"))
                binding.cvBadgeStatus.setCardBackgroundColor(Color.parseColor("#E6F4F1"))
                binding.tvHealthScore.setTextColor(Color.parseColor("#38C6A5"))
            }

            // 3. Format Waktu Scan
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val date = Date(history.timestamp)
            binding.tvWaktu.text = sdf.format(date)

            // 4. Klik Item untuk melihat detail
            binding.root.setOnClickListener {
                onItemClick(history)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(listHistory[position])
    }

    override fun getItemCount(): Int = listHistory.size

    fun updateData(newList: List<ScanHistory>) {
        listHistory = newList
        notifyDataSetChanged()
    }
}