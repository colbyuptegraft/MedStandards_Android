package com.colbycoapps.med_standards.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.colbycoapps.med_standards.ui.about.DownloadWorker.Companion.PREFS_NAME
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference

object Utils {
    val filesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
    var afFilesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
    var storage = false
    // COMMENTED OUT: Subscription logic disabled
    // var premium = false
    // var countFree = 0
    var premium = true // Always premium status
    var countFree = 999 // Unlimited views
    lateinit var sharedPreferences: SharedPreferences

    private fun listFilesWithPagination(folderRef: StorageReference, pageToken: String?) {
        val listQuery = if (pageToken != null) {
            folderRef.list(1000, pageToken)
        } else {
            folderRef.list(1000)
        }

        listQuery.addOnSuccessListener { listResult: ListResult ->
            val folderName = folderRef.name

            if (!filesMap.containsKey(folderName)) {
                filesMap[folderName] = mutableListOf()
            }
            val currentFolderList = filesMap[folderName] ?: run {
                filesMap[folderName] = mutableListOf()
                filesMap[folderName]!!
            }

            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "File in folder $folderName: ${file.name}")
                currentFolderList.add(file)
            }

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Folder: ${subFolder.name}")
                listFilesWithPagination(subFolder, null)
            }

            if (listResult.pageToken != null) {
                listFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
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

            if (!afFilesMap.containsKey(folderName)) {
                afFilesMap[folderName] = mutableListOf()
            }
            val currentFolderList = afFilesMap[folderName] ?: run {
                afFilesMap[folderName] = mutableListOf()
                afFilesMap[folderName]!!
            }

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Subfolder (af): ${subFolder.name}")
                currentFolderList.add(subFolder)
                listAfFilesWithPagination(subFolder, null)
            }
            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "File in folder $folderName (af): ${file.name}")
                currentFolderList.add(file)
            }

            if (listResult.pageToken != null) {
                listAfFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Error getting files (af): ${exception.message}")
        }
    }

    // COMMENTED OUT: Subscription status refresh function disabled
    // Force refresh subscription status from cache
    fun refreshSubscriptionStatus() {
        /*
        try {
            premium = sharedPreferences.getBoolean("premium_status", false)
            countFree = sharedPreferences.getInt("countFree", 3)
            Log.d("UTILS", "Refreshed subscription status: premium=$premium, countFree=$countFree")
        } catch (e: UninitializedPropertyAccessException) {
            Log.w("UTILS", "SharedPreferences not initialized yet")
        }
        */
        // Force set premium status
        premium = true
        countFree = 999
        Log.d("UTILS", "Subscription logic disabled - always premium")
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }
}
