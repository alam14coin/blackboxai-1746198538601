package com.example.youtubedownloader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.youtubedownloader.adapter.FormatAdapter
import com.example.youtubedownloader.api.ApiClient
import com.example.youtubedownloader.api.MetadataRequest
import com.example.youtubedownloader.api.Result
import com.example.youtubedownloader.api.safeApiCall
import com.example.youtubedownloader.databinding.ActivityMainBinding
import com.example.youtubedownloader.model.Format
import com.example.youtubedownloader.model.VideoMetadata
import com.example.youtubedownloader.utils.DownloadManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var formatAdapter: FormatAdapter
    private lateinit var downloadManager: DownloadManager
    private var currentVideo: VideoMetadata? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        
        downloadManager = DownloadManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        formatAdapter = FormatAdapter(
            onDownloadClick = { format -> startDownload(format) },
            onPreviewClick = { format -> openVideoPreview(format) }
        )

        binding.formatsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = formatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fetchButton.setOnClickListener {
            val url = binding.urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                fetchVideoMetadata(url)
            } else {
                showError(getString(R.string.error_invalid_url))
            }
        }

        binding.historyFab.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun fetchVideoMetadata(url: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            when (val result = safeApiCall { 
                ApiClient.service.getVideoMetadata(MetadataRequest(url))
            }) {
                is Result.Success -> {
                    val response = result.data
                    if (response.success && response.data != null) {
                        showVideoMetadata(response.data)
                    } else {
                        showError(response.error ?: getString(R.string.error_fetch_failed))
                    }
                }
                is Result.Error -> showError(result.message)
                Result.Loading -> Unit // Already showing loading
            }
            showLoading(false)
        }
    }

    private fun showVideoMetadata(metadata: VideoMetadata) {
        currentVideo = metadata
        
        binding.videoInfoCard.visibility = View.VISIBLE
        binding.formatsRecyclerView.visibility = View.VISIBLE

        // Load thumbnail
        Glide.with(this)
            .load(metadata.thumbnailUrl)
            .placeholder(R.drawable.placeholder_thumbnail)
            .error(R.drawable.error_thumbnail)
            .into(binding.thumbnailImage)

        binding.videoTitle.text = metadata.title
        
        // Update formats list
        formatAdapter.submitList(metadata.formats)
    }

    private fun startDownload(format: Format) {
        val video = currentVideo ?: return
        
        lifecycleScope.launch {
            downloadManager.downloadVideo(video, format).collect { state ->
                when (state) {
                    is DownloadManager.DownloadState.Progress -> {
                        formatAdapter.updateDownloadProgress(format.formatId, state.progress)
                    }
                    is DownloadManager.DownloadState.Success -> {
                        formatAdapter.clearDownloadProgress(format.formatId)
                        showSuccess(getString(R.string.success_download))
                    }
                    is DownloadManager.DownloadState.Error -> {
                        formatAdapter.clearDownloadProgress(format.formatId)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun openVideoPreview(format: Format) {
        val video = currentVideo ?: return
        val intent = Intent(this, VideoPreviewActivity::class.java).apply {
            putExtra(VideoPreviewActivity.EXTRA_VIDEO_METADATA, video)
            putExtra(VideoPreviewActivity.EXTRA_FORMAT, format)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fetchButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
}
