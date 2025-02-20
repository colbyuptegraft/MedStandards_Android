package com.colbycoapps.med_standarts.ui

import com.google.firebase.storage.StorageReference

object Utils {
    val filesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
    val afFilesMap: MutableMap<String, MutableList<StorageReference>> = mutableMapOf()
}
