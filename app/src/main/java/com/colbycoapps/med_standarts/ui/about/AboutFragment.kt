package com.colbycoapps.med_standarts.ui.about

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.colbycoapps.med_standarts.R
import com.colbycoapps.med_standarts.databinding.FragmentAboutBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView


private const val REQUEST_CODE_POST_NOTIFICATIONS = 111

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding
    private var synchronization = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        if(Utils.storage) {
            binding.button.setText("File Synchronization")
            synchronization = true
        }

        binding.button.setOnClickListener {
            if(Utils.filesMap.isNotEmpty() && Utils.afFilesMap.isNotEmpty() && Utils.isInternetAvailable(requireActivity())) {
                requestNotificationPermission()
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .build()
                WorkManager.getInstance(requireContext()).enqueue(workRequest)
                //downloadAllFromMaps()
            }
        }

        binding.buttonAbout.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigate(R.id.action_navigation_about_to_navigation_about_app)
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
        }

        binding.buttonSubs.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigate(R.id.action_navigation_about_to_navigation_subscriptin)
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.VISIBLE
    }



    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        }
    }

}