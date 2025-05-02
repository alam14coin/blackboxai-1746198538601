package com.example.youtubedownloader.utils

import android.content.Context
import android.os.Environment
import com.example.youtubedownloader.api.ApiClient
import com.example.youtubedownloader.api.DownloadRequest
import com.example.youtubedownloader.model.DownloadHistory
import com.example.youtubedownloader.model.Format
import com.example.youtubedownloader.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadManager(private val context: Context) {

    sealed class DownloadState {
        data class Progress(val progress: Int) : DownloadState()
        data class Error(val message: String) : DownloadState()
        data class Success(val history: DownloadHistory) : DownloadState()
    }

    fun downloadVideo(
        videoMetadata: VideoMetadata,
        format: Format
    ): Flow<DownloadState> = flow {
        try {
            // Create download request
            val request = DownloadRequest(videoMetadata.id, format.formatId)

            // Make API call
            val response = ApiClient.service.downloadVideo(request)
            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: ${response.code()}"))
                return@flow
            }

            val body = response.body()
            if (body == null) {
                emit(DownloadState.Error("Empty response"))
                return@flow
            }

            // Create download directory if it doesn't exist
            val downloadDir = getDownloadDirectory()
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Create output file
            val fileName = createFileName(videoMetadata.title, format)
            val outputFile = File(downloadDir, fileName)

            // Write to file and track progress
            val result = writeResponseBodyToFile(body, outputFile) { progress ->
                emit(DownloadState.Progress(progress))
            }

            if (result) {
                // Create download history entry
                val history = DownloadHistory(
                    id = "${videoMetadata.id}_${format.formatId}",
                    title = videoMetadata.title,
                    thumbnailUrl = videoMetadata.thumbnailUrl,
                    downloadDate = System.currentTimeMillis(),
                    format = format,
                    filePath = outputFile.absolutePath
                )
                emit(DownloadState.Success(history))
            } else {
                emit(DownloadState.Error("Failed to save file"))
            }

        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error occurred"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getDownloadDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "YouTubeDownloader"
        )
    }

    private fun createFileName(title: String, format: Format): String {
        val sanitizedTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val quality = when {
            format.isAudioOnly -> "audio"
            else -> format.resolution ?: "video"
        }
        return "${sanitizedTitle}_${quality}.${format.ext}"
    }

    private fun writeResponseBodyToFile(
        body: ResponseBody,
        outputFile: File,
        onProgress: (Int) -> Unit
    ): Boolean {
        try {
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(outputFile)
            val totalBytes = body.contentLength()
            var progressBytes = 0L

            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        progressBytes += read
                        val progress = ((progressBytes * 100) / totalBytes).toInt()
                        onProgress(progress)
                    }
                    output.flush()
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
}
