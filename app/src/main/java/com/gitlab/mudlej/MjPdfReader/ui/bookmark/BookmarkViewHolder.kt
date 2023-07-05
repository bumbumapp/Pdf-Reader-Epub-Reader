package com.gitlab.mudlej.MjPdfReader.ui.bookmark

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.BookmarksListItemBinding
import com.shockwave.pdfium.PdfDocument

class BookmarkViewHolder(
    private val binding: BookmarksListItemBinding,
    private val bookmarkFunctions: BookmarkFunctions,
    private val activity: BookmarksActivity,
    private val bookmarks: List<PdfDocument.Bookmark>
)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(bookmark: PdfDocument.Bookmark) {
        binding.root.removeAllViews()
        binding.root.addView(addSubBookmarkLayout(bookmark))
    }

    private fun addSubBookmarkLayout(subBookmark: PdfDocument.Bookmark): ConstraintLayout {

        val subBookmarkLayout = LayoutInflater.from(activity)
            .inflate(R.layout.children_bookmark_layout, null) as ConstraintLayout

        val subToggleButton = subBookmarkLayout[0] as ImageView
        val subText = subBookmarkLayout[1] as TextView
        val subPageNumber = subBookmarkLayout[2] as TextView
        val subChildrenLayout = subBookmarkLayout[3] as LinearLayoutCompat

        subText.text = bookmarks[position].title
        subPageNumber.text = (bookmarks[position].pageIdx + 1).toString()


        subText.setOnClickListener { bookmarkFunctions.onBookmarkClicked(bookmarks[position]) }
        subPageNumber.setOnClickListener { bookmarkFunctions.onBookmarkClicked(bookmarks[position]) }
        subBookmarkLayout.setOnClickListener { bookmarkFunctions.onBookmarkClicked(bookmarks[position]) }

//            if (subBookmark.level != 0)
//                subBookmarkLayout.setBackgroundResource(R.drawable.transparent_background)

        if (subBookmark.hasChildren()) {
            subChildrenLayout.removeAllViews()
            for (child in subBookmark.children) {
                val layout = addSubBookmarkLayout(child)
                subChildrenLayout.addView(layout)
            }

            subToggleButton.setOnClickListener {
                if (subChildrenLayout.isVisible) {
                    subChildrenLayout.visibility = View.GONE
                    subToggleButton.setImageResource(R.drawable.ic_small_arrow_right)
                } else {
                    subChildrenLayout.visibility = View.VISIBLE
                    subToggleButton.setImageResource(R.drawable.ic_small_arrow_down)
                }
            }
        }
        else {
            subToggleButton.setImageResource(R.drawable.ic_bar)
        }
        return subBookmarkLayout
    }
}