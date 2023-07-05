package com.gitlab.mudlej.MjPdfReader.manager.extractor

import android.app.Activity
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore

object PdfExtractorFactory {

    fun create(activity: Activity, uri: Uri): PdfExtractor {
        val pdfium = PdfiumCore(activity)
        var parcelFileDescriptor: ParcelFileDescriptor? = null
        try {
            parcelFileDescriptor = activity.contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        val pdfDocument = pdfium.newDocument(parcelFileDescriptor)
        return PdfExtractorImpl(pdfium, pdfDocument)
    }
}