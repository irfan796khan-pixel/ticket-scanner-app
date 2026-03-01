package com.ticketscanner.app.ui.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ticketscanner.app.R
import com.ticketscanner.app.databinding.FragmentSummaryBinding
import com.ticketscanner.app.util.PriceMatrix
import com.ticketscanner.app.viewmodel.TicketViewModel

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketViewModel by activityViewModels()
    private lateinit var adapter: TicketSummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TicketSummaryAdapter()
        binding.recyclerViewTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTickets.adapter = adapter

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.allTickets.observe(viewLifecycleOwner) { tickets ->
            adapter.submitList(tickets)
            binding.tvNoTickets.visibility = if (tickets.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewTickets.visibility = if (tickets.isEmpty()) View.GONE else View.VISIBLE
            binding.tvTicketCount.text = getString(R.string.total_tickets_format, tickets.size)
        }

        viewModel.grandTotal.observe(viewLifecycleOwner) { total ->
            val displayTotal = total ?: 0
            binding.tvGrandTotal.text = getString(R.string.grand_total_format, PriceMatrix.formatPrice(displayTotal))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
