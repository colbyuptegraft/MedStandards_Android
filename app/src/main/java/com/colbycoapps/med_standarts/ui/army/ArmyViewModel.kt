package com.colbycoapps.med_standarts.ui.army

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colbycoapps.med_standarts.ui.Utils
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class ArmyViewModel : ViewModel() {

    private val _files = MutableLiveData<List<Pair<String, String>>>()
    val files: LiveData<List<Pair<String, String>>> = _files

    fun loadFiles() {
        val armyFiles = Utils.filesMap["army"] ?: emptyList()

        if (armyFiles.isNotEmpty()) {
            val fileList = mutableListOf<Pair<String, String>>()

            armyFiles.forEach { storageRef ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val fileName = Uri.decode(storageRef.name).replace(".pdf", "")
                    fileList.add(Pair(fileName, uri.toString()))

                    if (fileList.size == armyFiles.size) {
                        // 📌 Сортуємо список за алфавітом
                        fileList.sortBy { it.first.lowercase() }
                        _files.postValue(fileList)
                    }
                }.addOnFailureListener {
                    Log.e("Firebase", "❌ Помилка отримання URL", it)
                }
            }
        } else {
            Log.e("Firebase", "❌ Файли у папці 'army' відсутні!")
        }
    }
}
