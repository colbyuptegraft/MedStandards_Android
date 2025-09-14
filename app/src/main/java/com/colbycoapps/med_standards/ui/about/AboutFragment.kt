package com.colbycoapps.med_standards.ui.about

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.colbycoapps.med_standards.R
import com.colbycoapps.med_standards.databinding.FragmentAboutBinding
import com.colbycoapps.med_standards.ui.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.play.core.review.ReviewManagerFactory


private const val REQUEST_CODE_POST_NOTIFICATIONS = 111

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding
    private var synchronization = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        if(Utils.storage) {
            binding.button.setText("Sync Content")
            synchronization = true
        }

        // SUBSCRIPTION DISABLED - Hide subscription button
        // if(Utils.premium)
            binding.buttonSubs.visibility = View.GONE

        binding.button.setOnClickListener {
            if(Utils.filesMap.isNotEmpty() && Utils.afFilesMap.isNotEmpty() && Utils.isInternetAvailable(requireActivity())) {
                requestNotificationPermission()
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .build()
                WorkManager.getInstance(requireContext()).enqueue(workRequest)
                //downloadAllFromMaps()
            }
        }

        binding.buttonSupport.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("info@doc-apps.com"))
                putExtra(Intent.EXTRA_SUBJECT, "${requireActivity().getString(R.string.app_name)} - Android")
            }
            startActivity(emailIntent)
        }

        binding.buttonShare.setOnClickListener {
            val appPackageName = requireContext().packageName
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Share the app")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "https://play.google.com/store/apps/details?$appPackageName"
                )
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }

        binding.buttonRate.setOnClickListener {
            val manager = ReviewManagerFactory.create(requireActivity())
            val request = manager.requestReviewFlow()

            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnCompleteListener {
                    }
                } else {
                    openPlayStore(requireActivity())
                }
            }
        }

        binding.buttonAbout.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigate(R.id.action_navigation_about_to_navigation_about_app)
            requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
        }

        // SUBSCRIPTION DISABLED - Subscription button disabled
        // binding.buttonSubs.setOnClickListener {
        //     Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigate(R.id.action_navigation_about_to_navigation_subscriptin)
        //     requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.GONE
        // }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<BottomNavigationView>(R.id.nav_view).visibility = View.VISIBLE
    }

    fun openPlayStore(context: Context) {
        val appPackageName = context.packageName
        try {
            // Open Play Store
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
            intent.setPackage("com.android.vending") // Specify Play Store as target app
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // If Play Store is unavailable, open browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName"))
            context.startActivity(intent)
        }
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