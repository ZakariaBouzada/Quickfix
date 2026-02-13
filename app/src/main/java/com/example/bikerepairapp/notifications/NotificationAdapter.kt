package com.example.bikerepairapp.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R

data class NotificationItem(val id: String, val message: String, val time: String)

class NotificationAdapter(private val onClick: (String) -> Unit) : RecyclerView.Adapter<NotificationAdapter.VH>() {
    private val items = mutableListOf<NotificationItem>()

    fun submitList(newList: List<NotificationItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // Use your custom item layout here!
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.t1.text = item.message
        holder.t2.text = item.time
        holder.itemView.setOnClickListener { onClick(item.id) }
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        // Updated to match your custom item_notification.xml IDs
        val t1: TextView = v.findViewById(R.id.tvMessage)
        val t2: TextView = v.findViewById(R.id.tvTime)
    }
}