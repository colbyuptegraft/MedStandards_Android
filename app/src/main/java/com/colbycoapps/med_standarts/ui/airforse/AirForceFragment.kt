package com.colbycoapps.med_standarts.ui.airforse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standarts.R
import com.colbycoapps.med_standarts.databinding.FragmentAirforseBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.colbycoapps.med_standarts.ui.adapter.AirForceFileAdapter
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity
import com.google.firebase.storage.StorageReference

class AirForceFragment : Fragment(), AirForceFileAdapter.OnFileClickListener {

    private lateinit var binding: FragmentAirforseBinding
    private val viewModel: AirForceViewModel by viewModels()
    private lateinit var adapter: AirForceFileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAirforseBinding.inflate(inflater, container, false)

        setupRecyclerView()
        setupObservers()

        if(!Utils.storage) {
//            val afFilesMap2: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
//            afFilesMap2["main"] = Utils.afFilesMap["main"]!!
//            afFilesMap2["bomc"] = Utils.afFilesMap["bomc"]!!
//            afFilesMap2["fsToolkit"] = Utils.afFilesMap["fsToolkit"]!!
//            afFilesMap2["AFIs"] = Utils.afFilesMap["AFIs"]!!
//            afFilesMap2["RSVs"] = Utils.afFilesMap["RSVs"]!!

//            Utils.afFilesMap = afFilesMap2
            viewModel.loadFiles()
        }
        else
            viewModel.loadFolderStorage(requireActivity())

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if(!Utils.storage) {
                if (!viewModel.navigateBack()) requireActivity().finish()
            }
            else
            {
                viewModel.loadFolderStorage(requireActivity())
            }
            val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(false)
            actionBar?.setDisplayShowHomeEnabled(false)
            actionBar?.title = getString(R.string.title_airforse)
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

            if(!Utils.storage) {
                //val sortedList = list.sortedBy { it.first }
                adapter.updateItems(list)
            }
        }

        viewModel.filesAndFoldersStorage.observe(viewLifecycleOwner) { list ->
            //val sortedList = list.sortedBy { it.first }
            adapter.updateItems(list)
        }
    }

    override fun onFileClick(itemName: String, itemValue: String) {
        if (itemValue == "folder") {
            if(!Utils.storage) {
                val newPath = itemName
                viewModel.loadFiles(newPath)
            }
            else
            {
                viewModel.loadFilesFolderStorage(requireActivity(), itemName)
            }
            val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
            actionBar?.setDisplayHomeAsUpEnabled(true)
            actionBar?.setDisplayShowHomeEnabled(true)
            when(itemName)
            {
                "AFIs" ->  actionBar?.title = "Other AFIs"
                "bomc" ->  actionBar?.title = "BOMC"
                "fsToolkit" ->  actionBar?.title = "Flight Surgeon Toolkit"
                "main" ->  actionBar?.title = "Main Documents"
                else -> actionBar?.title = itemName
            }

        } else {
            // Це URL файлу
            val intent = if(!Utils.storage)Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", itemValue)
                putExtra("COLOR", "airForce")
            }
            else
                Intent(requireContext(), PdfViewerActivity::class.java).apply {
                    putExtra("PDF_NAME", itemName)
                    putExtra("PDF_URL", itemValue)
                    putExtra("COLOR", "airForce")
                }

            startActivity(intent)
        }
    }
}
