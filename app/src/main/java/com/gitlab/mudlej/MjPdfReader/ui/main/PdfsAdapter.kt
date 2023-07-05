package com.gitlab.mudlej.MjPdfReader.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.gitlab.mudlej.MjPdfReader.BuildConfig
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.databinding.PdfItemsBinding
import com.gitlab.mudlej.MjPdfReader.entity.RecentPaths
import java.io.File

class PdfsAdapter(
    private var pdfs: List<RecentPaths>,
    private var itemOnClickListener: ItemOnClickListener
):RecyclerView.Adapter<PdfsAdapter.PdfsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfsAdapter.PdfsViewHolder {
        val view=PdfItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfsViewHolder(view);
    }



    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PdfsAdapter.PdfsViewHolder, position: Int) {
                 holder.binding.docuName.text=pdfs[position].name
                 holder.binding.docuDate.text= pdfs[position].date.toString() + " • " + pdfs[position].parent

                 if (pdfs[position].name!!.endsWith(".epub")){
                     holder.binding.icon.setImageResource(R.drawable.epub_ic)
                 }
        holder.itemView.setOnClickListener {
            val pathFromUri=Uri.parse(pdfs[position].path)
            pathFromUri.let { path -> itemOnClickListener.onItemClick(path) }
        }
    }

    override fun getItemCount(): Int {
        return if (pdfs.isEmpty()){
            0
        }else
            pdfs.size
    }

    class PdfsViewHolder(val binding: PdfItemsBinding):RecyclerView.ViewHolder(binding.root) {

    }
}