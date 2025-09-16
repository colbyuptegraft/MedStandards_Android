package com.colbycoapps.med_standards

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.colbycoapps.med_standards.ui.about.DownloadWorker.Companion.PREFS_NAME
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.colbycoapps.med_standards.databinding.ActivityMainBinding
import com.colbycoapps.med_standards.ui.Utils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MAIN_ACTIVITY", "=== MAIN ACTIVITY STARTED ===")
        
        // Ensure SharedPreferences is initialized
        try {
            // Try to access sharedPreferences to see if it's initialized
            Utils.sharedPreferences.getString("test", null)
            Log.d("MAIN_ACTIVITY", "SharedPreferences already initialized - premium: ${Utils.premium}, countFree: ${Utils.countFree}")
        } catch (e: UninitializedPropertyAccessException) {
            // Not initialized, initialize it now
            Utils.sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // COMMENTED OUT: Subscription status loading disabled
            // Utils.countFree = Utils.sharedPreferences.getInt("countFree", 3)
            // Utils.premium = Utils.sharedPreferences.getBoolean("premium_status", false)
            Utils.countFree = 999 // Unlimited views
            Utils.premium = true // Always premium
            Log.d("MAIN_ACTIVITY", "Initialized SharedPreferences: premium=${Utils.premium}, countFree=${Utils.countFree}")
        }
        
        // COMMENTED OUT: Subscription status refresh disabled
        // Refresh subscription status when MainActivity starts
        // Utils.refreshSubscriptionStatus()
        
        // Force set premium status
        Utils.premium = true
        Utils.countFree = 999
        
        Log.d("MAIN_ACTIVITY", "After refresh - premium: ${Utils.premium}, countFree: ${Utils.countFree}")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Explicitly show status bar and navigation bar
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
        }



        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_airfose, R.id.navigation_army, R.id.navigation_navy, R.id.navigation_dod, R.id.navigation_about
            )
        )
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_airfose -> updateColors(R.color.airForce)
                R.id.navigation_army -> updateColors(R.color.army)
                R.id.navigation_navy -> updateColors(R.color.navy)
                R.id.navigation_dod -> updateColors(R.color.dod)
                R.id.navigation_about -> updateColors(R.color.about)
            }
        }



        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        requestNotificationPermission()

        if (Build.VERSION.SDK_INT <= 34) {
            binding.viewBg.visibility = View.GONE
        }

    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    111
                )
            }
        }
    }

    private fun updateColors(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = color
        binding.viewBg.setBackgroundColor(color)


    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}