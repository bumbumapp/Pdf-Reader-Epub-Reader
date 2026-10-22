package com.gitlab.mudlej.MjPdfReader.manager.extractor

import android.app.Activity
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore

object PdfExtractorFactory {

    /** @throws java.io.IOException if the file can't be opened (missing, revoked permission, wrong password). */
    @Throws(java.io.IOException::class)
    fun create(activity: Activity, uri: Uri, password: String? = null): PdfExtractor {
        val pdfium = PdfiumCore(activity)
        val parcelFileDescriptor: ParcelFileDescriptor = try {
            activity.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            throw java.io.IOException("Cannot open $uri", e)
        } ?: throw java.io.FileNotFoundException("Cannot open $uri")
        val pdfDocument = pdfium.newDocument(parcelFileDescriptor, password)
        return PdfExtractorImpl(pdfium, pdfDocument, password)
    }
}