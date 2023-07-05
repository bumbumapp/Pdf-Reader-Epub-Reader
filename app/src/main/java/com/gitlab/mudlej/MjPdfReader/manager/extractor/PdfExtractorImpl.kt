package com.gitlab.mudlej.MjPdfReader.manager.extractor

import com.gitlab.mudlej.MjPdfReader.data.Link
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

class PdfExtractorImpl(
    private val pdfiumCore: PdfiumCore,
    private val pdfDocument: PdfDocument
) : PdfExtractor {

    override fun getPageText(pageNumber: Int): String {
        val index = getIndex(pageNumber) ?: return ""
        pdfiumCore.openPage(pdfDocument, index)

        return try {
           ""
        }
        catch (throwable: Throwable) {
            throwable.printStackTrace()
            "";
        }
    }

    override fun getPageCount() = pdfiumCore.getPageCount(pdfDocument)

    override fun getPageLinks(pageNumber: Int): List<PdfDocument.Link> {
        try {
            pdfiumCore.openPage(pdfDocument, pageNumber)
        }
        catch (throwable: Throwable) {
            throwable.printStackTrace()
            return listOf()
        }
        return pdfiumCore.getPageLinks(pdfDocument, pageNumber).filter { it.uri != null }
    }

    override fun getAllBookmarks(): List<PdfDocument.Bookmark> {
        return pdfiumCore.getTableOfContents(pdfDocument)
    }

    override fun getAllLinks(): List<Link> {
        val links = mutableListOf<Link>()
        for (i in 0 until getPageCount()) {
            val pageLinks = getPageLinks(i)
            for (link in pageLinks) {
                links.add(Link(
                    text = "",      // couldn't be extracted yet
                    url = link.uri,
                    pageNumber = i + 1
                ))
            }
        }
        return links
    }

    private fun getIndex(pageNumber: Int): Int? {
        return if (pageNumber < 1) null else pageNumber - 1
    }
}