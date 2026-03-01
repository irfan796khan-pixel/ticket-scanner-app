package com.ticketscanner.app.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ticketscanner.app.databinding.ItemPriceRuleBinding
import com.ticketscanner.app.util.PriceMatrix

data class PriceMatrixItem(
    val ticketNumber: Int,
    val priceInCents: Int
)

class PriceMatrixAdapter :
    ListAdapter<PriceMatrixItem, PriceMatrixAdapter.PriceRuleViewHolder>(PriceRuleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceRuleViewHolder {
        val binding = ItemPriceRuleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PriceRuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceRuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PriceRuleViewHolder(
        private val binding: ItemPriceRuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PriceMatrixItem) {
            binding.tvRuleTicketNumber.text = "Qty: ${item.ticketNumber}"
            binding.tvRulePrice.text = PriceMatrix.formatPrice(item.priceInCents)
        }
    }

    class PriceRuleDiffCallback : DiffUtil.ItemCallback<PriceMatrixItem>() {
        override fun areItemsTheSame(oldItem: PriceMatrixItem, newItem: PriceMatrixItem): Boolean {
            return oldItem.ticketNumber == newItem.ticketNumber
        }

        override fun areContentsTheSame(oldItem: PriceMatrixItem, newItem: PriceMatrixItem): Boolean {
            return oldItem == newItem
        }
    }
}
