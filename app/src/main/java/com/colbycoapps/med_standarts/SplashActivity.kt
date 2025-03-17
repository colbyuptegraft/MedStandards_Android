package com.colbycoapps.med_standarts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.colbycoapps.med_standarts.databinding.ActivityMainBinding
import com.colbycoapps.med_standarts.databinding.ActivitySplashBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.colbycoapps.med_standarts.ui.about.DownloadWorker.Companion.FILES_DOWNLOADED_KEY
import com.colbycoapps.med_standarts.ui.about.DownloadWorker.Companion.PREFS_NAME
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val downloaded = sharedPreferences.getBoolean(FILES_DOWNLOADED_KEY, false)

        if (downloaded) {
            Utils.storage = true
            Log.d("Storage", "true")
        }

//        setupBillingClientAndCheckSubscription { hasSubscription ->
//            runOnUiThread {
//                if (hasSubscription) {
//                    // Підписка дійсна – продовжуємо
//                } else {
//                    // Немає активної підписки – наприклад, переходимо на екран з підпискою
//                }
//            }
//        }

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
                // Спробуйте повторно підключитися або повернути false
                onResult(false)
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val params = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()

                    billingClient.queryPurchasesAsync(params) { result, purchasesList ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Перевіряємо, чи є хоч одна активна підписка
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
            val currentFolderList = Utils.filesMap[folderName]!!

            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "Файл у папці $folderName: ${file.name}")
                currentFolderList.add(file)
            }

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Папка: ${subFolder.name}")
                listFilesWithPagination(subFolder, null)
            }

            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Помилка отримання файлів: ${exception.message}")
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
            // Для af використовуємо folderRef.name як ключ
            val folderName = folderRef.name

            if (!Utils.afFilesMap.containsKey(folderName)) {
                Utils.afFilesMap[folderName] = mutableListOf()
            }
            val currentFolderList = Utils.afFilesMap[folderName]!!

            listResult.prefixes.forEach { subFolder ->
                Log.d("FIREBASE", "Підпапка (af): ${subFolder.name}")
                currentFolderList.add(subFolder)
                listAfFilesWithPagination(subFolder, null)
            }
            listResult.items.forEach { file ->
                Log.d("FIREBASE", "Файл у папці $folderName (af): ${file.name}")
                currentFolderList.add(file)
            }

            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listAfFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Помилка отримання файлів (af): ${exception.message}")
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
        //Log.d("FIREBASE", "Всі файли отримані! Переходимо на MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

