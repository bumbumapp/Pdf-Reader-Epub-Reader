package com.gitlab.mudlej.MjPdfReader.ui.bookmark

import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.shockwave.pdfium.PdfDocument

interface BookmarkFunctions {
    fun onBookmarkClicked(bookmark: PdfDocument.Bookmark)
}