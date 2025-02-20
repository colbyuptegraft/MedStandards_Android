package com.colbycoapps.med_standarts.ui.army

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standarts.databinding.FragmentArmyBinding
import com.colbycoapps.med_standarts.ui.adapter.FileAdapter
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity


class ArmyFragment : Fragment(), FileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentArmyBinding
    private val viewModel: ArmyViewModel by viewModels()
    private lateinit var adapter: FileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentArmyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModelObservers()
        viewModel.loadFiles() // Завантажуємо файли зі збереженої мапи

        return root
    }

    private fun setupViewModelObservers() {
        viewModel.files.observe(viewLifecycleOwner) { fileList ->
            adapter = FileAdapter(fileList, this)
            binding.listArmy.layoutManager = LinearLayoutManager(requireActivity())
            binding.listArmy.adapter = adapter
        }
    }

    override fun onFileClick(fileName: String) {
        val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
            putExtra("PDF_URL", fileName)
            putExtra("COLOR", "army")
        }
        startActivity(intent)
    }
}
