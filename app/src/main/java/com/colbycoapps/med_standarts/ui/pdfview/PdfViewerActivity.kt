package com.colbycoapps.med_standarts.ui.pdfview

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.colbycoapps.med_standarts.R
import com.colbycoapps.med_standarts.databinding.ActivityPdfViewerBinding
import com.colbycoapps.med_standarts.ui.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.common.PDStream
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


data class QuadInfo(
    val pageIndex: Int,
    val quads: FloatArray,
    val rectX: Float,
    val rectY: Float,
    val rectW: Float,
    val rectH: Float
)

data class SearchResult(
    val pageIndex: Int,
    val snippet: String,   // 3 слова перед + слово + 3 слова після
    val matchText: String  // Саме слово, яке знайшли
)

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFile: File? = null
    private lateinit var progressDialog: ProgressDialog

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var searchResults = mutableListOf<SearchResult>() // Список знайдених результатів

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PDFBoxResourceLoader.init(applicationContext)
        setSupportActionBar(binding.toolbar2)
        binding.toolbar2.navigationIcon?.setTint(Color.WHITE)
        binding.toolbar2.setTitleTextColor(Color.WHITE)
        binding.toolbar2.setSubtitleTextColor(Color.WHITE)

        val pdfUrl = intent.getStringExtra("PDF_URL") ?: run {
            Toast.makeText(this, "PDF URL not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val pdfName = intent.getStringExtra("PDF_NAME")
        val COLOR = intent.getStringExtra("COLOR")
        when(COLOR)
        {
            "airForce" -> {
                updateColors(R.color.airForce)
                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment
                        ?.replace("PDFs/af/AFIs/", "")
                        ?.replace("PDFs/af/RSVs/", "")
                        ?.replace("PDFs/af/bomc/", "")
                        ?.replace("PDFs/af/fsToolkit/", "")
                        ?.replace("PDFs/af/main/", "")!!.split("#")[0] ?: "PDF Viewer"
                }
            }
            "army" -> {
                updateColors(R.color.army)
                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/army/", "")!!.split("#")[0] ?: "PDF Viewer"
                }

            }
            "navy" -> {
                updateColors(R.color.navy)

                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/navy/", "")!!.split("#")[0] ?: "PDF Viewer"
                }
            }
            "dod" -> {
                updateColors(R.color.dod)
                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/dod/", "")!!.split("#")[0] ?: "PDF Viewer"
                }
            }
            "about" -> {
                updateColors(R.color.about)
            }
        }



        progressDialog = ProgressDialog(this).apply {
            setCancelable(false)
            setMessage("Loading PDF...")
            show()
        }

        setupWebView()

        CoroutineScope(Dispatchers.IO).launch {
            if(!Utils.storage) {
                pdfFile = if (pdfUrl.startsWith("http", ignoreCase = true)) {
                    File(downloadPdfFile(pdfUrl).path!!)
                } else {
                    File(Uri.parse(pdfUrl).path!!)
                }
                withContext(Dispatchers.Main) {
                    loadPdfFromUri(pdfFile!!.toUri())
                }
            }
            else
            {
                pdfFile = getFileFromUri(this@PdfViewerActivity, Uri.parse(pdfUrl))
                withContext(Dispatchers.Main) {
                    loadPdfFromUri(pdfFile!!.toUri())
                }
            }
        }

        setSupportActionBar(binding.toolbar2)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar2.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return if (uri.scheme.equals("file", ignoreCase = true)) {
            File(uri.path ?: "")
        } else if (uri.scheme.equals("content", ignoreCase = true)) {
            // Створюємо тимчасовий файл у cache директорії
            val fileName = "temp_pdf_file.pdf" // Можна розширити логіку для отримання оригінальної назви
            val tempFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } else {
            null
        }
    }

    private fun updateColors(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color)) // Змінюємо AppBar
        window.statusBarColor = color // Змінюємо статусбар
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true) // Дозволити масштабування
            builtInZoomControls = true // Додати контролери масштабування
            displayZoomControls = false // Приховати кнопки "+" і "-"
            useWideViewPort = true // **Головне: Масштабує контент до ширини екрану**
            loadWithOverviewMode = true // **Головне: Підганяє весь контент у WebView**
        }

        binding.webView.webViewClient = WebViewClient()
        binding.webView.webChromeClient = WebChromeClient()
    }


    private fun highlightWordsInPdf(query: String) {
        progressDialog.setMessage("Searching for \"$query\"...")
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            pdfFile?.let { file ->
                // 🔥 1. Перенумерація сторінок
                val newPdfFile = renumberAndSavePdf(file)
                val document = PDDocument.load(newPdfFile)

                // 🔥 2. Пошук за оновленою нумерацією
                val highlightMap: Map<Int, List<QuadInfo>> = findTextPositions(document, query)
                addHighlightAnnotations(document, highlightMap)

                // 🔥 3. Збереження нового PDF із анотаціями
                val finalFile = File(cacheDir, "highlighted.pdf")
                document.use { it.save(finalFile) }
                document.close()

                searchResults = extractTextWithContext(finalFile, query)

                withContext(Dispatchers.Main) {
                    pdfFile = finalFile
                    openRenderer(finalFile)
                    loadPdfFromUri(pdfFile!!.toUri())

                    progressDialog.dismiss()
                    showResultsBottomSheet()
                }
            }
        }
    }


    private fun extractTextWithContext(pdfFile: File, query: String): MutableList<SearchResult> {
        val results = mutableListOf<SearchResult>()
        PDDocument.load(pdfFile).use { document ->
            for (pageIndex in 0 until document.numberOfPages) {
                val pageText = extractPageText(document, pageIndex)
                val words = pageText.split(Regex("\\s+")).filter { it.isNotBlank() }

                for (i in words.indices) {
                    if (words[i].equals(query, ignoreCase = true)) {
                        val start = maxOf(0, i - 3)
                        val end = minOf(words.size, i + 4)
                        val snippet = words.subList(start, end).joinToString(" ")
                        results.add(SearchResult(pageIndex, snippet, words[i]))
                    }
                }
            }
        }
        return results
    }

    private fun loadPdfFromUri(uri: Uri) {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val bytes = stream.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP) // ✅ Уникнення \n

            Log.d("BASE64", "Base64 довжина: ${base64String.length}")
            Log.d("BASE64", "Перші 100 символів: ${base64String.take(100)}")

            binding.webView.loadUrl("about:blank")
            // Завантажуємо HTML
            binding.webView.loadUrl("file:///android_asset/index.html")

            // Додаємо невеликий delay перед передачею Base64
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000) // Затримка на 0.5 секунди
                binding.webView.evaluateJavascript("receivePDF('$base64String')", null)
                progressDialog.dismiss()
            }
        }

    }

    private fun showResultsBottomSheet() {
        if(searchResults.isEmpty())
        {
            Toast.makeText(this, "Could not find the word", Toast.LENGTH_SHORT).show()
            return
        }
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_search_results, null)
        bottomSheetDialog.setContentView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSearchResults)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = SearchResultsAdapter(searchResults) { clickedItem ->
            bottomSheetDialog.dismiss()
            scrollToWord(clickedItem.pageIndex)
        }
        recyclerView.adapter = adapter
        bottomSheetDialog.show()
    }

    private fun scrollToWord(pageIndex: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            binding.webView.evaluateJavascript("scrollToPage($pageIndex)", null)
        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val resultsItem = menu?.findItem(R.id.action_results)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        resultsItem?.setOnMenuItemClickListener {
            showResultsBottomSheet()
            true
        }

        searchView?.setOnCloseListener {
            searchItem.collapseActionView() // Закриває поле пошуку
            true
        }

        searchView?.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotEmpty()) {
                        highlightWordsInPdf(it)
                        resultsItem?.isVisible = true
                    }
                }
                searchView.clearFocus()
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean = false
        })
        return true
    }



    private fun openRenderer(file: File) {
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
    }


    private fun extractPageText(document: PDDocument, pageIndex: Int): String {
        val stripper = PDFTextStripper().apply {
            startPage = pageIndex + 1
            endPage = pageIndex + 1
        }
        return stripper.getText(document).trim() // Видаляємо зайві пробіли
    }


    private fun findTextPositions(document: PDDocument, query: String): Map<Int, List<QuadInfo>> {
        val result = mutableMapOf<Int, MutableList<QuadInfo>>()

        for (pageIndex in 0 until document.numberOfPages) {
            val page = document.getPage(pageIndex)
            val textPositions = mutableListOf<TextPosition>()

            val stripper = object : PDFTextStripper() {
                override fun processTextPosition(text: TextPosition) {
                    textPositions.add(text)
                }
            }
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(document)

            for (i in 0 until textPositions.size - query.length + 1) {
                val subList = textPositions.subList(i, i + query.length)
                val subText = subList.joinToString("") { it.unicode }

                if (subText.equals(query, ignoreCase = true)) {
                    val first = subList.first()
                    val last = subList.last()
                    val mediaBox = page.mediaBox
                    val x = first.xDirAdj
                    val y = mediaBox.height - first.yDirAdj
                    val w = (last.xDirAdj + last.widthDirAdj) - first.xDirAdj
                    val h = first.heightDir
                    val quads = floatArrayOf(x, y, x, y - h, x + w, y, x + w, y - h)

                    val info = QuadInfo(pageIndex, quads, x, y - h, w, h)
                    result.getOrPut(pageIndex) { mutableListOf() }.add(info)
                }
            }
        }

        return result.mapValues { it.value.toList() }.toMutableMap()
    }






    private fun addHighlightAnnotations(document: PDDocument, highlightMap: Map<Int, List<QuadInfo>>) {
        val highlightColor = PDColor(floatArrayOf(1f, 1f, 0f), PDDeviceRGB.INSTANCE)
        highlightMap.forEach { (pageIndex, quadInfos) ->
            val page = document.getPage(pageIndex)
            quadInfos.forEach { quad ->
                val annotation = PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT).apply {
                    color = highlightColor
                    rectangle = PDRectangle(quad.rectX, quad.rectY, quad.rectW, quad.rectH)
                    setQuadPoints(quad.quads)
                }
                page.annotations.add(annotation)
                Log.d("HIGHLIGHT", "Added annotation on page $pageIndex: rect=(${quad.rectX}, ${quad.rectY}, ${quad.rectW}, ${quad.rectH})")
            }
        }
    }

    private fun renumberAndSavePdf(originalFile: File): File {
        val document = PDDocument.load(originalFile)
        val newDocument = PDDocument()

        val pageNumberMap = mutableMapOf<Int, Int>()

        for ((newIndex, oldIndex) in document.pages.count().let { (0 until it).withIndex() }) {
            val oldPage = document.getPage(oldIndex)
            newDocument.addPage(oldPage)
            pageNumberMap[oldIndex] = newIndex
        }

        val renumberedFile = File(cacheDir, "renumbered.pdf")
        newDocument.use { it.save(renumberedFile) }
        document.close()

        Log.d("RENAMING", "Saved renumbered PDF as: ${renumberedFile.absolutePath}")
        return renumberedFile
    }

    

    // Завантаження PDF-файлу з URL (приклад)
    private fun downloadPdfFile(urlStr: String): Uri {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
        }
        val file = File(cacheDir, "temp.pdf")
        connection.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        connection.disconnect()
        return Uri.fromFile(file)
    }



}
