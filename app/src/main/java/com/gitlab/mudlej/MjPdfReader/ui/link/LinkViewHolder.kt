package com.gitlab.mudlej.MjPdfReader.ui.link

import androidx.recyclerview.widget.RecyclerView
import com.gitlab.mudlej.MjPdfReader.data.Link
import com.gitlab.mudlej.MjPdfReader.databinding.LinkItemBinding
import com.gitlab.mudlej.MjPdfReader.util.AdsLoader

class LinkViewHolder(
    private val binding: LinkItemBinding,
    private val linkFunctions: LinkFunctions,
    private val activity: LinksActivity
)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(link: Link) {
        //binding.linkText.text = link.text     // not extracted yet
        binding.linkUri.text = link.url
        binding.linkPageNumber.text = link.pageNumber.toString()

        binding.linkTextsLayout.setOnClickListener {
            AdsLoader.showAds(activity){
                linkFunctions.onLinkClicked(link)
            }
        }
        binding.linkPageNumber.setOnClickListener {
            AdsLoader.showAds(activity) {
                linkFunctions.onPageNumberClicked(link)
            }
        }
        binding.copyButton.setOnClickListener {
            AdsLoader.showAds(activity){
                linkFunctions.onCopyLinkClicked(link)
            }
        }
    }
}