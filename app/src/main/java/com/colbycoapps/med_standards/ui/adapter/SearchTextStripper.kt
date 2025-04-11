package com.colbycoapps.med_standards.ui.adapter


import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

class SearchTextStripper : PDFTextStripper() {

    data class LineInfo(val text: String, val positions: List<TextPosition>)

    val lines = mutableListOf<LineInfo>()

    init {
        sortByPosition = true
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        lines.add(LineInfo(text, textPositions.toList()))
        super.writeString(text, textPositions)
    }
}
