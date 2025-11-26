package com.example.neuroshelf20.domain.face

import android.content.Context
import java.io.File
import java.io.FileOutputStream

fun assetFilePath(context: Context, assetName: String): String {
    val file = File(context.filesDir, assetName)

    if (!file.exists() || file.length() == 0L) {
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
        }
    }
    return file.absolutePath
}
