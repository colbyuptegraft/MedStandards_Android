package com.colbycoapps.med_standarts.ui.airforse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standarts.databinding.FragmentAirforseBinding
import com.colbycoapps.med_standarts.ui.adapter.AirForceFileAdapter
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity

class AirForceFragment : Fragment(), AirForceFileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentAirforseBinding
    private val viewModel: AirForceViewModel by viewModels()
    private lateinit var adapter: AirForceFileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAirforseBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupObservers()

        // Завантажуємо корінь "af"
        viewModel.loadFiles()

        // Кнопка "Назад"
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!viewModel.navigateBack()) requireActivity().finish()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = AirForceFileAdapter(emptyList(), this)
        binding.listAirForse.layoutManager = LinearLayoutManager(requireContext())
        binding.listAirForse.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.filesAndFolders.observe(viewLifecycleOwner) { list ->
            adapter.updateItems(list)
        }
    }

    override fun onFileClick(itemName: String, itemValue: String) {
        if (itemValue == "folder") {
            // Користувач натиснув на підпапку
            val newPath = itemName
            viewModel.loadFiles(newPath)
        } else {
            // Це URL файлу
            val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", itemValue)
                putExtra("COLOR", "airForce")
            }
            startActivity(intent)
        }
    }
}
