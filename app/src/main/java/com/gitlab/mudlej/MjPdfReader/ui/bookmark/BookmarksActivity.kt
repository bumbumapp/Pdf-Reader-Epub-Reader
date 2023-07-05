package com.gitlab.mudlej.MjPdfReader.ui.bookmark

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityBookmarksBinding
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractor
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractorFactory
import com.shockwave.pdfium.PdfDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksActivity : AppCompatActivity(), BookmarkFunctions {
    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var pdfExtractor: PdfExtractor
    private lateinit var bookmarkAdapter:BookmarkAdapter
    private var bookmarks: List<PdfDocument.Bookmark> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressBar()
        initPdfExtractor()
        initActionBar()
        initBookmarks()
        initUi()
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun initPdfExtractor() {
        val pdfPath = intent.getStringExtra(PDF.filePathKey)
        pdfExtractor = PdfExtractorFactory.create(this, Uri.parse(pdfPath))
    }

    private fun initBookmarks() {
        CoroutineScope(Dispatchers.Default).launch {
            bookmarks = pdfExtractor.getAllBookmarks()
            Log.d("getAllBookmarks()",""+bookmarks)
            // back to the UI
            withContext(Dispatchers.Main) {
                bookmarkAdapter= BookmarkAdapter(bookmarks,this@BookmarksActivity,this@BookmarksActivity);
                binding.bookmarksRecyclerView.apply {
                    adapter = bookmarkAdapter
                    layoutManager = LinearLayoutManager(this@BookmarksActivity)
                }
                binding.progressBar.visibility = View.GONE
                postGettingBookmarks()
            }
        }
    }

    private fun postGettingBookmarks() {
        if (bookmarks.isNotEmpty()) {
            binding.message.visibility = View.GONE
        }
        else {
            binding.message.text = getString(R.string.no_table_of_contents);
        }
    }

    private fun initActionBar() {
        // add back button to the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Searching..."
    }

    private fun initUi() {
        title = getString(R.string.table_of_contents)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBookmarkClicked(bookmark: PdfDocument.Bookmark) {
        val resultIntent = Intent()
        resultIntent.putExtra(PDF.chosenBookmarkKey, bookmark.pageIdx.toInt())
        setResult(PDF.BOOKMARK_RESULT_OK, resultIntent)
        finish()
    }

    override fun onBackPressed() {
        finish()
    }
    companion object {
        const val TAG = "BookmarksActivity"
    }

}