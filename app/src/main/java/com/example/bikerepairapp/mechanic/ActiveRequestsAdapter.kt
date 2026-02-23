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




class ActiveRequestsAdapter(
    private val onComplete: (RepairRequest) -> Unit,
    private val onRelease: (RepairRequest) -> Unit
) : RecyclerView.Adapter<ActiveRequestsAdapter.ViewHolder>() {

    private val items = mutableListOf<RepairRequest>()
    private var highlightedId: String? = null

    fun submitList(newItems: List<RepairRequest>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_request_active, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, onComplete, onRelease)

        if (item.id == highlightedId) {
            holder.itemView.setBackgroundColor(Color.parseColor("#333300"))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun highlightItem(id: String) {
        highlightedId = id
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)

        private val btnComplete: MaterialButton = itemView.findViewById(R.id.btnComplete)
        private val btnRelease: MaterialButton = itemView.findViewById(R.id.btnRelease)
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivRequestPhoto)

        fun bind(
            req: RepairRequest,
            onComplete: (RepairRequest) -> Unit,
            onRelease: (RepairRequest) -> Unit
        ) {
            tvIssue.text = req.issue

            // QuickFix colors
            val yellow = Color.parseColor("#FFFF00")
            val dark = Color.parseColor("#202020")
            val white = Color.WHITE
            val black = Color.BLACK

            // ---- Base meta line ----
            val date = req.date.ifBlank { "No time" }
            val loc = req.location.ifBlank { "No location" }


            // If mechanic already completed -> waiting for customer confirmation
            val isWaitingCustomer = req.status == "completed_pending"
            if (isWaitingCustomer) {
                val who = req.customerName
                    .ifBlank { req.customerEmail.ifBlank { "customer" } }
                tvMeta.text = "Waiting for confirmation from $who"
            } else {
                tvMeta.text = "$date • $loc"
            }

            if (!req.imageUri.isNullOrEmpty()) {
                ivPhoto.visibility = View.VISIBLE
                ivPhoto.load(req.imageUri) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_bike) // Make sure this exists or remove line
                }
            } else {
                ivPhoto.visibility = View.GONE
            }

            // ---- Complete (primary) ----
            btnComplete.text = if (isWaitingCustomer) "Waiting…" else "Repair completed"
            btnComplete.isAllCaps = false
            btnComplete.cornerRadius = dp(12)
            btnComplete.backgroundTintList = ColorStateList.valueOf(yellow)
            btnComplete.setTextColor(black)
            btnComplete.strokeWidth = 0

            // Disable (and visually soften) when waiting
            btnComplete.isEnabled = !isWaitingCustomer
            btnComplete.alpha = if (isWaitingCustomer) 0.55f else 1.0f

            // Optional: if you prefer, hide instead of disable:
            // btnComplete.visibility = if (isWaitingCustomer) View.GONE else View.VISIBLE

            // ---- Release (secondary) ----
            btnRelease.text = "Release"
            btnRelease.isAllCaps = false
            btnRelease.cornerRadius = dp(12)
            btnRelease.backgroundTintList = ColorStateList.valueOf(dark)
            btnRelease.setTextColor(white)
            btnRelease.strokeColor = ColorStateList.valueOf(yellow)
            btnRelease.strokeWidth = dp(1)
            btnRelease.alpha = 1.0f
            btnRelease.isEnabled = true

            // Clicks
            btnComplete.setOnClickListener { onComplete(req) }
            btnRelease.setOnClickListener { onRelease(req) }
        }

        private fun dp(value: Int): Int {
            val density = itemView.resources.displayMetrics.density
            return (value * density).toInt()
        }
    }
}
