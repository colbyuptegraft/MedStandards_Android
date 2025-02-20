package com.colbycoapps.med_standarts

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.colbycoapps.med_standarts.databinding.ActivityMainBinding
import com.colbycoapps.med_standarts.databinding.ActivitySplashBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var allFilesFetched = false // Флаг, що всі файли отримані

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Починаємо завантаження з кореневої папки "PDFs"
        val storageRef = FirebaseStorage.getInstance().reference.child("PDFs")

        // Завантаження загального вмісту
        listFilesWithPagination(storageRef, null)
        // Завантаження окремо для "af" (якщо потрібно зберегти окремо)
        listAfFilesWithPagination(storageRef.child("af"), null)
    }

    private fun listFilesWithPagination(folderRef: StorageReference, pageToken: String?) {
        val listQuery = if (pageToken != null) {
            folderRef.list(1000, pageToken)
        } else {
            folderRef.list(1000)
        }

        listQuery.addOnSuccessListener { listResult: ListResult ->
            // Використовуємо folderRef.name як ключ (наприклад, "af", "army", "dod", "navy")
            val folderName = folderRef.name

            if (!Utils.filesMap.containsKey(folderName)) {
                Utils.filesMap[folderName] = mutableListOf()
            }
            val currentFolderList = Utils.filesMap[folderName]!!

            // Додаємо файли, які безпосередньо знаходяться в цій папці
            listResult.items.forEach { file ->
                Log.d("FIREBASE", "📄 Файл у папці $folderName: ${file.name}")
                currentFolderList.add(file)
            }

            // Рекурсивно обходимо всі підпапки
            listResult.prefixes.forEach { subFolder ->
                Log.d("FIREBASE", "📁 Папка: ${subFolder.name}")
                listFilesWithPagination(subFolder, null)
            }

            // Перевірка пагінації
            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            Log.e("FIREBASE", "❌ Помилка отримання файлів: ${exception.message}")
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

            // Спочатку додаємо підпапки
            listResult.prefixes.forEach { subFolder ->
                Log.d("FIREBASE", "📁 Підпапка (af): ${subFolder.name}")
                currentFolderList.add(subFolder)
                listAfFilesWithPagination(subFolder, null)
            }
            // Потім додаємо файли
            listResult.items.forEach { file ->
                Log.d("FIREBASE", "📄 Файл у папці $folderName (af): ${file.name}")
                currentFolderList.add(file)
            }

            if (listResult.pageToken == null) {
                checkAllFilesFetched()
            } else {
                listAfFilesWithPagination(folderRef, listResult.pageToken)
            }
        }.addOnFailureListener { exception ->
            Log.e("FIREBASE", "❌ Помилка отримання файлів (af): ${exception.message}")
            checkAllFilesFetched()
        }
    }

    private fun checkAllFilesFetched() {
        // Перевіряємо, що хоча б одна з мап не порожня
        if (!allFilesFetched && Utils.filesMap.isNotEmpty() && Utils.afFilesMap.isNotEmpty()) {
            allFilesFetched = true
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        Log.d("FIREBASE", "✅ Всі файли отримані! Переходимо на MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

