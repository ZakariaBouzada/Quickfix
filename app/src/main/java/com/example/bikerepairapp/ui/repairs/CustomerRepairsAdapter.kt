package com.example.bikerepairapp.ui.repairs

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class CustomerRepairsAdapter(
    private val onConfirm: ((CustomerRepairRow) -> Unit)? = null,
    private val onTrackMechanic: ((CustomerRepairRow) -> Unit)? = null
) : RecyclerView.Adapter<CustomerRepairsAdapter.VH>() {

    private val items = mutableListOf<CustomerRepairRow>()

    private val confirmClickedIds = mutableSetOf<String>()

    fun submitList(newItems: List<CustomerRepairRow>) {
        items.clear()
        items.addAll(newItems)

        val idsNow = items.map { it.id }.toSet()
        confirmClickedIds.retainAll(idsNow)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_repair, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(
            row = items[position],
            onConfirm = onConfirm,
            onTrackMechanic = onTrackMechanic,
            confirmClickedIds = confirmClickedIds
        ) { clickedId ->
            confirmClickedIds.add(clickedId)

            val p = holder.adapterPosition
            if (p != RecyclerView.NO_POSITION) {
                notifyItemChanged(p)
            }
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)

        private val tvStatus: TextView? = itemView.findViewById(R.id.tvStatus)
        private val btnConfirm: MaterialButton? = itemView.findViewById(R.id.btnConfirmRepair)
        private val btnTrackMechanic: MaterialButton? = itemView.findViewById(R.id.btnTrackMechanic)

        fun bind(
            row: CustomerRepairRow,
            onConfirm: ((CustomerRepairRow) -> Unit)?,
            onTrackMechanic: ((CustomerRepairRow) -> Unit)?,
            confirmClickedIds: Set<String>,
            onOptimisticHide: (String) -> Unit
        ) {
            tvIssue.text = row.issue
            tvMeta.text = "${row.date} \u2022 ${row.location}"

            val prettyStatus = when (row.status) {
                "pending" -> "Pending"
                "accepted" -> "Accepted"
                "completed_pending" -> "Waiting for your confirmation"
                "closed" -> "Closed \u2705"
                else -> row.status
            }

            val mech = row.mechanicName.takeIf { it.isNotBlank() }
            tvStatus?.text = if (mech != null && row.status != "pending") {
                "Status: $prettyStatus \u2022 Mechanic: $mech"
            } else {
                "Status: $prettyStatus"
            }

            // ---- Track mechanic button (visible for accepted repairs) ----
            val canTrack = row.status == "accepted" && onTrackMechanic != null
            btnTrackMechanic?.apply {
                visibility = if (canTrack) View.VISIBLE else View.GONE

                val blue = Color.parseColor("#4472C4")
                val dark = Color.parseColor("#101010")

                if (row.locationSharingActive) {
                    text = "\uD83D\uDFE2 Track mechanic"
                    setTextColor(blue)
                    strokeColor = ColorStateList.valueOf(blue)
                } else {
                    text = "View mechanic location"
                    setTextColor(blue)
                    strokeColor = ColorStateList.valueOf(blue)
                }

                backgroundTintList = ColorStateList.valueOf(dark)
                strokeWidth = dp(1)
                isAllCaps = false
                cornerRadius = dp(12)

                setOnClickListener { onTrackMechanic?.invoke(row) }
            }

            // ---- Confirm repair button ----
            val canConfirm =
                row.status == "completed_pending" &&
                        onConfirm != null &&
                        !confirmClickedIds.contains(row.id)

            btnConfirm?.apply {
                visibility = if (canConfirm) View.VISIBLE else View.GONE

                val blue = Color.parseColor("#4472C4")
                backgroundTintList = ColorStateList.valueOf(blue)
                setTextColor(Color.WHITE)
                isAllCaps = false
                text = "Confirm repair"

                setOnClickListener {
                    onOptimisticHide(row.id)
                    onConfirm?.invoke(row)
                }
            }
        }

        private fun dp(value: Int): Int {
            val density = itemView.resources.displayMetrics.density
            return (value * density).toInt()
        }
    }
}
