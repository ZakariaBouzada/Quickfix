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

class ActiveRequestsAdapter(
    private val onComplete: (RepairRequest) -> Unit,
    private val onRelease: (RepairRequest) -> Unit,
    private val onShareLocation: (RepairRequest) -> Unit,
    private val onStopSharing: (RepairRequest) -> Unit
) : RecyclerView.Adapter<ActiveRequestsAdapter.ViewHolder>() {

    private val items = mutableListOf<RepairRequest>()

    /** ID of the request currently sharing location (set from fragment) */
    var sharingRequestId: String? = null

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
        val isSharing = item.locationSharingActive || item.id == sharingRequestId
        holder.bind(item, onComplete, onRelease, onShareLocation, onStopSharing, isSharing)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)

        private val btnComplete: MaterialButton = itemView.findViewById(R.id.btnComplete)
        private val btnRelease: MaterialButton = itemView.findViewById(R.id.btnRelease)
        private val btnShareLocation: MaterialButton = itemView.findViewById(R.id.btnShareLocation)

        fun bind(
            req: RepairRequest,
            onComplete: (RepairRequest) -> Unit,
            onRelease: (RepairRequest) -> Unit,
            onShareLocation: (RepairRequest) -> Unit,
            onStopSharing: (RepairRequest) -> Unit,
            isSharing: Boolean
        ) {
            tvIssue.text = req.issue

            // QuickFix colors
            val blue = Color.parseColor("#4472C4")
            val red = Color.parseColor("#FF4444")
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
                tvMeta.text = "$date \u2022 $loc"
            }

            // ---- Share Location button (only for accepted, not completed_pending) ----
            if (req.status == "accepted") {
                btnShareLocation.visibility = View.VISIBLE

                if (isSharing) {
                    btnShareLocation.text = "Sharing location\u2026 Stop"
                    btnShareLocation.setTextColor(red)
                    btnShareLocation.strokeColor = ColorStateList.valueOf(red)
                    btnShareLocation.backgroundTintList = ColorStateList.valueOf(dark)
                    btnShareLocation.strokeWidth = dp(1)
                    btnShareLocation.setOnClickListener { onStopSharing(req) }
                } else {
                    btnShareLocation.text = "Share my location"
                    btnShareLocation.setTextColor(blue)
                    btnShareLocation.strokeColor = ColorStateList.valueOf(blue)
                    btnShareLocation.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#101010"))
                    btnShareLocation.strokeWidth = dp(1)
                    btnShareLocation.setOnClickListener { onShareLocation(req) }
                }
            } else {
                btnShareLocation.visibility = View.GONE
            }

            // ---- Complete (primary) ----
            btnComplete.text = if (isWaitingCustomer) "Waiting\u2026" else "Repair completed"
            btnComplete.isAllCaps = false
            btnComplete.cornerRadius = dp(12)
            btnComplete.backgroundTintList = ColorStateList.valueOf(blue)
            btnComplete.setTextColor(white)
            btnComplete.strokeWidth = 0

            // Disable (and visually soften) when waiting
            btnComplete.isEnabled = !isWaitingCustomer
            btnComplete.alpha = if (isWaitingCustomer) 0.55f else 1.0f

            // ---- Release (secondary) ----
            btnRelease.text = "Release"
            btnRelease.isAllCaps = false
            btnRelease.cornerRadius = dp(12)
            btnRelease.backgroundTintList = ColorStateList.valueOf(dark)
            btnRelease.setTextColor(white)
            btnRelease.strokeColor = ColorStateList.valueOf(blue)
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
