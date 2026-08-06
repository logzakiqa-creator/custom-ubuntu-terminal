package com.custom.terminal

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private val mainHandler = Handler(Looper.getMainLooper())

    private val rootfsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-arm64.tar.gz"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 120, 60, 60)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        statusText = TextView(this).apply {
            text = "🚀 Ubuntu Terminal Auto Bootstrapper\n\nĐang chuẩn bị..."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = false
            setPadding(0, 30, 0, 0)
        }

        layout.addView(statusText)
        layout.addView(progressBar)
        setContentView(layout)

        startBootstrapping()
    }

    private fun startBootstrapping() {
        thread {
            try {
                val ubuntuDir = File(filesDir, "ubuntu")
                val rootfsFile = File(filesDir, "ubuntu-rootfs.tar.gz")

                if (!ubuntuDir.exists()) ubuntuDir.mkdirs()

                // Tải Ubuntu Rootfs
                if (!File(ubuntuDir, "bin/bash").exists()) {
                    updateStatus("Đang tải Ubuntu Rootfs (ARM64)...", 0)
                    
                    downloadFile(rootfsUrl, rootfsFile) { progress ->
                        updateStatus("Đang tải Ubuntu Rootfs: $progress%", progress)
                    }

                    updateStatus("Đang giải nén Ubuntu Rootfs...", 100)
                    extractTarGz(rootfsFile, ubuntuDir)
                    rootfsFile.delete()
                }

                updateStatus("✅ Khởi tạo môi trường Ubuntu thành công!", 100)

            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus("❌ Lỗi: ${e.localizedMessage}", 0)
            }
        }
    }

    private fun downloadFile(urlString: String, destination: File, onProgress: (Int) -> Unit) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server HTTP ${connection.responseCode}")
        }

        val fileLength = connection.contentLength
        val input: InputStream = connection.inputStream
        val output = FileOutputStream(destination)

        val data = ByteArray(8192)
        var total: Long = 0
        var count: Int

        while (input.read(data).also { count = it } != -1) {
            total += count.toLong()
            if (fileLength > 0) {
                val progress = ((total * 100) / fileLength).toInt()
                mainHandler.post { onProgress(progress) }
            }
            output.write(data, 0, count)
        }

        output.flush()
        output.close()
        input.close()
    }

    private fun extractTarGz(tarFile: File, destDir: File) {
        val process = ProcessBuilder("tar", "-xzf", tarFile.absolutePath, "-C", destDir.absolutePath)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
    }

    private fun updateStatus(message: String, progress: Int) {
        mainHandler.post {
            statusText.text = message
            progressBar.progress = progress
        }
    }
}
