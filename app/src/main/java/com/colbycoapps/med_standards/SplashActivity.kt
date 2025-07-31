package com.colbycoapps.med_standards

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
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
    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Utils.sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val downloaded = sharedPreferences.getBoolean(FILES_DOWNLOADED_KEY, false)
        Utils.countFree = sharedPreferences.getInt("countFree", 10)

        if (downloaded) {
            Utils.storage = true
            Log.d("Storage", "true")
        }

        setupBillingClientAndCheckSubscription { hasSubscription ->
            runOnUiThread {
                if (hasSubscription) {
                    Utils.countFree = 10
                    Utils.premium = true
                } else {

                }
            }
        }

        if (isInternetAvailable(this)) {
            if(Utils.filesMap.isEmpty() && Utils.afFilesMap.isEmpty()) {
                val storageRef = FirebaseStorage.getInstance().reference.child("PDFs")
                listFilesWithPagination(storageRef, null)
                listAfFilesWithPagination(storageRef.child("af"), null)
                Log.d("FirebaseStorage", "true")
            }
            else
            {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(2000)
                    navigateToMain()
                }
            }
        }
        else
        {
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                navigateToMain()
            }
        }

    }


    private fun setupBillingClientAndCheckSubscription(onResult: (Boolean) -> Unit) {
        billingClient = BillingClient.newBuilder(this)
            .setListener { _, _ -> }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
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
                            onResult(activeSubscription)
                        } else {
                            onResult(false)
                        }
                    }
                } else {
                    onResult(false)
                }
            }
        })
    }


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
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        //Log.d("FIREBASE", "All files fetched! Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

