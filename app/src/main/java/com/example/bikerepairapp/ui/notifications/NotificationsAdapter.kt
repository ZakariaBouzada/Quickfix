package com.example.bikerepairapp.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R

data class NotificationUi(
    val id: String,                 // IMPORTANT: unique id e.g. "requestId:accepted"
    val title: String,
    val body: String,
    val timeLabel: String,
    val isUnread: Boolean,
    val sortTimeMillis: Long,
    val requestId: String
)

class NotificationsAdapter(
    private val onItemClick: ((NotificationUi) -> Unit)? = null
) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

    private val items = mutableListOf<NotificationUi>()

    fun submit(newItems: List<NotificationUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): NotificationUi = items[position]

    fun removeAt(position: Int) {
        if (position < 0 || position >= items.size) return
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvNotifTitle)
        val body: TextView = v.findViewById(R.id.tvNotifBody)
        val time: TextView = v.findViewById(R.id.tvNotifTime)
        val unreadDot: View = v.findViewById(R.id.viewUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.body.text = item.body
        holder.time.text = item.timeLabel
        holder.unreadDot.visibility = if (item.isUnread) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size
}
