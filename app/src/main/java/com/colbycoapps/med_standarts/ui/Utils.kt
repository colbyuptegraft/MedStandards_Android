package com.colbycoapps.med_standarts.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference

object Utils {
    val filesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
    var afFilesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
    var storage = false
    var premium = false


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
            val currentFolderList = filesMap[folderName]!!

            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "Файл у папці $folderName: ${file.name}")
                currentFolderList.add(file)
            }

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Папка: ${subFolder.name}")
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
            // Для af використовуємо folderRef.name як ключ
            val folderName = folderRef.name

            if (!afFilesMap.containsKey(folderName)) {
                afFilesMap[folderName] = mutableListOf()
            }
            val currentFolderList = afFilesMap[folderName]!!

            listResult.prefixes.forEach { subFolder ->
                //Log.d("FIREBASE", "Підпапка (af): ${subFolder.name}")
                currentFolderList.add(subFolder)
                listAfFilesWithPagination(subFolder, null)
            }
            listResult.items.forEach { file ->
                //Log.d("FIREBASE", "Файл у папці $folderName (af): ${file.name}")
                currentFolderList.add(file)
            }

            if (listResult.pageToken != null) {
                listAfFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            //Log.e("FIREBASE", "Помилка отримання файлів (af): ${exception.message}")
        }
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
