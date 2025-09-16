package com.colbycoapps.med_standards.ui.dod

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standards.R
import com.colbycoapps.med_standards.databinding.FragmentDodBinding
import com.colbycoapps.med_standards.ui.Utils
import com.colbycoapps.med_standards.ui.adapter.FileAdapter
import com.colbycoapps.med_standards.ui.pdfview.PdfViewerActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class DodFragment : Fragment(), FileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentDodBinding
    private val viewModel: DodViewModel by viewModels()
    private var adapter: FileAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDodBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupViewModelObservers()
        
        if(!Utils.storage)
            viewModel.loadFiles()
        else
            viewModel.loadFilesStorage(requireActivity())

        return binding.root
    }
    
    private fun setupRecyclerView() {
        // Create LayoutManager only once
        binding.listDod.layoutManager = LinearLayoutManager(requireContext())
        
        // Optimize RecyclerView performance
        binding.listDod.setHasFixedSize(true)
        binding.listDod.setItemViewCacheSize(20)
    }

    private fun setupViewModelObservers() {
        viewModel.files.observe(viewLifecycleOwner) { fileList ->
            if(!Utils.storage) {
                if (adapter == null) {
                    adapter = FileAdapter(fileList, this, false)
                    binding.listDod.adapter = adapter
                } else {
                    // Update existing adapter instead of creating new one
                    (adapter as? FileAdapter)?.updateItems(fileList)
                }
            }
        }

        viewModel.filesStorage.observe(viewLifecycleOwner) { fileList ->
            if (adapter == null) {
                adapter = FileAdapter(fileList, this, true)
                binding.listDod.adapter = adapter
            } else {
                // Update existing adapter instead of creating new one
                (adapter as? FileAdapter)?.updateItems(fileList)
            }
        }
    }


    override fun onFileClick(fileName: String, fileUrl: String) {
        Log.d("DOD_FRAGMENT", "PDF clicked - premium: ${Utils.premium}, countFree: ${Utils.countFree}")
        // COMMENTED OUT: Subscription check disabled - always allow PDF access
        /*
        if (Utils.premium || Utils.countFree > 0) {
        */
            val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", fileUrl)
                if (fileName != "") putExtra("PDF_NAME", fileName)
                putExtra("COLOR", "dod")
            }
            startActivity(intent)
        /*
        } else
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
        */
    }
}