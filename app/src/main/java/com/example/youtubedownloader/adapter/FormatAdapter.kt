package com.example.youtubedownloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.youtubedownloader.R
import com.example.youtubedownloader.model.Format
import com.google.android.material.button.MaterialButton

class FormatAdapter(
    private val onDownloadClick: (Format) -> Unit,
    private val onPreviewClick: (Format) -> Unit
) : ListAdapter<Format, FormatAdapter.FormatViewHolder>(FormatDiffCallback()) {

    private val downloadingItems = mutableMapOf<String, Int>() // formatId to progress

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_format, parent, false)
        return FormatViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
        val format = getItem(position)
        holder.bind(format)
    }

    fun updateDownloadProgress(formatId: String, progress: Int) {
        downloadingItems[formatId] = progress
        notifyItemChanged(currentList.indexOfFirst { it.formatId == formatId })
    }

    fun clearDownloadProgress(formatId: String) {
        downloadingItems.remove(formatId)
        notifyItemChanged(currentList.indexOfFirst { it.formatId == formatId })
    }

    inner class FormatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val formatIcon: ImageView = itemView.findViewById(R.id.formatIcon)
        private val formatInfo: TextView = itemView.findViewById(R.id.formatInfo)
        private val downloadButton: MaterialButton = itemView.findViewById(R.id.downloadButton)
        private val downloadProgress: ProgressBar = itemView.findViewById(R.id.downloadProgress)

        fun bind(format: Format) {
            // Set format icon based on type
            formatIcon.setImageResource(
                when {
                    format.isAudioOnly -> R.drawable.ic_audio
                    format.isVideoOnly -> R.drawable.ic_video_only
                    else -> R.drawable.ic_video
                }
            )

            // Set format information
            formatInfo.text = format.getDisplayName()

            // Handle download progress
            val progress = downloadingItems[format.formatId]
            if (progress != null) {
                downloadButton.visibility = View.INVISIBLE
                downloadProgress.visibility = View.VISIBLE
                downloadProgress.progress = progress
            } else {
                downloadButton.visibility = View.VISIBLE
                downloadProgress.visibility = View.GONE
            }

            // Set click listeners
            downloadButton.setOnClickListener {
                onDownloadClick(format)
            }

            // Enable preview only for video formats
            if (!format.isAudioOnly) {
                itemView.setOnClickListener {
                    onPreviewClick(format)
                }
            }
        }
    }

    private class FormatDiffCallback : DiffUtil.ItemCallback<Format>() {
        override fun areItemsTheSame(oldItem: Format, newItem: Format): Boolean {
            return oldItem.formatId == newItem.formatId
        }

        override fun areContentsTheSame(oldItem: Format, newItem: Format): Boolean {
            return oldItem == newItem
        }
    }
}
