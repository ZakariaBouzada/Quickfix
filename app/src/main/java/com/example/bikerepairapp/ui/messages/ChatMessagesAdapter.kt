package com.example.bikerepairapp.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R

class ChatMessagesAdapter : RecyclerView.Adapter<ChatMessagesAdapter.VH>() {

    private val items = mutableListOf<ChatMessageUi>()

    fun submit(newItems: List<ChatMessageUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: TextView = itemView.findViewById(R.id.tvChatMessage)

        fun bind(m: ChatMessageUi) {
            tv.text = m.text
            // Simple left/right vibe by padding + alignment style in XML (kept minimal here)
            itemView.alpha = if (m.isMine) 1.0f else 0.92f
        }
    }
}
