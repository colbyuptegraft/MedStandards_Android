package com.colbycoapps.med_standarts.ui.dod

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standarts.databinding.FragmentDodBinding
import com.colbycoapps.med_standarts.ui.adapter.FileAdapter
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity

class DodFragment : Fragment(), FileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentDodBinding
    private val viewModel: DodViewModel by viewModels()
    private lateinit var adapter: FileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDodBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModelObservers()
        viewModel.loadFiles() // Завантажуємо файли зі збереженої мапи

        return root
    }

    private fun setupViewModelObservers() {
        viewModel.files.observe(viewLifecycleOwner) { fileList ->
            adapter = FileAdapter(fileList, this)
            binding.listDod.layoutManager = LinearLayoutManager(requireActivity())
            binding.listDod.adapter = adapter
        }
    }

    override fun onFileClick(fileName: String) {
        val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
            putExtra("PDF_URL", fileName)
            putExtra("COLOR", "dod")
        }
        startActivity(intent)
    }
}