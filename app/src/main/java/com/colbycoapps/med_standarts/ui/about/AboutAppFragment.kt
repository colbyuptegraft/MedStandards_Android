package com.colbycoapps.med_standarts.ui.about

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import com.colbycoapps.med_standarts.R
import com.colbycoapps.med_standarts.databinding.FragmentAboutAppBinding
import com.colbycoapps.med_standarts.databinding.FragmentAirforseBinding
import com.colbycoapps.med_standarts.databinding.FragmentDodBinding
import com.colbycoapps.med_standarts.databinding.FragmentNavyBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.colbycoapps.med_standarts.ui.adapter.FileAdapter
import com.colbycoapps.med_standarts.ui.dod.DodViewModel
import com.colbycoapps.med_standarts.ui.pdfview.PdfViewerActivity

class AboutAppFragment : Fragment() {

    private lateinit var binding: FragmentAboutAppBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAboutAppBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

}