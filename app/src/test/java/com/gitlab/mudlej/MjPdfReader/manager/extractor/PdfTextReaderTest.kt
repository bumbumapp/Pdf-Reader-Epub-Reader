package com.gitlab.mudlej.MjPdfReader.manager.extractor

import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/** Regression: search in a password-protected PDF crashed with BadPasswordException. */
class PdfTextReaderTest {

    private fun makePdf(password: String?): File {
        val file = File.createTempFile("search", ".pdf")
        val doc = Document()
        val writer = PdfWriter.getInstance(doc, FileOutputStream(file))
        if (password != null) {
            writer.setEncryption(password.toByteArray(), "owner".toByteArray(),
                PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128)
        }
        doc.open()
        doc.add(Paragraph("Hello searchable world"))
        doc.newPage()
        doc.add(Paragraph("Second page text"))
        doc.close()
        return file
    }

    @Test
    fun readsPlainPdf() {
        val f = makePdf(null)
        val r = PdfTextReader(null)
        assertTrue(r.getPageText(f.path, 1).contains("searchable"))
        assertTrue(r.getPageText(f.path, 2).contains("Second"))
        assertEquals("", r.getPageText(f.path, 3))
    }

    @Test
    fun protectedPdfNeedsPasswordButNeverThrows() {
        val f = makePdf("1234")
        assertEquals("", PdfTextReader(null).getPageText(f.path, 1))
        assertEquals("", PdfTextReader("wrong").getPageText(f.path, 1))
        assertTrue(PdfTextReader("1234").getPageText(f.path, 1).contains("searchable"))
    }

    @Test
    fun missingFileGivesEmptyText() {
        assertEquals("", PdfTextReader(null).getPageText("/no/such/file.pdf", 1))
    }
}
