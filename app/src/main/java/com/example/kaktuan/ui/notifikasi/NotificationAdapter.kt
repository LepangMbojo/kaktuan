package com.example.kaktuan.ui.notifikasi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kaktuan.R
import com.example.kaktuan.model.NotifModel

class NotificationAdapter(
    private val listNotif: List<NotifModel>,
    private val onItemClick: (NotifModel) -> Unit // TAMBAHKAN INI: Detektor klik
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBackground: LinearLayout = view.findViewById(R.id.layoutBackground)
        val tvTitle: TextView = view.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView = view.findViewById(R.id.tvNotifTime)
        val dotUnread: View = view.findViewById(R.id.dotUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = listNotif[position]
        holder.tvTitle.text = notif.title
        holder.tvMessage.text = notif.message
        holder.tvTime.text = notif.time

        if (notif.isRead) {
            holder.dotUnread.visibility = View.INVISIBLE
            holder.layoutBackground.setBackgroundColor(Color.WHITE)
        } else {
            holder.dotUnread.visibility = View.VISIBLE
            holder.layoutBackground.setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        // TAMBAHKAN INI: Daftarkan aksi klik pada seluruh area kotak notifikasi
        holder.itemView.setOnClickListener {
            onItemClick(notif)
        }
    }

    override fun getItemCount(): Int = listNotif.size
}