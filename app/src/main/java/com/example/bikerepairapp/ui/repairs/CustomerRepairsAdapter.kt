package com.example.bikerepairapp.ui.repairs

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class CustomerRepairsAdapter(
    private val onConfirm: ((CustomerRepairRow) -> Unit)? = null,
    private val onDelete: ((CustomerRepairRow) -> Unit)? = null
) : RecyclerView.Adapter<CustomerRepairsAdapter.VH>() {

    private val items = mutableListOf<CustomerRepairRow>()
    private var highlightedId: String? = null

    fun submitList(newItems: List<CustomerRepairRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setHighlight(id: String?) {
        highlightedId = id
        notifyDataSetChanged()
    }
    fun getItems(): List<CustomerRepairRow> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_repair, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        if (item.id == highlightedId) {
            holder.itemView.setBackgroundColor(Color.parseColor("#4DFFD700"))
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        holder.bind(item, onConfirm, onDelete)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvStatus: TextView? = itemView.findViewById(R.id.tvStatus)
        private val btnConfirm: MaterialButton? = itemView.findViewById(R.id.btnConfirmRepair)
        private val btnDelete: ImageButton? = itemView.findViewById(R.id.btnDeleteRepair)

        fun bind(
            row: CustomerRepairRow,
            onConfirm: ((CustomerRepairRow) -> Unit)?,
            onDelete: ((CustomerRepairRow) -> Unit)?
        ) {
            tvIssue.text = row.issue
            tvMeta.text = "${row.date} • ${row.location}"

            // Long click still works as a backup
            itemView.setOnLongClickListener {
                onDelete?.invoke(row)
                true
            }

            // Visible Delete Button
            btnDelete?.setOnClickListener {
                onDelete?.invoke(row)
            }

            // Hide delete if not pending
            btnDelete?.visibility = if (row.status == "pending") View.VISIBLE else View.GONE

            val prettyStatus = when (row.status) {
                "pending" -> "Pending"
                "accepted" -> "Accepted"
                "completed_pending" -> "Waiting for confirmation"
                "closed" -> "Closed ✅"
                else -> row.status
            }

            val mech = row.mechanicName.takeIf { it.isNotBlank() }
            tvStatus?.text = if (mech != null && row.status != "pending") {
                "Status: $prettyStatus • Mechanic: $mech"
            } else {
                "Status: $prettyStatus"
            }

            btnConfirm?.visibility = if (row.status == "completed_pending") View.VISIBLE else View.GONE
            btnConfirm?.setOnClickListener { onConfirm?.invoke(row) }
        }
    }
}