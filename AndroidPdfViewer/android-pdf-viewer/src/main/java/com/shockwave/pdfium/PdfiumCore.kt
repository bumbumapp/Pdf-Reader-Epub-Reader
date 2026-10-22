package com.shockwave.pdfium

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.ParcelFileDescriptor
import android.util.Log
import com.harissk.pdfium.exception.IncorrectPasswordException
import com.shockwave.pdfium.util.Size
import kotlinx.coroutines.runBlocking
import java.io.IOException
import com.harissk.pdfium.Bookmark as HBookmark
import com.harissk.pdfium.PdfiumCore as Engine

/**
 * Drop-in replacement for the com.shockwave.pdfium.PdfiumCore API used by the PDF viewer, backed by
 * io.github.rhariskumar3:pdfium (maintained, 16 KB page aligned native libraries). Each
 * [PdfDocument] owns its own engine instance.
 */
class PdfiumCore(@Suppress("UNUSED_PARAMETER") ctx: Context?) {

    private fun engine(doc: PdfDocument): Engine =
        doc.engine as? Engine ?: throw IllegalStateException("Document already closed")

    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor): PdfDocument = newDocument(fd, null)

    @Throws(IOException::class)
    fun newDocument(fd: ParcelFileDescriptor, password: String?): PdfDocument {
        val engine = open { it.newDocument(fd, password) }
        return PdfDocument().also {
            it.engine = engine
            it.parcelFileDescriptor = fd
        }
    }

    @Throws(IOException::class)
    fun newDocument(data: ByteArray): PdfDocument = newDocument(data, null)

    @Throws(IOException::class)
    fun newDocument(data: ByteArray, password: String?): PdfDocument {
        val engine = open { it.newDocument(data, password) }
        return PdfDocument().also { it.engine = engine }
    }

    private inline fun open(block: (Engine) -> Unit): Engine {
        val engine = Engine()
        try {
            block(engine)
        } catch (e: IncorrectPasswordException) {
            engine.close()
            throw PdfPasswordException(e.message ?: "Password required or incorrect password.")
        } catch (e: IOException) {
            engine.close()
            throw e
        } catch (e: Exception) {
            engine.close()
            if (isPasswordError(e)) throw PdfPasswordException(e.message ?: "Password required.")
            throw IOException(e.message, e)
        }
        return engine
    }

    fun getPageCount(doc: PdfDocument): Int = engine(doc).pageCount

    fun openPage(doc: PdfDocument, pageIndex: Int): Long {
        doc.mNativePagesPtr[pageIndex]?.let { return it }
        val ptr = engine(doc).openPage(pageIndex)
        doc.mNativePagesPtr[pageIndex] = ptr
        return ptr
    }

    fun getPageWidthPoint(doc: PdfDocument, index: Int): Int = engine(doc).getPageWidthPoint(index)

    fun getPageHeightPoint(doc: PdfDocument, index: Int): Int = engine(doc).getPageHeightPoint(index)

    fun getPageSize(doc: PdfDocument, index: Int): Size {
        val s = engine(doc).getPageSize(index)
        return Size(s.width, s.height)
    }

    @JvmOverloads
    fun renderPageBitmap(
        doc: PdfDocument, bitmap: Bitmap, pageIndex: Int,
        startX: Int, startY: Int, drawSizeX: Int, drawSizeY: Int, renderAnnot: Boolean = false
    ) {
        try {
            engine(doc).renderPageBitmap(bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, renderAnnot)
        } catch (e: Exception) {
            Log.e(TAG, "renderPageBitmap failed", e)
        }
    }

    fun closeDocument(doc: PdfDocument) {
        (doc.engine as? Engine)?.close()
        doc.engine = null
        doc.mNativePagesPtr.clear()
        try {
            doc.parcelFileDescriptor?.close()
        } catch (ignored: IOException) {
        }
        doc.parcelFileDescriptor = null
    }

    fun getDocumentMeta(doc: PdfDocument): PdfDocument.Meta {
        val m = runBlocking { engine(doc).getDocumentMeta() }
        return PdfDocument.Meta().apply {
            title = m.title
            author = m.author
            subject = m.subject
            keywords = m.keywords
            creator = m.creator
            producer = m.producer
            creationDate = m.creationDate
            modDate = m.modDate
        }
    }

    fun getTableOfContents(doc: PdfDocument): List<PdfDocument.Bookmark> =
        runBlocking { engine(doc).getTableOfContents() }.map { convert(it) }

    private fun convert(b: HBookmark): PdfDocument.Bookmark = PdfDocument.Bookmark().apply {
        title = b.title
        pageIdx = b.pageIdx
        mNativePtr = b.mNativePtr
        setChildren(b.children.map { convert(it) })
    }

    fun getPageLinks(doc: PdfDocument, pageIndex: Int): List<PdfDocument.Link> =
        engine(doc).getPageLinks(pageIndex).map { PdfDocument.Link(it.bounds, it.destPageIdx, it.uri) }

    fun mapRectToDevice(
        doc: PdfDocument, pageIndex: Int, startX: Int, startY: Int, sizeX: Int, sizeY: Int,
        rotate: Int, coords: RectF
    ): RectF = engine(doc).mapPageCoordinateToDevice(pageIndex, startX, startY, sizeX, sizeY, rotate, coords)

    companion object {
        private const val TAG = "PdfiumCore"

        /** Some engine versions report a missing/wrong password as a generic error. */
        @JvmStatic
        fun isPasswordError(e: Throwable): Boolean {
            val msg = (e.message ?: "").lowercase()
            return msg.contains("password")
        }
    }
}
