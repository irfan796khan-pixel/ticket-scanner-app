package com.ticketscanner.app.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ticketscanner.app.R
import com.ticketscanner.app.databinding.FragmentScannerBinding
import com.ticketscanner.app.util.PriceMatrix
import com.ticketscanner.app.util.ScanResult
import com.ticketscanner.app.util.TicketParser
import com.ticketscanner.app.viewmodel.TicketViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TicketViewModel by activityViewModels()
    private lateinit var cameraExecutor: ExecutorService
    private var isCameraActive = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required for scanning", Toast.LENGTH_LONG).show()
            showManualInputOnly()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupUI()
        observeViewModel()
        checkCameraPermission()
    }

    private fun setupUI() {
        // Toggle between camera and manual input
        binding.btnToggleInput.setOnClickListener {
            if (binding.manualInputLayout.visibility == View.VISIBLE) {
                binding.manualInputLayout.visibility = View.GONE
                binding.cameraPreviewContainer.visibility = View.VISIBLE
                binding.btnToggleInput.text = getString(R.string.manual_input)
                if (!isCameraActive) checkCameraPermission()
            } else {
                binding.manualInputLayout.visibility = View.VISIBLE
                binding.cameraPreviewContainer.visibility = View.GONE
                binding.btnToggleInput.text = getString(R.string.camera_scan)
            }
        }

        // Manual input submit
        binding.btnSubmitManual.setOnClickListener {
            handleManualInput()
        }

        // Manual code submit (full 29-digit code)
        binding.btnSubmitCode.setOnClickListener {
            val code = binding.etFullCode.text.toString().trim()
            if (code.isNotEmpty()) {
                viewModel.processScannedCode(code)
                binding.etFullCode.text?.clear()
            } else {
                binding.etFullCode.error = "Enter a 29-digit code"
            }
        }
    }

    private fun handleManualInput() {
        val ticketId = binding.etTicketId.text.toString().trim()
        val ticketNumberStr = binding.etTicketNumber.text.toString().trim()
        val priceStr = binding.etManualPrice.text.toString().trim()

        // Validate ticket ID
        if (!TicketParser.isValidTicketId(ticketId)) {
            binding.etTicketId.error = "Enter a valid 10-digit Ticket ID"
            return
        }

        // Validate ticket number
        if (!TicketParser.isValidTicketNumber(ticketNumberStr)) {
            binding.etTicketNumber.error = "Enter a valid ticket number (0-999)"
            return
        }

        val ticketNumber = ticketNumberStr.toInt()

        // Optional manual price (in dollars, convert to cents)
        val manualPrice = if (priceStr.isNotEmpty()) {
            val dollars = priceStr.toDoubleOrNull()
            if (dollars == null || dollars < 0) {
                binding.etManualPrice.error = "Enter a valid price"
                return
            }
            (dollars * 100).toInt()
        } else {
            null
        }

        viewModel.processManualEntry(ticketId, ticketNumber, manualPrice)

        // Clear inputs
        binding.etTicketId.text?.clear()
        binding.etTicketNumber.text?.clear()
        binding.etManualPrice.text?.clear()
    }

    private fun observeViewModel() {
        viewModel.scanResult.observe(viewLifecycleOwner) { result ->
            result?.let { handleScanResult(it) }
        }

        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.progressBar.visibility = if (processing) View.VISIBLE else View.GONE
        }

        viewModel.lastScannedTicket.observe(viewLifecycleOwner) { ticket ->
            ticket?.let {
                binding.resultCard.visibility = View.VISIBLE
                binding.tvResultTicketId.text = getString(R.string.ticket_id_format, it.ticketId)
                binding.tvResultTicketCount.text = getString(R.string.ticket_count_format, it.ticketNumber)
                binding.tvResultPrice.text = getString(R.string.price_format, PriceMatrix.formatPrice(it.ticketPrice))
                binding.tvResultTotalSale.text = getString(R.string.total_sale_format, PriceMatrix.formatPrice(it.totalSale))

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                binding.tvResultTimestamp.text = getString(R.string.last_scan_format, dateFormat.format(Date(it.lastScanTimestamp)))
            }
        }
    }

    private fun handleScanResult(result: ScanResult) {
        when (result) {
            is ScanResult.NewTicket -> {
                val msg = "New ticket: ${result.ticket.ticketId}"
                binding.tvScanStatus.text = msg
                binding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_new))

                if (result.needsManualPrice) {
                    showManualPriceDialog(result.ticket.ticketId, result.ticket.ticketNumber)
                }
            }

            is ScanResult.SaleRecorded -> {
                val msg = "Sale: ${result.ticketsSold} tickets = ${PriceMatrix.formatPrice(result.saleAmount)}"
                binding.tvScanStatus.text = msg
                binding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_sale))
            }

            is ScanResult.QuantityIncreased -> {
                showQuantityIncreasedDialog(result)
            }

            is ScanResult.NoChange -> {
                binding.tvScanStatus.text = getString(R.string.no_change_detected)
                binding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_info))
            }

            is ScanResult.Error -> {
                binding.tvScanStatus.text = result.message
                binding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error))
                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
            }
        }
        binding.tvScanStatus.visibility = View.VISIBLE
    }

    private fun showQuantityIncreasedDialog(result: ScanResult.QuantityIncreased) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quantity Increased")
            .setMessage(
                "Previously scanned quantity not fully accounted for.\n\n" +
                        "Old quantity: ${result.oldTicketNumber}\n" +
                        "New quantity: ${result.newTicketNumber}\n\n" +
                        "Has the remaining stock been sold?"
            )
            .setPositiveButton("Yes, All Sold") { _, _ ->
                viewModel.confirmRemainingStockSold(result.ticket)
            }
            .setNegativeButton("No, Ignore") { dialog, _ ->
                dialog.dismiss()
                binding.tvScanStatus.text = getString(R.string.scan_ignored)
                binding.tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_info))
            }
            .setCancelable(false)
            .show()
    }

    private fun showManualPriceDialog(ticketId: String, ticketNumber: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_manual_price, null)
        val etPrice = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_dialog_price)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Ticket Price")
            .setMessage("No matching price rule for ticket number $ticketNumber.\nPlease enter the price per ticket.")
            .setView(dialogView)
            .setPositiveButton("Set Price") { _, _ ->
                val priceStr = etPrice.text.toString().trim()
                val dollars = priceStr.toDoubleOrNull()
                if (dollars != null && dollars > 0) {
                    viewModel.updateTicketPrice(ticketId, (dollars * 100).toInt())
                } else {
                    Toast.makeText(requireContext(), "Invalid price. Please update in settings.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Camera Permission")
                    .setMessage("Camera is needed to scan Data Matrix codes. You can also use manual input.")
                    .setPositiveButton("Grant") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Use Manual Input") { _, _ ->
                        showManualInputOnly()
                    }
                    .show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }

            val barcodeOptions = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX, Barcode.FORMAT_QR_CODE)
                .build()

            val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            for (barcode in barcodes) {
                                barcode.rawValue?.let { value ->
                                    activity?.runOnUiThread {
                                        viewModel.processScannedCode(value)
                                    }
                                }
                            }
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                } else {
                    imageProxy.close()
                }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                isCameraActive = true
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Camera failed: ${e.message}", Toast.LENGTH_SHORT).show()
                showManualInputOnly()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun showManualInputOnly() {
        binding.cameraPreviewContainer.visibility = View.GONE
        binding.manualInputLayout.visibility = View.VISIBLE
        binding.btnToggleInput.text = getString(R.string.camera_scan)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
