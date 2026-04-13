package com.cylonid.nativealpha.waos.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.cylonid.nativealpha.R
import com.cylonid.nativealpha.waos.util.WaosConstants
import java.io.File

class UniversalFileViewerActivity : AppCompatActivity() {
    private lateinit var textViewer: TextView
    private lateinit var imageViewer: ImageView
    private lateinit var videoViewer: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waos_file_viewer)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textViewer = findViewById(R.id.file_text)
        imageViewer = findViewById(R.id.file_image)
        videoViewer = findViewById(R.id.file_video)

        val filePath = intent.getStringExtra(WaosConstants.EXTRA_FILE_PATH) ?: ""
        loadFile(filePath)
    }

    private fun loadFile(filePath: String) {
        if (filePath.isBlank()) {
            showError("No file path provided")
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            showError("File not found: $filePath")
            return
        }

        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"

        textViewer.visibility = View.GONE
        imageViewer.visibility = View.GONE
        videoViewer.visibility = View.GONE

        when {
            mimeType.startsWith("image/") -> showImage(file)
            mimeType.startsWith("video/") -> showVideo(file)
            mimeType.startsWith("audio/") -> playAudio(file)
            extension == "pdf" -> showPdf(file)
            extension in listOf("txt", "log", "csv", "json", "xml", "html") -> showText(file)
            else -> openExternal(file)
        }
    }

    private fun showImage(file: File) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        if (bitmap == null) {
            openExternal(file)
            return
        }
        imageViewer.visibility = View.VISIBLE
        imageViewer.setImageBitmap(bitmap)
    }

    private fun showVideo(file: File) {
        videoViewer.visibility = View.VISIBLE
        videoViewer.setVideoURI(Uri.fromFile(file))
        videoViewer.setMediaController(MediaController(this))
        videoViewer.setOnPreparedListener { mp: MediaPlayer -> mp.isLooping = false }
        videoViewer.start()
    }

    private fun playAudio(file: File) {
        videoViewer.visibility = View.VISIBLE
        videoViewer.setVideoURI(Uri.fromFile(file))
        videoViewer.setMediaController(MediaController(this))
        videoViewer.setOnPreparedListener { mp: MediaPlayer -> mp.start() }
    }

    private fun showPdf(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val bitmap = BitmapFactory.createBitmap(page.width, page.height, BitmapFactory.Options().also {
                        it.inPreferredConfig = BitmapFactory.Config.ARGB_8888
                    })
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    imageViewer.visibility = View.VISIBLE
                    imageViewer.setImageBitmap(bitmap)
                } else {
                    openExternal(file)
                }
            } catch (e: Exception) {
                openExternal(file)
            }
        } else {
            openExternal(file)
        }
    }

    private fun showText(file: File) {
        val text = try {
            file.readText()
        } catch (e: Exception) {
            openExternal(file)
            return
        }
        textViewer.visibility = View.VISIBLE
        textViewer.text = text
    }

    private fun openExternal(file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showError("Cannot open this file type")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
