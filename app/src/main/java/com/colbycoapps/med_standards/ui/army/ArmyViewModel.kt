package com.colbycoapps.med_standards.ui.army

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colbycoapps.med_standards.ui.Utils

class ArmyViewModel : ViewModel() {

    private val _files = MutableLiveData<List<Pair<String, String>>>()
    val files: LiveData<List<Pair<String, String>>> = _files

    private val _filesStorage = MutableLiveData<List<Pair<String, String>>>()
    val filesStorage: LiveData<List<Pair<String, String>>> = _filesStorage

    fun loadFiles() {
        val armyFiles = Utils.filesMap["army"] ?: emptyList()

        if (armyFiles.isNotEmpty()) {
            val fileList = mutableListOf<Pair<String, String>>()

            armyFiles.forEach { storageRef ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val fileName = Uri.decode(storageRef.name).replace(".pdf", "")
                    fileList.add(Pair(fileName, uri.toString()))

                    if (fileList.size == armyFiles.size) {
                        fileList.sortBy { it.first.lowercase() }
                        _files.postValue(fileList)
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "Recieved an error URL", it)
                }
            }
        } else {
            Log.e("Firebase", "There are no files in folder 'army'!")
        }
    }

    fun loadFilesStorage(context: Context) {
        val result = mutableListOf<Pair<String, String>>()
        val rootDir = context.getExternalFilesDir("pdfs/army")

        if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
            val files = rootDir.listFiles()?.filter {
                it.isFile && it.extension.equals("pdf", ignoreCase = true)
            } ?: emptyList()

            files.forEach { file ->
                            val fileName = file.nameWithoutExtension  // name without .pdf
            val fileUri = Uri.fromFile(file)          // Uri for opening
                result.add(fileName to fileUri.toString())
            }
        }
        _filesStorage.value = result

    }
}
