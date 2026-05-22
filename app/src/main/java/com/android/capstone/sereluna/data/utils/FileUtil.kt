package com.android.capstone.sereluna.data.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {
    fun uriToFile(context: Context, uri: Uri): File? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

            inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            
            val tempFile = File(context.cacheDir, "temp_upload_file_${System.currentTimeMillis()}.$extension")
            outputStream = FileOutputStream(tempFile)
            
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }
}
