package com.colbycoapps.med_standards.ui.navy

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standards.R
import com.colbycoapps.med_standards.databinding.FragmentNavyBinding
import com.colbycoapps.med_standards.ui.Utils
import com.colbycoapps.med_standards.ui.adapter.FileAdapter
import com.colbycoapps.med_standards.ui.pdfview.PdfViewerActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

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
            builder.setTitle("Subscription Required")
            builder.setMessage("You've exhausted the number of free documents views! You must subscribe to view any additional documents.")
            builder.setPositiveButton("Subscribe Now") { dialog, _ ->
                val navController = requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
                val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
                navView.selectedItemId = navView.menu.getItem(4).itemId
                navView.visibility = View.GONE
                navController.navigate(R.id.navigation_subscriptin)
                dialog.dismiss()
            }
            builder.setNegativeButton("Close"){ dialog, _ ->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
}