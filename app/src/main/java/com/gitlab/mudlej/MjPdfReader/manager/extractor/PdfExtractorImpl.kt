package com.gitlab.mudlej.MjPdfReader.manager.extractor

import com.gitlab.mudlej.MjPdfReader.data.Link
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File

class PdfExtractorImpl(
    private val pdfiumCore: PdfiumCore,
    private val pdfDocument: PdfDocument
) : PdfExtractor {

    override fun getPageText(pdfFilePath: String, pageNumber: Int): String? {
        var pageText: String? = null
        var file = File(pdfFilePath)

        if (file.exists()) {
            val pdfReader: PdfReader = PdfReader(pdfFilePath)
            pageText = PdfTextExtractor.getTextFromPage(pdfReader,pageNumber)
        }


        return if (pageText == null) "" else pageText
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