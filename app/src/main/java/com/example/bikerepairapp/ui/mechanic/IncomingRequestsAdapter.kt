package com.example.bikerepairapp.ui.mechanic

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class IncomingRequestsAdapter(
    private val onAccept: (RepairRequest) -> Unit,
    private val onReject: (RepairRequest) -> Unit
) : RecyclerView.Adapter<IncomingRequestsAdapter.ViewHolder>() {

    private val items = mutableListOf<RepairRequest>()

    fun submitList(newItems: List<RepairRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_incoming, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onAccept, onReject)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        private val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)

        fun bind(
            req: RepairRequest,
            onAccept: (RepairRequest) -> Unit,
            onReject: (RepairRequest) -> Unit
        ) {
            tvIssue.text = req.issue

            val date = req.date.ifBlank { "No time" }
            val loc = req.location.ifBlank { "No location" }
            tvMeta.text = "$date • $loc"

            val yellow = Color.parseColor("#FFFF00")
            val dark = Color.parseColor("#202020")

            btnAccept.text = "Accept"
            btnAccept.isAllCaps = false
            btnAccept.cornerRadius = dp(12)
            btnAccept.backgroundTintList = ColorStateList.valueOf(yellow)
            btnAccept.setTextColor(Color.BLACK)
            btnAccept.strokeWidth = 0

            btnReject.text = "Reject"
            btnReject.isAllCaps = false
            btnReject.cornerRadius = dp(12)
            btnReject.backgroundTintList = ColorStateList.valueOf(dark)
            btnReject.setTextColor(Color.WHITE)
            btnReject.strokeColor = ColorStateList.valueOf(yellow)
            btnReject.strokeWidth = dp(1)

            btnAccept.setOnClickListener { onAccept(req) }
            btnReject.setOnClickListener { onReject(req) }
        }

        private fun dp(value: Int): Int {
            val density = itemView.resources.displayMetrics.density
            return (value * density).toInt()
        }
    }
}
