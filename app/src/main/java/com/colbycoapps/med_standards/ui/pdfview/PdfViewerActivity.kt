package com.colbycoapps.med_standards.ui.pdfview

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.colbycoapps.med_standards.R
import com.colbycoapps.med_standards.databinding.ActivityPdfViewerBinding
import com.colbycoapps.med_standards.ui.Utils
import com.colbycoapps.med_standards.ui.Utils.sharedPreferences
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
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
    val snippet: String,   // 3 words before + word + 3 words after
    val matchText: String  // The actual word that was found
)

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding
    private var pdfFile: File? = null
    private lateinit var progressDialog: ProgressDialog

    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var searchResults = mutableListOf<SearchResult>() // List of found search results

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("PDF_VIEWER", "=== PDF VIEWER STARTED ===")
        Log.d("PDF_VIEWER", "On entry - premium: ${Utils.premium}, countFree: ${Utils.countFree}")

        // COMMENTED OUT: View counting logic disabled
        /*
        // Double-check subscription status and decrease counter safely
        if(!Utils.premium && Utils.countFree > 0)
        {
            Utils.countFree -= 1
            Utils.sharedPreferences.edit().putInt("countFree", Utils.countFree).apply()
            Log.d("PDF_VIEWER", "Decreased countFree to: ${Utils.countFree}")
        } else if (!Utils.premium && Utils.countFree <= 0) {
            // This should not happen if fragments check properly, but safety measure
            Log.w("PDF_VIEWER", "Attempted to open PDF with no free views remaining")
            Toast.makeText(this, "No free views remaining. Please subscribe.", Toast.LENGTH_LONG).show()
            finish()
            return
        } else {
            Log.d("PDF_VIEWER", "Premium user - no counter decrease")
        }
        */
        
        // Subscription logic disabled - always allow PDF access
        Log.d("PDF_VIEWER", "Subscription logic disabled - PDF access granted")

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
                        ?.replace("PDFs/af/main/", "")?.split("#")?.get(0) ?: "PDF Viewer"
                }
            }
            "army" -> {
                updateColors(R.color.army)
                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/army/", "")?.split("#")?.get(0) ?: "PDF Viewer"
                }

            }
            "navy" -> {
                updateColors(R.color.navy)

                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/navy/", "")?.split("#")?.get(0) ?: "PDF Viewer"
                }
            }
            "dod" -> {
                updateColors(R.color.dod)
                if(pdfName != null)
                {
                    supportActionBar?.title = pdfName.split("#")[0]
                }
                else {
                    supportActionBar?.title = Uri.parse(pdfUrl).lastPathSegment?.replace("PDFs/dod/", "")?.split("#")?.get(0) ?: "PDF Viewer"
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
                    downloadPdfFile(pdfUrl)?.let { uri ->
                        File(uri.path ?: throw Exception("Failed to get file path"))
                    } ?: throw Exception("File download error")
                } else {
                    val path = Uri.parse(pdfUrl).path
                    if (path != null) File(path) else throw Exception("Invalid file path")
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

        if (Build.VERSION.SDK_INT <= 34) {
            binding.viewBg.visibility = View.GONE
        }
    }

    fun getFileFromUri(context: Context, uri: Uri): File? {
        return if (uri.scheme.equals("file", ignoreCase = true)) {
            File(uri.path ?: "")
        } else if (uri.scheme.equals("content", ignoreCase = true)) {
            val fileName = "temp_pdf_file.pdf"
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
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))
        window.statusBarColor = color
        binding.viewBg.setBackgroundColor(color)
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
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE // Disable cache for PDF
            domStorageEnabled = true
            databaseEnabled = true
            // setAppCacheEnabled removed in newer APIs, using alternative settings
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                setAppCacheEnabled(false)
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("WebView", "Page finished loading: $url")
            }
            
            override fun onReceivedError(
                view: android.webkit.WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e("WebView", "Error loading page: ${error?.description}")
            }
        }
        
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                Log.d("WebView", "Loading progress: $newProgress%")
            }
            
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                Log.d("WebView-Console", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                return true
            }
        }
    }

    private fun setAppCacheEnabled(b: Boolean) {

    }


    private fun highlightWordsInPdf(query: String) {
        progressDialog.setMessage("Searching for \"$query\"...")
        progressDialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            pdfFile?.let { file ->
                val newPdfFile = renumberAndSavePdf(file)
                val document = PDDocument.load(newPdfFile)

                val highlightMap: Map<Int, List<QuadInfo>> = findTextPositions(document, query)
                addHighlightAnnotations(document, highlightMap)

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
        Log.d("PDF_LOAD", "Starting PDF load from URI: $uri")
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val bytes = stream.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)

            Log.d("PDF_LOAD", "PDF file size: ${bytes.size} bytes")
            Log.d("PDF_LOAD", "Base64 length: ${base64String.length}")
            Log.d("PDF_LOAD", "First 100 characters: ${base64String.take(100)}")

            binding.webView.loadUrl("about:blank")
            binding.webView.loadUrl("file:///android_asset/index.html")

            CoroutineScope(Dispatchers.Main).launch {
                binding.webView.post {
                    // Check WebView readiness
                    if (binding.webView.progress == 100) {
                        binding.webView.evaluateJavascript("receivePDF('$base64String')", null)
                        // Dismiss progress dialog immediately after sending PDF to WebView
                        progressDialog.dismiss()
                    } else {
                        // If WebView is not ready yet, wait a bit and retry
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500)
                            binding.webView.evaluateJavascript("receivePDF('$base64String')", null)
                            progressDialog.dismiss()
                        }
                    }
                }
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
            // Remove fixed delay, use rendering readiness check
            binding.webView.evaluateJavascript("(function() { return renderingInProgress; })()", { result ->
                if (result == "false") {
                    // Rendering completed, can scroll
                    binding.webView.evaluateJavascript("scrollToPage($pageIndex)", null)
                } else {
                    // Rendering still in progress, wait and retry
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(200)
                        binding.webView.evaluateJavascript("scrollToPage($pageIndex)", null)
                    }
                }
            })
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
            searchItem.collapseActionView()
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
        return stripper.getText(document).trim()
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
        val totalPages = document.numberOfPages

        // Ensure correct page order during copying
        for (pageIndex in 0 until totalPages) {
            val page = document.getPage(pageIndex)
            newDocument.addPage(page)
            pageNumberMap[pageIndex] = pageIndex
            Log.d("RENAMING", "Copying page ${pageIndex + 1} of $totalPages")
        }

        val renumberedFile = File(cacheDir, "renumbered_${System.currentTimeMillis()}.pdf")
        newDocument.use { 
            it.save(renumberedFile)
            Log.d("RENAMING", "Saved renumbered PDF with ${it.numberOfPages} pages")
        }
        document.close()

        Log.d("RENAMING", "Renumbered PDF saved as: ${renumberedFile.absolutePath}")
        return renumberedFile
    }

    

    // Safe PDF file download from URL
    private fun downloadPdfFile(urlStr: String): Uri? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000 // 30 seconds
            connection.readTimeout = 60000 // 60 seconds
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("PDF_DOWNLOAD", "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                return null
            }
            
            val file = File(cacheDir, "downloaded_${System.currentTimeMillis()}.pdf")
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            connection.disconnect()
            Log.d("PDF_DOWNLOAD", "File downloaded successfully: ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e("PDF_DOWNLOAD", "Error downloading PDF: ${e.message}", e)
            null
        }
    }



}
