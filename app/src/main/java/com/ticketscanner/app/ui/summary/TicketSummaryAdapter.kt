package com.ticketscanner.app.ui.summary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ticketscanner.app.R
import com.ticketscanner.app.data.entity.Ticket
import com.ticketscanner.app.databinding.ItemTicketSummaryBinding
import com.ticketscanner.app.util.PriceMatrix
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketSummaryAdapter :
    ListAdapter<Ticket, TicketSummaryAdapter.TicketViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TicketViewHolder(
        private val binding: ItemTicketSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())

        fun bind(ticket: Ticket) {
            binding.tvTicketId.text = ticket.ticketId
            binding.tvTicketCount.text = "Remaining: ${ticket.ticketNumber}"
            binding.tvTicketPrice.text = "Price: ${PriceMatrix.formatPrice(ticket.ticketPrice)}/ea"
            binding.tvTicketTotalSale.text = "Sales: ${PriceMatrix.formatPrice(ticket.totalSale)}"
            binding.tvLastScan.text = "Last scan: ${dateFormat.format(Date(ticket.lastScanTimestamp))}"

            if (ticket.isSoldOut) {
                binding.tvSoldOutBadge.visibility = android.view.View.VISIBLE
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.sold_out_bg)
                )
            } else {
                binding.tvSoldOutBadge.visibility = android.view.View.GONE
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.card_bg)
                )
            }
        }
    }

    class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {
        override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem.ticketId == newItem.ticketId
        }

        override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
            return oldItem == newItem
        }
    }
}
