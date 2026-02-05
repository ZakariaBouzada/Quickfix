package com.example.bikerepairapp.ui.mechanic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R

class CompletedRequestsAdapter : RecyclerView.Adapter<CompletedRequestsAdapter.VH>() {

    private val items = mutableListOf<RepairRequest>()

    fun submitList(newItems: List<RepairRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_completed, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(req: RepairRequest) {
            tvIssue.text = req.issue
            val date = req.date.ifBlank { "No time" }
            val loc = req.location.ifBlank { "No location" }
            tvMeta.text = "$date • $loc"
            tvStatus.text = "Completed"
        }
    }
}
