package com.colbycoapps.med_standards.ui.airforse

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colbycoapps.med_standards.ui.Utils
import java.io.File

class AirForceViewModel : ViewModel() {


    private val _filesAndFolders = MutableLiveData<List<Pair<String, String>>>()
    val filesAndFolders: LiveData<List<Pair<String, String>>> = _filesAndFolders

    private val _filesAndFoldersStorage = MutableLiveData<List<Pair<String, String>>>()
    val filesAndFoldersStorage: LiveData<List<Pair<String, String>>> = _filesAndFoldersStorage

    private var currentPath = "af"

    fun loadFiles(path: String = "af") {
        currentPath = path
        val storageRefs = Utils.afFilesMap[path] ?: emptyList()

        if (storageRefs.isEmpty()) {
            Log.e("Firebase", "❌ Папка '$path' порожня!")
            _filesAndFolders.postValue(emptyList())
            return
        }

        val folderList = mutableListOf<String>()
        val fileList = mutableListOf<Pair<String, String>>()

        storageRefs.forEach { ref ->

            if (path == "af") {
                folderList.add(ref.name)
            } else {
                // Інакше вважаємо, що це файл
                fileList.add(Pair(ref.name, "pending_url"))
            }
        }
        val totalFiles = fileList.size
        var completed = 0

        fileList.forEachIndexed { i, (rawName, _) ->
            val fileRef = storageRefs.find { it.name == rawName }
            fileRef?.downloadUrl?.addOnSuccessListener { uri ->
                // Видаляємо ".pdf" для гарної назви
                val displayName = rawName.replace(".pdf", "")
                fileList[i] = Pair(displayName, uri.toString())

                completed++
                if (completed == totalFiles) {
                    postResult(folderList, fileList)
                }
            }?.addOnFailureListener {
                completed++
                if (completed == totalFiles) {
                    postResult(folderList, fileList)
                }
            }
        }

        if (totalFiles == 0) {
            postResult(folderList, fileList)
        }
    }

    private fun postResult(folderList: List<String>, fileList: List<Pair<String, String>>) {
        val tempList = mutableListOf<Pair<String, String>>()

        folderList.sortedBy { it.lowercase() }.forEach { folderName ->
            tempList.add(Pair(folderName, "folder"))
        }
        fileList.sortedBy { it.first.lowercase() }.forEach { (fName, fUrl) ->
            tempList.add(Pair(fName, fUrl))
        }

        _filesAndFolders.postValue(tempList)
    }

    fun navigateBack(): Boolean {
        if (currentPath == "af") return false

        val parentPath = currentPath.substringBeforeLast("/", "af")
        loadFiles(parentPath)
        return true
    }

    fun loadFolderStorage(context: Context)
    {
        val result = mutableListOf<Pair<String, String>>()
        val rootDir = context.getExternalFilesDir("pdfs/af")
        val folders: List<File> = if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
            rootDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        } else {
            emptyList()
        }

        folders.forEach { folder ->
            Log.e("folder", folder.name)
            result.add(Pair(folder.name, "folder"))
        }

        _filesAndFoldersStorage.value = result

    }

    fun loadFilesFolderStorage(context: Context, folder: String) {
        val result = mutableListOf<Pair<String, String>>()

        val rootDir = context.getExternalFilesDir("pdfs/af/${folder}")

        if (rootDir != null && rootDir.exists() && rootDir.isDirectory) {
            val files = rootDir.listFiles()?.filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) } ?: emptyList()

            files.forEach { file ->
                val fileName = file.nameWithoutExtension
                val fileUri = Uri.fromFile(file)
                result.add(fileName to fileUri.toString())
            }
        }


        _filesAndFoldersStorage.value = result

    }

    fun getCurrentPath(): String = currentPath
}

