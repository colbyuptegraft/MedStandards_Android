package com.colbycoapps.med_standards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
// COMMENTED OUT: Billing imports disabled
/*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
*/
import com.colbycoapps.med_standards.databinding.ActivitySplashBinding
import com.colbycoapps.med_standards.ui.Utils
import com.colbycoapps.med_standards.ui.Utils.sharedPreferences
import com.colbycoapps.med_standards.ui.about.DownloadWorker.Companion.FILES_DOWNLOADED_KEY
import com.colbycoapps.med_standards.ui.about.DownloadWorker.Companion.PREFS_NAME
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var allFilesFetched = false
    private var filesCheckCompleted = false
    private var subscriptionCheckCompleted = false
    // COMMENTED OUT: Billing client variable disabled
    // private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val downloaded = sharedPreferences.getBoolean(FILES_DOWNLOADED_KEY, false)
        // COMMENTED OUT: Subscription status loading disabled
        // Utils.countFree = sharedPreferences.getInt("countFree", 3)
        // Load cached subscription status
        // Utils.premium = sharedPreferences.getBoolean("premium_status", false)
        
        // Force set premium status
        Utils.countFree = 999 // Unlimited views
        Utils.premium = true // Always premium
        
        Log.d("SPLASH_INIT", "=== SPLASH ACTIVITY STARTED ===")
        Log.d("SPLASH_INIT", "Initial countFree: ${Utils.countFree}")
        Log.d("SPLASH_INIT", "Initial premium: ${Utils.premium}")
        Log.d("SPLASH_INIT", "Files downloaded: $downloaded")

        if (downloaded) {
            Utils.storage = true
            Log.d("Storage", "true")
        }

        // COMMENTED OUT: Subscription check disabled
        // Check subscription with timeout and proper error handling
        // checkSubscriptionWithTimeout()
        
        // Skip subscription check - mark as completed immediately
        subscriptionCheckCompleted = true

        if (isInternetAvailable(this)) {
            if(Utils.filesMap.isEmpty() && Utils.afFilesMap.isEmpty()) {
                val storageRef = FirebaseStorage.getInstance().reference.child("PDFs")
                listFilesWithPagination(storageRef, null)
                listAfFilesWithPagination(storageRef.child("af"), null)
                Log.d("FirebaseStorage", "Starting file download")
                // Files check will be handled in checkAllFilesFetched()
            }
            else
            {
                // Files already loaded, mark as completed
                filesCheckCompleted = true
                Log.d("FirebaseStorage", "Files already loaded from cache")
                checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
            }
        }
        else
        {
            // No internet, proceed with cached data
            filesCheckCompleted = true
            Log.d("FirebaseStorage", "No internet, using cached data")
            checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
        }

    }


    // COMMENTED OUT: Subscription check function disabled
    /*
    private fun checkSubscriptionWithTimeout() {
        subscriptionCheckCompleted = false
        if (!isInternetAvailable(this)) {
            filesCheckCompleted = true
            subscriptionCheckCompleted = true  // No internet = can't check, use cached
            Log.d("SUBSCRIPTION", "No internet - using cached subscription status")
            // If no internet, check if we can navigate immediately
            checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
        }
        
        // Timeout protection - proceed after 5 seconds regardless
        CoroutineScope(Dispatchers.Main).launch {
            delay(5000)
            if (!subscriptionCheckCompleted) {
                Log.w("SUBSCRIPTION", "Subscription check timeout, using cached status")
                subscriptionCheckCompleted = true
                checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
            }
        }
        
        // Start subscription check only if we have internet
        if (isInternetAvailable(this)) {
            setupBillingClientAndCheckSubscription { hasSubscription ->
                runOnUiThread {
                    if (hasSubscription) {
                        Utils.countFree = 3
                        Utils.premium = true
                        // Cache the subscription status
                        sharedPreferences.edit()
                            .putBoolean("premium_status", true)
                            .putInt("countFree", 3)
                            .apply()
                        Log.d("SUBSCRIPTION", "Active subscription found")
                    } else {
                        Utils.premium = false
                        // Update cached status
                        sharedPreferences.edit()
                            .putBoolean("premium_status", false)
                            .apply()
                        Log.d("SUBSCRIPTION", "No active subscription")
                    }
                    subscriptionCheckCompleted = true
                    checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
                }
            }
        } else {
            Log.d("SUBSCRIPTION", "No internet - subscription check already marked complete")
        }
    }
    */

    private fun checkIfReadyToNavigate(subscriptionDone: Boolean, filesDone: Boolean) {
        Log.d("NAVIGATION", "Checking navigation readiness: subscription=$subscriptionDone, files=$filesDone")
        if (subscriptionDone && filesDone) {
            Log.d("NAVIGATION", "Both checks completed, navigating to MainActivity")
            CoroutineScope(Dispatchers.Main).launch {
                delay(500) // Small delay for UI stability
                proceedToMainActivity()
            }
        } else {
            Log.d("NAVIGATION", "Still waiting for checks to complete")
        }
    }

    private fun proceedToMainActivity() {
        if (!isFinishing && !isDestroyed) {
            navigateToMain()
        }
    }

    // COMMENTED OUT: Billing check function disabled
    /*
    private fun setupBillingClientAndCheckSubscription(onResult: (Boolean) -> Unit) {
        try {
            billingClient = BillingClient.newBuilder(this)
                .setListener { _, _ -> }
                .enablePendingPurchases()
                .build()

            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingServiceDisconnected() {
                    Log.w("BILLING", "Billing service disconnected")
                    onResult(false)
                }

                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val params = QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()

                        billingClient.queryPurchasesAsync(params) { result, purchasesList ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                // Check if there is at least one active subscription
                                val activeSubscription = purchasesList.any { purchase ->
                                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                            purchase.isAcknowledged
                                }
                                Log.d("BILLING", "Subscription check completed. Active: $activeSubscription")
                                onResult(activeSubscription)
                            } else {
                                Log.w("BILLING", "Query purchases failed: ${result.responseCode}")
                                onResult(false)
                            }
                        }
                    } else {
                        Log.w("BILLING", "Billing setup failed: ${billingResult.responseCode}")
                        onResult(false)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("BILLING", "Error setting up billing client", e)
            onResult(false)
        }
    }
    */


    private fun listFilesWithPagination(folderRef: StorageReference, pageToken: String?) {
        val listQuery = if (pageToken != null) {
            folderRef.list(1000, pageToken)
        } else {
            folderRef.list(1000)
        }

        listQuery.addOnSuccessListener { listResult: ListResult ->
            val folderName = folderRef.name

            if (!Utils.filesMap.containsKey(folderName)) {
                Utils.filesMap[folderName] = mutableListOf()
            }
            val currentFolderList = Utils.filesMap[folderName] ?: run {
                Utils.filesMap[folderName] = mutableListOf()
                Utils.filesMap[folderName]!!
            }

            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "File in folder $folderName: ${file.name}")
                currentFolderList.add(file)
            }

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Folder: ${subFolder.name}")
                listFilesWithPagination(subFolder, null)
            }

            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Error getting files: ${exception.message}")
            checkAllFilesFetched()
        }
    }

    private fun listAfFilesWithPagination(folderRef: StorageReference, pageToken: String?) {
        val listQuery = if (pageToken != null) {
            folderRef.list(1000, pageToken)
        } else {
            folderRef.list(1000)
        }

        listQuery.addOnSuccessListener { listResult: ListResult ->
            // For af we use folderRef.name as key
            val folderName = folderRef.name

            if (!Utils.afFilesMap.containsKey(folderName)) {
                Utils.afFilesMap[folderName] = mutableListOf()
            }
            val currentFolderList = Utils.afFilesMap[folderName] ?: run {
                Utils.afFilesMap[folderName] = mutableListOf()
                Utils.afFilesMap[folderName]!!
            }

            listResult.prefixes.forEach { subFolder ->
                Log.d("FIREBASE", "Subfolder (af): ${subFolder.name}")
                currentFolderList.add(subFolder)
                listAfFilesWithPagination(subFolder, null)
            }
            listResult.items.forEach { file ->
                Log.d("FIREBASE", "File in folder $folderName (af): ${file.name}")
                currentFolderList.add(file)
            }

            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listAfFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Error getting files (af): ${exception.message}")
            checkAllFilesFetched()
        }
    }

    private fun isInternetAvailable(context: Context?): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        Log.d("isInternetAvailable", (activeNetwork != null && activeNetwork.isConnected).toString())
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun checkAllFilesFetched() {
        if (!allFilesFetched && Utils.filesMap.isNotEmpty() && Utils.afFilesMap.isNotEmpty()) {
            allFilesFetched = true
            filesCheckCompleted = true
            Log.d("FirebaseStorage", "All files fetched successfully")
            checkIfReadyToNavigate(subscriptionCheckCompleted, filesCheckCompleted)
        }
    }

    private fun navigateToMain() {
        //Log.d("FIREBASE", "All files fetched! Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

