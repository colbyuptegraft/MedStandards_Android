package com.colbycoapps.med_standarts

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.colbycoapps.med_standarts.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_airfose, R.id.navigation_army, R.id.navigation_navy, R.id.navigation_dod, R.id.navigation_about
            )
        )
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
    }


    private fun updateColors(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color)) // Змінюємо AppBar
        window.statusBarColor = color // Змінюємо статусбар
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}