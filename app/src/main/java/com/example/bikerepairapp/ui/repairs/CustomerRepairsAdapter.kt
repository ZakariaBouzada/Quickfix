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
    private val onConfirm: ((CustomerRepairRow) -> Unit)? = null
) : RecyclerView.Adapter<CustomerRepairsAdapter.VH>() {

    private val items = mutableListOf<CustomerRepairRow>()

    // ✅ local UI state: hide confirm after user taps it (until Firestore updates to "closed")
    private val confirmClickedIds = mutableSetOf<String>()

    fun submitList(newItems: List<CustomerRepairRow>) {
        items.clear()
        items.addAll(newItems)

        // optional cleanup so the set doesn't grow forever
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
            confirmClickedIds = confirmClickedIds
        ) { clickedId ->
            // hide immediately
            confirmClickedIds.add(clickedId)

            // ✅ compatible with older RecyclerView
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

        fun bind(
            row: CustomerRepairRow,
            onConfirm: ((CustomerRepairRow) -> Unit)?,
            confirmClickedIds: Set<String>,
            onOptimisticHide: (String) -> Unit
        ) {
            tvIssue.text = row.issue
            tvMeta.text = "${row.date} • ${row.location}"

            val prettyStatus = when (row.status) {
                "pending" -> "Pending"
                "accepted" -> "Accepted"
                "completed_pending" -> "Waiting for your confirmation"
                "closed" -> "Closed ✅"
                else -> row.status
            }

            val mech = row.mechanicName.takeIf { it.isNotBlank() }
            tvStatus?.text = if (mech != null && row.status != "pending") {
                "Status: $prettyStatus • Mechanic: $mech"
            } else {
                "Status: $prettyStatus"
            }

            // ✅ Show confirm only when completed_pending AND user hasn't clicked it already
            val canConfirm =
                row.status == "completed_pending" &&
                        onConfirm != null &&
                        !confirmClickedIds.contains(row.id)

            btnConfirm?.apply {
                visibility = if (canConfirm) View.VISIBLE else View.GONE

                val yellow = Color.parseColor("#FFFF00")
                backgroundTintList = ColorStateList.valueOf(yellow)
                setTextColor(Color.BLACK)
                isAllCaps = false
                text = "Confirm repair"

                setOnClickListener {
                    // ✅ hide instantly so it doesn't pop back while rating screen is open
                    onOptimisticHide(row.id)
                    onConfirm?.invoke(row)
                }
            }
        }
    }
}
