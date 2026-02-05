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

    fun submitList(newItems: List<CustomerRepairRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // IMPORTANT: this must match the XML you actually use for customer rows
        // If your file name is different, change this line only.
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_repair, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onConfirm)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // REQUIRED (must exist in XML)
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)

        // OPTIONAL (won't crash if missing)
        private val tvStatus: TextView? = itemView.findViewById(R.id.tvStatus)
        private val btnConfirm: MaterialButton? = itemView.findViewById(R.id.btnConfirmRepair)

        fun bind(
            row: CustomerRepairRow,
            onConfirm: ((CustomerRepairRow) -> Unit)?
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

            // Show confirm only when completed_pending
            val canConfirm = row.status == "completed_pending" && onConfirm != null

            btnConfirm?.apply {
                visibility = if (canConfirm) View.VISIBLE else View.GONE

                // QuickFix style
                val yellow = Color.parseColor("#FFFF00")
                backgroundTintList = ColorStateList.valueOf(yellow)
                setTextColor(Color.BLACK)
                isAllCaps = false
                text = "Confirm repair"

                setOnClickListener { onConfirm?.invoke(row) }
            }
        }
    }
}
