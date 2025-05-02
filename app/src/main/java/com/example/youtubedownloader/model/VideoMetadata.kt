package com.example.youtubedownloader.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoMetadata(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val formats: List<Format>,
    val uploader: String,
    val uploadDate: String,
    val viewCount: Long,
    val description: String? = null
) : Parcelable

@Parcelize
data class Format(
    val formatId: String,
    val ext: String,
    val resolution: String?,
    val filesize: Long?,
    val formatNote: String?,
    val url: String,
    val vcodec: String?,
    val acodec: String?,
    val isAudioOnly: Boolean = false,
    val isVideoOnly: Boolean = false
) : Parcelable {
    
    fun getDisplayName(): String {
        val type = when {
            isAudioOnly -> "Audio"
            isVideoOnly -> "Video only"
            else -> "Video"
        }
        
        val quality = when {
            isAudioOnly -> formatNote ?: "Unknown quality"
            else -> resolution ?: formatNote ?: "Unknown quality"
        }
        
        val size = if (filesize != null) {
            val mb = filesize / (1024 * 1024f)
            String.format("%.1f MB", mb)
        } else ""
        
        return "$type - $quality ($ext) $size".trim()
    }
}

@Parcelize
data class DownloadHistory(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val downloadDate: Long,
    val format: Format,
    val filePath: String
) : Parcelable
