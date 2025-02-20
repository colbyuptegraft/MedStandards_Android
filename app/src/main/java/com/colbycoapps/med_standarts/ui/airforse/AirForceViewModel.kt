package com.colbycoapps.med_standarts.ui.airforse

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colbycoapps.med_standarts.ui.Utils

class AirForceViewModel : ViewModel() {


    private val _filesAndFolders = MutableLiveData<List<Pair<String, String>>>()
    val filesAndFolders: LiveData<List<Pair<String, String>>> = _filesAndFolders

    private var currentPath = "af" // Ключ у `afFilesMap` (наприклад, "af", "af/AFIs", тощо)

    /**
     * Завантажити вміст для path (наприклад, "af" або "main" чи "af/RSVs").
     */
    fun loadFiles(path: String = "af") {
        currentPath = path
        //        val storageRefs = if(path == "af")
//            Utils.afFilesMap[currentPath] else Utils.afFilesMap[currentPath] ?: emptyList()
        val storageRefs = Utils.afFilesMap[path] ?: emptyList()

        if (storageRefs.isEmpty()) {
            Log.e("Firebase", "❌ Папка '$path' порожня!")
            _filesAndFolders.postValue(emptyList())
            return
        }

        // Список назв папок (folderName, "folder")
        val folderList = mutableListOf<String>()
        // Список файлів (fileName, fileUrl)
        val fileList = mutableListOf<Pair<String, String>>()

        // 1. Розділяємо на папки / файли
        storageRefs.forEach { ref ->

//            var folder = false
//            Utils.afFilesMap[path]?.forEach {
//                if(it.name.contains(currentPath))
//                    folder = true
//            }

            if (path == "af") {
                folderList.add(ref.name)
            } else {
                // Інакше вважаємо, що це файл
                fileList.add(Pair(ref.name, "pending_url"))
            }
        }
        // 2. Завантажуємо URL для файлів
        val totalFiles = fileList.size
        var completed = 0

        fileList.forEachIndexed { i, (rawName, _) ->
            // Знаходимо сам `StorageReference` серед `storageRefs`
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

        // Якщо файлів 0, просто постимо папки
        if (totalFiles == 0) {
            postResult(folderList, fileList)
        }
    }

    /**
     * Відсортовуємо папки та файли і оновлюємо LiveData
     */
    private fun postResult(folderList: List<String>, fileList: List<Pair<String, String>>) {
        val tempList = mutableListOf<Pair<String, String>>()

        // Спочатку папки (folderName, "folder")
        folderList.sortedBy { it.lowercase() }.forEach { folderName ->
            tempList.add(Pair(folderName, "folder"))
        }
        // Потім файли (fileName, url)
        fileList.sortedBy { it.first.lowercase() }.forEach { (fName, fUrl) ->
            tempList.add(Pair(fName, fUrl))
        }

        _filesAndFolders.postValue(tempList)
    }

    fun navigateBack(): Boolean {
        // Якщо вже в корені "af", то далі не піднімаємось
        if (currentPath == "af") return false

        val parentPath = currentPath.substringBeforeLast("/", "af")
        loadFiles(parentPath)
        return true
    }

    fun getCurrentPath(): String = currentPath
}

