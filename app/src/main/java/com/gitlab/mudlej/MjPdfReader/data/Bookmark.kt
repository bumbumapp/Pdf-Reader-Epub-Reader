package com.gitlab.mudlej.MjPdfReader.data

import android.util.Log
import com.shockwave.pdfium.PdfDocument

class Bookmark(bookmark:PdfDocument.Bookmark, val level: Int) : PdfDocument.Bookmark() {
    val subBookmarks: MutableList<Bookmark> = mutableListOf()

    init {


        // add all children recursively
        if (hasChildren())
            for (child in children)
                subBookmarks.add(Bookmark(child, level + 1))
    }

    fun hasSubBookmarks() = subBookmarks.isNotEmpty()
}