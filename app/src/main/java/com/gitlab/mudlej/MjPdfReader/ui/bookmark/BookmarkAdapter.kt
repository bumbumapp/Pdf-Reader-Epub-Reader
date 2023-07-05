package com.gitlab.mudlej.MjPdfReader.ui.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gitlab.mudlej.MjPdfReader.databinding.BookmarksListItemBinding
import com.shockwave.pdfium.PdfDocument


class BookmarkAdapter(
    private var bookmarks:List<PdfDocument.Bookmark>,
    private val bookmarkFunctions: BookmarkFunctions,
    val activity: BookmarksActivity
) : RecyclerView.Adapter<BookmarkViewHolder>() {

    var shouldExpand = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        return BookmarkViewHolder(
            BookmarksListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            bookmarkFunctions,
            activity,
            bookmarks

        )
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, i: Int) {
        val bookmark = bookmarks[i]
        holder.bind(bookmark)
    }

    override fun getItemCount(): Int {
        return if(bookmarks.isEmpty())
            0
        else
            bookmarks.size
    }

}