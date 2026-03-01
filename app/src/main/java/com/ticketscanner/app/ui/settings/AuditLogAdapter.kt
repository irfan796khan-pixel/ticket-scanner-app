package com.ticketscanner.app.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ticketscanner.app.data.entity.AuditLog
import com.ticketscanner.app.databinding.ItemAuditLogBinding
import com.ticketscanner.app.util.PriceMatrix
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditLogAdapter :
    ListAdapter<AuditLog, AuditLogAdapter.AuditLogViewHolder>(AuditLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditLogViewHolder {
        val binding = ItemAuditLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AuditLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AuditLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AuditLogViewHolder(
        private val binding: ItemAuditLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(log: AuditLog) {
            binding.tvLogTimestamp.text = dateFormat.format(Date(log.timestamp))
            binding.tvLogTicketId.text = "Ticket: ${log.ticketId}"
            binding.tvLogAction.text = log.actionType
            binding.tvLogDetails.text = log.details

            if (log.amountChange > 0) {
                binding.tvLogAmount.text = "+${PriceMatrix.formatPrice(log.amountChange)}"
                binding.tvLogAmount.visibility = android.view.View.VISIBLE
            } else {
                binding.tvLogAmount.visibility = android.view.View.GONE
            }
        }
    }

    class AuditLogDiffCallback : DiffUtil.ItemCallback<AuditLog>() {
        override fun areItemsTheSame(oldItem: AuditLog, newItem: AuditLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AuditLog, newItem: AuditLog): Boolean {
            return oldItem == newItem
        }
    }
}
