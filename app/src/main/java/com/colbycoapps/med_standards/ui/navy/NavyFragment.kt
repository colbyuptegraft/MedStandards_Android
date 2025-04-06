package com.colbycoapps.med_standards.ui.navy

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standards.databinding.FragmentNavyBinding
import com.colbycoapps.med_standards.ui.Utils
import com.colbycoapps.med_standards.ui.adapter.FileAdapter
import com.colbycoapps.med_standards.ui.pdfview.PdfViewerActivity

class NavyFragment : Fragment(), FileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentNavyBinding
    private val viewModel: NavyViewModel by viewModels()
    private lateinit var adapter: FileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNavyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModelObservers()
        if(!Utils.storage)
            viewModel.loadFiles()
        else
            viewModel.loadFilesStorage(requireActivity())

        return root
    }

    private fun setupViewModelObservers() {
        viewModel.files.observe(viewLifecycleOwner) { fileList ->
            if(!Utils.storage) {
                adapter = FileAdapter(fileList, this, false)
                binding.listNavy.layoutManager = LinearLayoutManager(requireActivity())
                binding.listNavy.adapter = adapter
            }
        }

        viewModel.filesStorage.observe(viewLifecycleOwner) { fileList ->
            adapter = FileAdapter(fileList, this, true)
            binding.listNavy.layoutManager = LinearLayoutManager(requireActivity())
            binding.listNavy.adapter = adapter
        }
    }


    override fun onFileClick(fileName: String, fileUrl: String) {
        if (Utils.countFree > 0) {
            val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", fileUrl)
                if (fileName != "") putExtra("PDF_NAME", fileName)
                putExtra("COLOR", "navy")
            }
            startActivity(intent)
        }
        else
        {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Subscription plan")
            builder.setMessage("The number of free views has been exhausted! You must subscribe to view.!")
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
}