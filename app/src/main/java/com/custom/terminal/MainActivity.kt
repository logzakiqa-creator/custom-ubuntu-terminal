package com.example.ubuntuterminal

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    // Link Ubuntu 22.04 LTS Rootfs (aarch64) & PRoot binary cho Android
    private val rootfsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04-base-arm64.tar.gz"
    private val prootUrl = "https://raw.githubusercontent.com/termux/proot-distro/master/proot-x86_64" // Thay link PRoot binary arm64 chuẩn

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Tạo layout đơn giản bằng Code nếu không dùng XML
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 100, 50, 50)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        statusText = TextView(this).apply {
            text = "🚀 Ubuntu Terminal Auto Bootstrapper\n\nĐang chuẩn bị..."
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
        }

        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            isIndeterminate = false
        }

        layout.addView(statusText)
        layout.addView(progressBar)
        setContentView(layout)

        // Bắt đầu tiến trình khởi tạo
        startBootstrapping()
    }

    private fun startBootstrapping() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val filesDir = filesDir
                val ubuntuDir = File(filesDir, "ubuntu")
                val rootfsFile = File(filesDir, "ubuntu-rootfs.tar.gz")
                val prootFile = File(filesDir, "proot")

                if (!ubuntuDir.exists()) ubuntuDir.mkdirs()

                // 1. Tải PRoot Binary nếu chưa có
                if (!prootFile.exists()) {
                    updateStatus("Đang tải PRoot binary...", 0)
                    downloadFile("https://github.com/termux/termux-packages/raw/master/packages/proot/proot", prootFile) { progress ->
                        updateStatus("Đang tải PRoot executable: $progress%", progress)
                    }
                    prootFile.setExecutable(true, false)
                }

                // 2. Tải Ubuntu Rootfs nếu chưa có
                if (!File(ubuntuDir, "bin/bash").exists()) {
                    updateStatus("Đang tải Ubuntu Rootfs...", 0)
                    downloadFile(rootfsUrl, rootfsFile) { progress ->
                        updateStatus("Đang tải Ubuntu Rootfs: $progress%", progress)
                    }

                    updateStatus("Đang giải nén Ubuntu Rootfs...", 100)
                    extractTarGz(rootfsFile, ubuntuDir)
                    rootfsFile.delete() // Xóa file nén sau khi giải nén xong
                }

                updateStatus("✅ Khởi tạo môi trường Ubuntu thành công!", 100)

            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus("❌ Lỗi: ${e.localizedMessage}", 0)
            }
        }
    }

    private suspend fun downloadFile(urlString: String, destination: File, onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
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
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()
        }
    }

    private suspend fun extractTarGz(tarFile: File, destDir: File) {
        withContext(Dispatchers.IO) {
            // Sử dụng Runtime tar command có sẵn của Android/Linux
            val process = ProcessBuilder("tar", "-xzf", tarFile.absolutePath, "-C", destDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            process.waitFor()
        }
    }

    private suspend fun updateStatus(message: String, progress: Int) {
        withContext(Dispatchers.Main) {
            statusText.text = message
            progressBar.progress = progress
        }
    }
}
