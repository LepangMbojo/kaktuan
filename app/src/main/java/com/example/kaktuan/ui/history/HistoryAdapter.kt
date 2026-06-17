package com.example.kaktuan.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kaktuan.R
import com.example.kaktuan.model.ScanHistory
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoryAdapter(
    private val items: List<ScanHistory>,
    private val onClick: (ScanHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNamaProduk: TextView = itemView.findViewById(R.id.tvNamaProduk)
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
        val tvRekomendasiSingkat: TextView = itemView.findViewById(R.id.tvRekomendasiSingkat)
        val tvBadgeStatus: TextView = itemView.findViewById(R.id.tvBadgeStatus)
        val tvHealthScore: TextView = itemView.findViewById(R.id.tvHealthScore)
        val tvScoreIcon: TextView = itemView.findViewById(R.id.tvScoreIcon)
        val cvIconBg: MaterialCardView = itemView.findViewById(R.id.cvIconBg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]

        // Nama produk — fallback jika kosong
        holder.tvNamaProduk.text = item.productName.ifEmpty { "Produk Tanpa Nama" }

        // Rekomendasi singkat
        holder.tvRekomendasiSingkat.text = item.recommendation.ifEmpty { "Tidak ada rekomendasi." }

        // Skor kesehatan
        holder.tvHealthScore.text = "Skor: ${item.healthScore}"

        // Waktu relatif
        holder.tvWaktu.text = getRelativeTime(item.timestamp)

        // Badge + ikon berdasarkan healthScore
        when {
            item.healthScore >= 70 -> {
                holder.tvBadgeStatus.text = "Cocok"
                holder.tvBadgeStatus.setTextColor(Color.parseColor("#245F58"))
                holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_green)
                holder.tvScoreIcon.text = "😊"
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#E8F1F0"))
                holder.tvHealthScore.setTextColor(Color.parseColor("#245F58"))
            }
            item.healthScore >= 40 -> {
                holder.tvBadgeStatus.text = "Perhatikan"
                holder.tvBadgeStatus.setTextColor(Color.parseColor("#C07000"))
                holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_orange)
                holder.tvScoreIcon.text = "⚠️"
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#FFF3E8"))
                holder.tvHealthScore.setTextColor(Color.parseColor("#C07000"))
            }
            else -> {
                holder.tvBadgeStatus.text = "Hindari"
                holder.tvBadgeStatus.setTextColor(Color.parseColor("#C62828"))
                holder.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_red)
                holder.tvScoreIcon.text = "⛔"
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#FDECEA"))
                holder.tvHealthScore.setTextColor(Color.parseColor("#C62828"))
            }
        }

        // Klik item
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    // Mengubah timestamp menjadi teks relatif
    private fun getRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "-"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Baru saja"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} mnt lalu"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} jam lalu"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} hari lalu"
            else -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(timestamp))
        }
    }
}