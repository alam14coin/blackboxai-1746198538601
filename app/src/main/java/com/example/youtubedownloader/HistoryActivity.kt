package com.example.youtubedownloader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.youtubedownloader.adapter.HistoryAdapter
import com.example.youtubedownloader.databinding.ActivityHistoryBinding
import com.example.youtubedownloader.model.DownloadHistory
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.ArrayList

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: HistoryAdapter
    private val downloadHistory = ArrayList<DownloadHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadDownloadHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_history -> {
                showClearHistoryDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { history -> playVideo(history) },
            onMenuClick = { view, history -> showItemMenu(view, history) }
        )

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun loadDownloadHistory() {
        // In a real app, you would load this from a database
        // For now, we'll just update the UI
        updateHistoryUI()
    }

    private fun updateHistoryUI() {
        if (downloadHistory.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.historyRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.historyRecyclerView.visibility = View.VISIBLE
            historyAdapter.submitList(downloadHistory)
        }
    }

    private fun playVideo(history: DownloadHistory) {
        try {
            val file = File(history.filePath)
            if (!file.exists()) {
                showError("File not found")
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (history.format.isAudioOnly) "audio/*" else "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(intent)
        } catch (e: Exception) {
            showError("Cannot open file: ${e.message}")
        }
    }

    private fun showItemMenu(view: View, history: DownloadHistory) {
        PopupMenu(this, view).apply {
            inflate(R.menu.history_item_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        deleteHistoryItem(history)
                        true
                    }
                    R.id.action_share -> {
                        shareFile(history)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun deleteHistoryItem(history: DownloadHistory) {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage("Delete ${history.title}?")
            .setPositiveButton(R.string.yes) { _, _ ->
                // Delete file
                try {
                    File(history.filePath).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Remove from list and update UI
                downloadHistory.remove(history)
                updateHistoryUI()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun shareFile(history: DownloadHistory) {
        try {
            val file = File(history.filePath)
            if (!file.exists()) {
                showError("File not found")
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (history.format.isAudioOnly) "audio/*" else "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            showError("Cannot share file: ${e.message}")
        }
    }

    private fun showClearHistoryDialog() {
        if (downloadHistory.isEmpty()) {
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_clear_history)
            .setMessage("This will only clear the history list, not delete the actual files.")
            .setPositiveButton(R.string.yes) { _, _ ->
                downloadHistory.clear()
                updateHistoryUI()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
