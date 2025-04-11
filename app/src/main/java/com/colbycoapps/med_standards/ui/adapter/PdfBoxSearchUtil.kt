package com.colbycoapps.med_standards.ui.adapter

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object PdfBoxSearchUtil {


    fun searchInLine(line: SearchTextStripper.LineInfo, query: String): List<RectF> {
        val results = mutableListOf<RectF>()
        var startIndex = 0
        while (true) {
            val index = line.text.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) break
            val subPositions = line.positions.subList(index, index + query.length)
            var left = Float.MAX_VALUE
            var bottom = Float.MAX_VALUE
            var right = -Float.MAX_VALUE
            var top = -Float.MAX_VALUE

            for (tp in subPositions) {
                val x = tp.xDirAdj
                val y = tp.yDirAdj
                val w = tp.widthDirAdj
                val h = tp.heightDir
                left = minOf(left, x)
                bottom = minOf(bottom, y)
                right = maxOf(right, x + w)
                top = maxOf(top, y + h)
            }
            results.add(RectF(left, bottom, right, top))
            startIndex = index + query.length
        }
        return results
    }

    suspend fun extractSearchResults(context: Context, pdfUri: Uri, query: String): Map<Int, List<RectF>> {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext emptyMap()
            val document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages
            val resultMap = mutableMapOf<Int, MutableList<RectF>>()

            for (page in 1..totalPages) {
                val stripper = SearchTextStripper().apply {
                    startPage = page
                    endPage = page
                }

                val a = stripper.getText(document)

                for (line in stripper.lines) {
                    val boxes = searchInLine(line, query)
                    if (boxes.isNotEmpty()) {

                        val pageIndex = page - 1
                        resultMap.getOrPut(pageIndex) { mutableListOf() }.addAll(boxes)
                    }
                }
            }
            document.close()
            inputStream.close()
            resultMap
        }
    }
}