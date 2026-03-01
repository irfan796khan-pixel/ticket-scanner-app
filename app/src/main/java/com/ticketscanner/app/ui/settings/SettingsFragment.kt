package com.ticketscanner.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ticketscanner.app.R
import com.ticketscanner.app.databinding.FragmentSettingsBinding
import com.ticketscanner.app.util.PriceMatrix
import com.ticketscanner.app.viewmodel.TicketViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketViewModel by activityViewModels()
    private lateinit var auditLogAdapter: AuditLogAdapter
    private lateinit var priceMatrixAdapter: PriceMatrixAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPriceMatrix()
        setupAuditLog()
        setupActions()
        observeViewModel()
    }

    private fun setupPriceMatrix() {
        priceMatrixAdapter = PriceMatrixAdapter()
        binding.recyclerViewPriceMatrix.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPriceMatrix.adapter = priceMatrixAdapter

        // Load current price matrix
        val matrix = PriceMatrix.getMatrix()
        priceMatrixAdapter.submitList(matrix.entries.map { PriceMatrixItem(it.key, it.value) })

        // Nearest match toggle
        binding.switchNearestMatch.isChecked = PriceMatrix.isNearestMatchEnabled()
        binding.switchNearestMatch.setOnCheckedChangeListener { _, isChecked ->
            PriceMatrix.setNearestMatchEnabled(requireContext(), isChecked)
        }

        // Add custom price rule
        binding.btnAddPriceRule.setOnClickListener {
            showAddPriceRuleDialog()
        }

        // Reset to defaults
        binding.btnResetPrices.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset Price Matrix")
                .setMessage("Reset all price rules to defaults?")
                .setPositiveButton("Reset") { _, _ ->
                    PriceMatrix.resetToDefaults(requireContext())
                    refreshPriceMatrix()
                    Toast.makeText(requireContext(), "Price matrix reset", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupAuditLog() {
        auditLogAdapter = AuditLogAdapter()
        binding.recyclerViewAuditLog.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAuditLog.adapter = auditLogAdapter
    }

    private fun setupActions() {
        // Clear all data
        binding.btnClearData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("This will permanently delete all ticket records and audit logs. This action cannot be undone.")
                .setPositiveButton("Clear All") { _, _ ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Are you sure?")
                        .setMessage("All sales data will be lost permanently.")
                        .setPositiveButton("Yes, Delete Everything") { _, _ ->
                            viewModel.clearAllData()
                            Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Toggle audit log visibility
        binding.btnToggleAuditLog.setOnClickListener {
            if (binding.auditLogContainer.visibility == View.VISIBLE) {
                binding.auditLogContainer.visibility = View.GONE
                binding.btnToggleAuditLog.text = getString(R.string.show_audit_log)
            } else {
                binding.auditLogContainer.visibility = View.VISIBLE
                binding.btnToggleAuditLog.text = getString(R.string.hide_audit_log)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.allAuditLogs.observe(viewLifecycleOwner) { logs ->
            auditLogAdapter.submitList(logs)
            binding.tvNoLogs.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddPriceRuleDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_price_rule, null)
        val etTicketNumber = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_rule_ticket_number)
        val etPrice = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_rule_price)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Price Rule")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val ticketNum = etTicketNumber.text.toString().toIntOrNull()
                val price = etPrice.text.toString().toDoubleOrNull()

                if (ticketNum != null && price != null && ticketNum > 0 && price > 0) {
                    PriceMatrix.setCustomPrice(requireContext(), ticketNum, (price * 100).toInt())
                    refreshPriceMatrix()
                    Toast.makeText(requireContext(), "Price rule added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshPriceMatrix() {
        val matrix = PriceMatrix.getMatrix()
        priceMatrixAdapter.submitList(matrix.entries.map { PriceMatrixItem(it.key, it.value) })
        binding.switchNearestMatch.isChecked = PriceMatrix.isNearestMatchEnabled()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
