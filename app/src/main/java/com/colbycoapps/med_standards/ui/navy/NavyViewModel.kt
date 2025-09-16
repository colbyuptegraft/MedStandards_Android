package com.colbycoapps.med_standards.ui.navy

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colbycoapps.med_standards.ui.Utils

class NavyViewModel : ViewModel() {

    private val _files = MutableLiveData<List<Pair<String, String>>>()
    val files: LiveData<List<Pair<String, String>>> = _files

    private val _filesStorage = MutableLiveData<List<Pair<String, String>>>()
    val filesStorage: LiveData<List<Pair<String, String>>> = _filesStorage
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Cache for download URLs to avoid repeated Firebase calls
    private val urlCache = mutableMapOf<String, String>()

    fun loadFiles() {
        val navyFiles = Utils.filesMap["navy"] ?: emptyList()

        if (navyFiles.isNotEmpty()) {
            _isLoading.value = true
            val fileList = mutableListOf<Pair<String, String>>()
            var completedCount = 0

            navyFiles.forEach { storageRef ->
                val storagePath = storageRef.path
                
                // Check cache first
                if (urlCache.containsKey(storagePath)) {
                    val fileName = Uri.decode(storageRef.name).replace(".pdf", "")
                    fileList.add(Pair(fileName, urlCache[storagePath]!!))
                    
                    completedCount++
                    if (completedCount == navyFiles.size) {
                        fileList.sortBy { it.first.lowercase() }
                        _files.postValue(fileList)
                        _isLoading.value = false
                    }
                } else {
                    // Load from Firebase and cache
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val fileName = Uri.decode(storageRef.name).replace(".pdf", "")
                        val urlString = uri.toString()
                        
                        // Cache the URL
                        urlCache[storagePath] = urlString
                        fileList.add(Pair(fileName, urlString))

                        completedCount++
                        if (completedCount == navyFiles.size) {
                            fileList.sortBy { it.first.lowercase() }
                            _files.postValue(fileList)
                            _isLoading.value = false
                        }
                    }.addOnFailureListener {
                        Log.e("Firebase", "Error getting URL for navy files", it)
                        completedCount++
                        if (completedCount == navyFiles.size) {
                            _isLoading.value = false
                        }
                    }
                }
            }
        } else {
            Log.e("Firebase", "Navy files folder is empty!")
            _isLoading.value = false
        }
    }

    fun loadFilesStorage(context: Context) {
        val result = mutableListOf<Pair<String, String>>()
        val rootDir = context.getExternalFilesDir("pdfs/navy")

        if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
            // Read all .pdf files
            val files = rootDir.listFiles()?.filter {
                it.isFile && it.extension.equals("pdf", ignoreCase = true)
            } ?: emptyList()

            files.forEach { file ->
                val fileName = file.nameWithoutExtension
                val fileUri = Uri.fromFile(file)
                result.add(fileName to fileUri.toString())
            }
        }
        _filesStorage.value = result

    }
}