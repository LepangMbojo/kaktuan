package com.example.kaktuan.ui.history

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kaktuan.R
import com.example.kaktuan.databinding.ItemHistoryBinding
import com.example.kaktuan.model.ScanHistory
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var listHistory: List<ScanHistory>,
    private val onItemClick: (ScanHistory) -> Unit,
    private val onEditClick: (ScanHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(history: ScanHistory) {

            val score = history.healthScore ?: 0

            binding.tvNamaProduk.text = history.productName
            binding.tvHealthScore.text = score.toString()

            if (score < 40) {

                binding.tvBadgeStatus.text = "Bahaya"
                binding.tvBadgeStatus.setTextColor(Color.parseColor("#EF4444"))
                binding.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_red)

                binding.tvScoreIcon.text = "😨"
                binding.tvHealthScore.setTextColor(Color.parseColor("#EF4444"))

            } else {

                binding.tvBadgeStatus.text = "Aman"
                binding.tvBadgeStatus.setTextColor(Color.parseColor("#245F58"))
                binding.tvBadgeStatus.setBackgroundResource(R.drawable.bg_badge_green)

                binding.tvScoreIcon.text = "😊"
                binding.tvHealthScore.setTextColor(Color.parseColor("#38C6A5"))
            }

            val sdf = SimpleDateFormat(
                "dd MMM yyyy, HH:mm",
                Locale.getDefault()
            )

            val date = Date(history.timestamp)

            binding.tvWaktu.text = sdf.format(date)

            binding.root.setOnClickListener {
                onItemClick(history)
            }

            binding.btnEditName.setOnClickListener {
                onEditClick(history)
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