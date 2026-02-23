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
import android.widget.ImageView
import coil.load




class IncomingRequestsAdapter(
    private val onAccept: (RepairRequest) -> Unit,
    private val onReject: (RepairRequest) -> Unit
) : RecyclerView.Adapter<IncomingRequestsAdapter.ViewHolder>() {

    private val items = mutableListOf<RepairRequest>()
    private var highlightedId: String? = null

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
        val item = items[position]
        holder.bind(item, onAccept, onReject)

        // Highlight logic
        holder.itemView.setBackgroundColor(
            if (item.id == highlightedId) Color.parseColor("#333300")
            else Color.TRANSPARENT
        )
    }
    fun highlightItem(id: String) {
        highlightedId = id
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        private val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivRequestPhoto)

        fun bind(
            req: RepairRequest,
            onAccept: (RepairRequest) -> Unit,
            onReject: (RepairRequest) -> Unit
        ) {
            tvIssue.text = req.issue

            val date = req.date.ifBlank { "No time" }
            val loc = req.location.ifBlank { "No location" }
            tvMeta.text = "$date • $loc"

            if (!req.imageUri.isNullOrEmpty()) {
                ivPhoto.visibility = View.VISIBLE
                ivPhoto.load(req.imageUri) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_bike) // Make sure this exists or remove line
                }
            } else {
                ivPhoto.visibility = View.GONE
            }

            // ---- UI-only styling ----
            val yellow = Color.parseColor("#FFFF00")
            val dark = Color.parseColor("#202020")
            val white = Color.WHITE
            val black = Color.BLACK

            // Accept = primary (yellow filled)
            btnAccept.text = "Accept"
            btnAccept.isAllCaps = false
            btnAccept.cornerRadius = dp(12)
            btnAccept.setPadding(dp(16), dp(10), dp(16), dp(10))
            btnAccept.backgroundTintList = ColorStateList.valueOf(yellow)
            btnAccept.setTextColor(black)
            btnAccept.strokeWidth = 0

            // Reject = secondary (dark with yellow stroke)
            btnReject.text = "Reject"
            btnReject.isAllCaps = false
            btnReject.cornerRadius = dp(12)
            btnReject.setPadding(dp(16), dp(10), dp(16), dp(10))
            btnReject.backgroundTintList = ColorStateList.valueOf(dark)
            btnReject.setTextColor(white)
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
