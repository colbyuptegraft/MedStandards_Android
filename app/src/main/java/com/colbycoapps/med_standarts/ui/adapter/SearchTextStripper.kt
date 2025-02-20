package com.colbycoapps.med_standarts.ui.adapter


import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

class SearchTextStripper : PDFTextStripper() {

    data class LineInfo(val text: String, val positions: List<TextPosition>)

    // Збираємо кожен рядок разом із позиціями символів
    val lines = mutableListOf<LineInfo>()

    init {
        // Якщо потрібно – увімкніть сортування позицій
        sortByPosition = true
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        lines.add(LineInfo(text, textPositions.toList()))
        super.writeString(text, textPositions)
    }
}
