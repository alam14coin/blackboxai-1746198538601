package com.example.youtubedownloader

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.youtubedownloader.databinding.ActivityVideoPreviewBinding
import com.example.youtubedownloader.model.Format
import com.example.youtubedownloader.model.VideoMetadata
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource

class VideoPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIDEO_METADATA = "extra_video_metadata"
        const val EXTRA_FORMAT = "extra_format"
    }

    private lateinit var binding: ActivityVideoPreviewBinding
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        
        // Restore state
        savedInstanceState?.let { bundle ->
            playWhenReady = bundle.getBoolean("play_when_ready")
            currentWindow = bundle.getInt("current_window")
            playbackPosition = bundle.getLong("playback_position")
        }
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("play_when_ready", playWhenReady)
        outState.putInt("current_window", currentWindow)
        outState.putLong("playback_position", playbackPosition)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            
            // Get video metadata from intent
            val video = intent.getParcelableExtra<VideoMetadata>(EXTRA_VIDEO_METADATA)
            title = video?.title ?: getString(R.string.app_name)
        }
    }

    private fun initializePlayer() {
        val video = intent.getParcelableExtra<VideoMetadata>(EXTRA_VIDEO_METADATA)
        val format = intent.getParcelableExtra<Format>(EXTRA_FORMAT)
        
        if (video == null || format == null) {
            showError()
            return
        }

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            
            val mediaSource = ProgressiveMediaSource.Factory(
                DefaultHttpDataSource.Factory()
            ).createMediaSource(MediaItem.fromUri(format.url))

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.seekTo(currentWindow, playbackPosition)
            exoPlayer.addListener(playbackStateListener)
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentWindow = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    }

    private val playbackStateListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            val stateString = when (state) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE"
                ExoPlayer.STATE_BUFFERING -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                    "ExoPlayer.STATE_BUFFERING"
                }
                ExoPlayer.STATE_READY -> {
                    binding.loadingProgressBar.visibility = View.GONE
                    "ExoPlayer.STATE_READY"
                }
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED"
                else -> "UNKNOWN_STATE"
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                binding.loadingProgressBar.visibility = View.GONE
            }
        }
    }

    private fun showError() {
        binding.playerView.visibility = View.GONE
        // You could show an error message here
        finish()
    }
}
