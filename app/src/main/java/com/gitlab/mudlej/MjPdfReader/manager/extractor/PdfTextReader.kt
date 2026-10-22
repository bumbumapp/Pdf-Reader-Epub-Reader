package com.gitlab.mudlej.MjPdfReader.manager.extractor

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.io.File

/**
 * Page text via iText for in-document search. Keeps one reader per file (it used to re-parse the
 * whole PDF for every page) and passes the document password: protected PDFs used to crash the
 * search screen with BadPasswordException.
 */
class PdfTextReader(private val password: String?) {
    private var reader: PdfReader? = null
    private var readerPath: String? = null

    @Synchronized
    fun getPageText(pdfFilePath: String, pageNumber: Int): String {
        return try {
            val r = readerFor(pdfFilePath) ?: return ""
            if (pageNumber < 1 || pageNumber > r.numberOfPages) return ""
            PdfTextExtractor.getTextFromPage(r, pageNumber) ?: ""
        } catch (e: Exception) {
            ""
        } catch (e: LinkageError) {
            ""
        }
    }

    private fun readerFor(path: String): PdfReader? {
        if (readerPath == path) return reader
        close()
        if (!File(path).exists()) return null
        reader = PdfReader(path, password?.toByteArray())
        readerPath = path
        return reader
    }

    @Synchronized
    fun close() {
        reader?.close()
        reader = null
        readerPath = null
    }
}
