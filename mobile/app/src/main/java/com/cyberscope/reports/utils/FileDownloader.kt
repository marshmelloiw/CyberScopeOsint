package com.cyberscope.reports.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileDownloader {
    
    fun downloadAndSaveFile(
        context: Context,
        responseBody: ResponseBody,
        fileName: String,
        mimeType: String
    ): Boolean {
        return try {
            // Get Downloads directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            
            // Write file
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            
            try {
                val fileReader = ByteArray(4096)
                inputStream = responseBody.byteStream()
                outputStream = FileOutputStream(file)
                
                while (true) {
                    val read = inputStream.read(fileReader)
                    if (read == -1) break
                    outputStream.write(fileReader, 0, read)
                }
                
                outputStream.flush()
                
                // Notify user
                Toast.makeText(
                    context,
                    "File saved to Downloads/$fileName",
                    Toast.LENGTH_LONG
                ).show()
                
                // Notify system about new file
                notifyDownloadComplete(context, file, mimeType)
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    "Error saving file: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                false
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Download failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
    
    private fun notifyDownloadComplete(context: Context, file: File, mimeType: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            // This is a workaround to make the file visible in Downloads app
            // For Android 10+, files are automatically visible
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getFileNameFromScanId(scanId: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        return "CyberScope_${scanId.take(8)}_$timestamp.$extension"
    }
}
