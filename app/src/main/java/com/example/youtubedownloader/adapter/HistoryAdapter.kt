package com.example.youtubedownloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.youtubedownloader.R
import com.example.youtubedownloader.model.DownloadHistory
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onItemClick: (DownloadHistory) -> Unit,
    private val onMenuClick: (View, DownloadHistory) -> Unit
) : ListAdapter<DownloadHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnailImage)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val formatChip: Chip = itemView.findViewById(R.id.formatChip)
        private val menuButton: ImageButton = itemView.findViewById(R.id.menuButton)

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

        fun bind(item: DownloadHistory) {
            // Load thumbnail
            Glide.with(thumbnailImage)
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.placeholder_thumbnail)
                .error(R.drawable.error_thumbnail)
                .centerCrop()
                .into(thumbnailImage)

            // Set title
            titleText.text = item.title

            // Set date
            dateText.text = dateFormatter.format(Date(item.downloadDate))

            // Set format info
            formatChip.text = when {
                item.format.isAudioOnly -> "Audio - ${item.format.ext.uppercase()}"
                else -> "${item.format.resolution} - ${item.format.ext.uppercase()}"
            }

            // Set click listeners
            itemView.setOnClickListener { onItemClick(item) }
            menuButton.setOnClickListener { view -> onMenuClick(view, item) }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<DownloadHistory>() {
        override fun areItemsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory): Boolean {
            return oldItem == newItem
        }
    }
}
