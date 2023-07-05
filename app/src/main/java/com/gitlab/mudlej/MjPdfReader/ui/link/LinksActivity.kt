package com.gitlab.mudlej.MjPdfReader.ui.link

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Link
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityLinkBinding
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractor
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractorFactory
import com.gitlab.mudlej.MjPdfReader.util.copyToClipboard
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinksActivity : AppCompatActivity(), LinkFunctions {
    private lateinit var binding: ActivityLinkBinding
    private lateinit var pdfExtractor: PdfExtractor
    private val linkAdapter = LinkAdapter(this, this)
    private var links: List<Link> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showProgressBar()
        initPdfExtractor()
        initActionBar()
        initLinks()
        initUi()
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun initPdfExtractor() {
        val pdfPath = intent.getStringExtra(PDF.filePathKey)
        pdfExtractor = PdfExtractorFactory.create(this, Uri.parse(pdfPath))
    }

    private fun initLinks() {
        CoroutineScope(Dispatchers.Default).launch {
            links = pdfExtractor.getAllLinks()
            linkAdapter.submitList(links)

            // back to the UI
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                postGettingLinks()
            }
        }
    }
    override fun onBackPressed() {
        finish()
    }
    private fun postGettingLinks() {
        if (links.isNotEmpty()) {
            binding.message.visibility = View.GONE
        }
        else {
            binding.message.text = getString(R.string.no_links_put_in_pdf)
        }

        // set up the title in the App Bar
        title = "${"%,d".format(links.size)} ${getString(R.string.links_in_document)}"

        // show too many results message
        if (links.size > PDF.TOO_MANY_RESULTS) {
            Snackbar.make(binding.root,getString(R.string.too_many_results_may_be_slow), Snackbar.LENGTH_INDEFINITE).also {
                it.setAction(getText(R.string.ok)) { }
                it.show()
            }
        }
    }

    private fun initActionBar() {
        // add back button to the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = getString(R.string.loading)
    }

    private fun initUi() {
        title = getString(R.string.links_activity_title)
        linkAdapter.submitList(links)
        linkAdapter.progressBar = binding.progressBar
        binding.linkRecyclerView.apply {
            adapter = linkAdapter
            layoutManager = LinearLayoutManager(this@LinksActivity)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search_menu2, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // set search functionality
        val searchView = menu.findItem(R.id.search_in_search_activity).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(query: String): Boolean {
                linkAdapter.nestedQuery = query
                binding.progressBar.visibility = View.VISIBLE
                val filteredList = links.filter {
                    it.url.contains(query, true)  || it.text.contains(query, true)
                }
                linkAdapter.submitList(filteredList)
                linkAdapter.notifyDataSetChanged() // because the comparator doesn't see the difference in text style

                Snackbar.make(
                    binding.root,
                    getString(R.string.number_of_filtered_results).format(filteredList.size),
                    Snackbar.LENGTH_SHORT
                ).show()
                return false
            }
        })
        searchView.setOnCloseListener {
            linkAdapter.submitList(links)
            true
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onLinkClicked(link: Link) {
        Intent(Intent.ACTION_VIEW).also {
            it.data = (Uri.parse(link.url))
            try {
                startActivity(it)
            } catch (throwable: Throwable) {
                Snackbar.make(binding.root, getString(R.string.no_app_to_open_link), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onPageNumberClicked(link: Link) {
        Intent().also { resultIntent ->
            resultIntent.putExtra(PDF.linkResultKey, link.pageNumber)
            setResult(PDF.LINK_RESULT_OK, resultIntent)
        }
        finish()
    }

    override fun onCopyLinkClicked(link: Link) {
        val copyLabel = "Link URL copy"
        copyToClipboard(this, copyLabel, link.url)

        // show message to user before closing
        Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val TAG = "LinksActivity"
    }

}