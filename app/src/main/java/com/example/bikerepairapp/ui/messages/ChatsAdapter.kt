package com.example.bikerepairapp.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R

class ChatsAdapter(
    private val onClick: (ChatUi) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.VH>() {

    private val items = mutableListOf<ChatUi>()

    fun submit(newItems: List<ChatUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.bind(item, onClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvChatTitle)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvChatSubtitle)

        fun bind(item: ChatUi, onClick: (ChatUi) -> Unit) {
            tvTitle.text = "Request: ${item.requestId.take(6)}…"
            tvSubtitle.text = item.lastMessage
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
