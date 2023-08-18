package com.gitlab.mudlej.MjPdfReader.ui.search

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import android.view.Display
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.data.SearchResult
import com.gitlab.mudlej.MjPdfReader.databinding.ActivitySearch2Binding
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractor
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractorFactory
import com.gitlab.mudlej.MjPdfReader.util.indexesOf
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class SearchActivity2 : AppCompatActivity(), SearchResultFunctions {

    private val tag = javaClass.name
    private lateinit var binding: ActivitySearch2Binding
    private val searchResultAdapter = SearchResultAdapter(this)
    private var searchResults: MutableList<SearchResult> = mutableListOf()
    private lateinit var pdfExtractor: PdfExtractor
    private var path:String? = null
    private var pdfPath:String? = null
    private val lastPageLiveData = MutableLiveData<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearch2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        pdfPath = intent.getStringExtra(PDF.filePathKey)
        val uri = Uri.parse(pdfPath)
        path = getFilePathForN(uri,this)
        showProgressBar()
        initPdfExtractor()
        initActionBar()
        initSearchResults()
        initLoadingProgressBar()
        initRecyclerView()

    }

    private fun initLoadingProgressBar() {
        binding.searchProgressBar.show()
        binding.searchProgressBar.max = pdfExtractor.getPageCount()
        lastPageLiveData.observe(this@SearchActivity2) { pageNumber ->
            binding.searchProgressBar.progress = pageNumber
        }
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun initActionBar() {
        // add back button to the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.searching)
    }

    private fun initPdfExtractor() {
        pdfExtractor = PdfExtractorFactory.create(this, Uri.parse(pdfPath))
    }

    private fun initRecyclerView() {
        searchResultAdapter.submitList(searchResults)
        searchResultAdapter.progressBar = binding.progressBar

        binding.searchRecyclerView.apply {
            adapter = searchResultAdapter
            layoutManager = LinearLayoutManager(this@SearchActivity2)
        }
    }

    private fun initSearchResults() {
        val query: String? = intent.getStringExtra(PDF.searchQueryKey)
        if (query.isNullOrEmpty() || query.isBlank()) {
            return
        }

        CoroutineScope(Dispatchers.Default).launch {
            searchResults = search(query)
            searchResultAdapter.submitList(searchResults)
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.searchProgressBar.hide()
                postSearch()
            }
        }
        return
    }

    private fun search(query: String): MutableList<SearchResult> {
        val searchResults = mutableListOf<SearchResult>()
        val time = measureTimeMillis {
            for (pageNumber in 1 .. pdfExtractor.getPageCount()) {
                val pageText = pdfExtractor.getPageText(path!!,pageNumber)
                if (pageText!!.isEmpty() || pageText!!.isBlank()) {
                    continue
                }

                pageText.indexesOf(query, ignoreCase = true).forEach { indexInPage ->
//                    Log.d(tag, "search: ${
//                        pageText.substring(
//                            max(indexInPage - 10, indexInPage),
//                            min(indexInPage + 10, pdfExtractor.getPageCount() + indexInPage + 10)
//                        )}"
//                    )
                    searchResults.add(getPageResult(query, indexInPage, pageText, pageNumber))
                }
                lastPageLiveData.postValue(pageNumber)
            }
        }
        Log.d(tag, "getSearchResults: elapsed time: ${time / 1000F}s")
        return searchResults
    }

    private fun getPageResult(
        query: String,
        indexInPage: Int,
        pageText: String,
        pageNumber: Int,
        textOffset: Int? = null,
        expanded: Boolean = false
    ): SearchResult {

        val offset = textOffset ?: PDF.SEARCH_RESULT_OFFSET
        val count = query.length

        val starting = max(0, indexInPage - offset)
        val ending = min(pageText.length, indexInPage + count + offset)
        val resultText = pageText.substring(startIndex = starting, endIndex = ending)

        // remove half words (e.g. "er can I found hi" -> "can I found")
        val queryIndex = indexInPage - starting
        val firstSpace = resultText.indexOf(" ") + 1
        val start = if (firstSpace != -1) min(firstSpace, queryIndex) else 0

        val lastSpace = resultText.lastIndexOf(" ")
        val end = if (lastSpace != -1) max(lastSpace, queryIndex + count) else resultText.length

        val trimmedText = resultText.substring(start, end)
        val newStart = queryIndex - start

        return SearchResult(
            originalIndex = indexInPage,
            inputStart = newStart,
            inputEnd = newStart + count,
            text = trimmedText,
            pageNumber = pageNumber,
            expanded = expanded
        )
    }

    private fun postSearch() {
        // set up the title in the App Bar
        title = "${"%,d".format(searchResults.size)} ${getString(R.string.search_results)}"

        // show too many results message
        if (searchResults.size > PDF.TOO_MANY_RESULTS) {
            Snackbar.make(binding.root,getString(R.string.too_many_results_may_be_slow), Snackbar.LENGTH_INDEFINITE).also {
                it.setAction(getText(R.string.ok)) { }
                it.show()
            }
        }
    }

    private fun initFakeData(size: Int = 5000) {
        // generate fake data
        val input = "big"
        val texts = listOf(
            "When the big guy hit the big wall with a very very very big hammer.",
            "When the big guy have big hammer.",
            "Change the color of a few characters, make them big clickable, scale the size of the text or even draw custom bullet points",
        )

        fun createSearchResult(): SearchResult {
            val index = Random.nextInt(0, texts.size)
            val start = texts[index].indexOf(input)
            val end = start + input.length
            return SearchResult(originalIndex = start, start, end, texts[index], pageNumber = Random.nextInt())
        }

        searchResults = MutableList(size) { _ -> createSearchResult() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // set search functionality
        val searchView = menu.findItem(R.id.search_in_search_activity).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(query: String): Boolean {
                searchResultAdapter.nestedQuery = query
                binding.progressBar.visibility = View.VISIBLE
                val filteredList = searchResults.filter { it.text.contains(query, true) }
                searchResultAdapter.submitList(filteredList)
                searchResultAdapter.notifyDataSetChanged() // because the comparator doesn't see the difference in text style
                Snackbar.make(
                    binding.root,
                    getString(R.string.number_of_filtered_results).format(filteredList.size),
                    Snackbar.LENGTH_SHORT
                ).show()
                return false
            }
        })
        searchView.setOnCloseListener {
            searchResultAdapter.submitList(searchResults)
            true
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onSearchResultClicked(searchResult: SearchResult) {
        val resultIntent = Intent()
        resultIntent.putExtra(PDF.searchResultKey, Gson().toJson(searchResult))
        setResult(PDF.SEARCH_RESULT_OK, resultIntent)
        finish()
    }
    @SuppressLint("Recycle")
    private fun getFilePathForN(uri: Uri, context: Context): String? {
        var filePath = uri.path.toString()
        val file = File(filePath)
        if (file.exists()){
            return filePath
        }
        else{
            val returnCursor = context.contentResolver.query(uri, null, null, null, null)
            val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
            val file = File(context.filesDir, name)
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)
                var read = 0
                val maxBufferSize = 1 * 1024 * 1024
                val bytesAvailable = inputStream!!.available()

                //int bufferSize = 1024;
                val bufferSize = Math.min(bytesAvailable, maxBufferSize)
                val buffers = ByteArray(bufferSize)
                while (inputStream.read(buffers).also { read = it } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                Log.e("File Size", "Size " + file.length())
                inputStream.close()
                outputStream.close()
                Log.e("File Path", "Path " + file.path)
                Log.e("File Size", "Size " + file.length())
            } catch (e: java.lang.Exception) {
                Log.e("Exception", e.message!!)
            }
            return file.path
        }

    }


    override fun onShowMoreResultTextClicked(searchResult: SearchResult, searchResultIndex: Int): SearchResult {
        val query = searchResult.text.substring(searchResult.inputStart, searchResult.inputEnd)
        val pageText = pdfExtractor.getPageText(path!!,searchResult.pageNumber)
        val newSearchResult = getPageResult(
            query,
            searchResult.originalIndex,
            pageText!!,
            searchResult.pageNumber,
            200,
            expanded = true
        )
        val index = searchResults.indexOf(searchResult)
        if (index == -1) throw RuntimeException("index is -1!!")
        searchResults[index] = newSearchResult
        searchResultAdapter.notifyItemChanged(index)
        //searchResultAdapter.notifyItemChanged(searchResultIndex)

        return newSearchResult
    }
}