package com.cyberscope.reports.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
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
            val file: File
            val uri: Uri
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false
                
                resolver.openOutputStream(uri)?.use { outputStream ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Open file after download
                openFile(context, uri, mimeType)
                
                Toast.makeText(
                    context,
                    "File saved: $fileName",
                    Toast.LENGTH_LONG
                ).show()
                
                true
            } else {
                // Android 9 and below
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file = File(downloadsDir, fileName)
                
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
                    
                    // Open file
                    uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else {
                        Uri.fromFile(file)
                    }
                    
                    openFile(context, uri, mimeType)
                    
                    Toast.makeText(
                        context,
                        "File saved: $fileName",
                        Toast.LENGTH_LONG
                    ).show()
                    
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
    
    private fun openFile(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Cannot open file. Saved to Downloads.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    fun getFileNameFromScanId(scanId: String, extension: String): String {
        val timestamp = System.currentTimeMillis()
        return "CyberScope_${scanId.take(8)}_$timestamp.$extension"
    }
}
