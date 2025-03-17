package com.colbycoapps.med_standarts.ui.about

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.colbycoapps.med_standarts.SplashActivity
import com.colbycoapps.med_standarts.ui.Utils
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File

class DownloadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 123
        const val PREFS_NAME = "app_prefs"
        const val FILES_DOWNLOADED_KEY = "files_downloaded"
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        val fileList = gatherAllFileReferences()
        var totalFiles = fileList.size
        var downloadedCount = 0

        if (totalFiles == 0) {
            return Result.success()
        }

        setForegroundAsync(createForegroundInfo(0, totalFiles))

        for ((storageRef, folderName) in fileList) {
            val success = downloadFile(storageRef, folderName)
            if (success) {
                downloadedCount++
            } else {
                totalFiles--
            }
            setForegroundAsync(createForegroundInfo(downloadedCount, totalFiles))
        }


        showFinalNotification(context, downloadedCount, totalFiles)

        return Result.success()
    }

    private fun markFilesAsDownloaded(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(FILES_DOWNLOADED_KEY, true)
            apply()
        }
        try {
            Utils.storage = true
        }
        catch (e:Exception)
        {

        }
    }


    @SuppressLint("MissingPermission")
    private fun showFinalNotification(context: Context, progress: Int, max: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(if (max < 10) "Update completed" else "Download completed")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (max < 10) {
            builder.setContentText("Updated $progress з $max files")
        } else {
            builder.setContentText("Downloaded $progress з $max files")
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID+1, builder.build())
        markFilesAsDownloaded(context)
    }


    private fun createForegroundInfo(progress: Int, max: Int): ForegroundInfo {
        val context = applicationContext
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Download PDFs", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading PDFs")
            .setContentText("Downloaded $progress of $max files")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(max, progress, false)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun gatherAllFileReferences(): List<Pair<StorageReference, String>> {
        val result = mutableListOf<Pair<StorageReference, String>>()

        for ((folderName, refs) in Utils.filesMap) {
            if (folderName in listOf("army", "dod", "navy")) {
                for (ref in refs) {
                    if (!Utils.filesMap.containsKey(ref.name) && !Utils.afFilesMap.containsKey(ref.name)) {
                        result.add(Pair(ref, folderName))
                    }
                }
            }
        }

        for ((folderName, refs) in Utils.afFilesMap) {
            for (ref in refs) {
                if (!Utils.afFilesMap.containsKey(ref.name) && !Utils.filesMap.containsKey(ref.name)) {
                    result.add(Pair(ref, "af/$folderName"))
                }
            }
        }
        return result
    }

    private suspend fun downloadFile(storageRef: StorageReference, folderName: String): Boolean {
        return try {
            val rootDir = applicationContext.getExternalFilesDir("pdfs")
            val localDir = File(rootDir, folderName)
            localDir.mkdirs()

            val localFile = File(localDir, storageRef.name)
            if (localFile.exists()) return false

            storageRef.getFile(localFile).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}


