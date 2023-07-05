package com.gitlab.mudlej.MjPdfReader.ui.link

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.ListAdapter
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.Link
import com.gitlab.mudlej.MjPdfReader.databinding.BookmarksListItemBinding
import com.gitlab.mudlej.MjPdfReader.databinding.LinkItemBinding


class LinkAdapter(
    private val linkFunctions: LinkFunctions,
    val activity: LinksActivity
) : ListAdapter<Link, LinkViewHolder>(LinkComparator()) {

    var nestedQuery: String? = null
    var progressBar: ProgressBar? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        return LinkViewHolder(
            LinkItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            linkFunctions,
            activity
        )
    }

    override fun onBindViewHolder(holder: LinkViewHolder, i: Int) {
        getItem(i)?.let { holder.bind(it) }
    }

    override fun onCurrentListChanged(previousList: MutableList<Link>, currentList: MutableList<Link>) {
        progressBar?.visibility = View.GONE
        super.onCurrentListChanged(previousList, currentList)
    }
}