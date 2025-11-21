package com.example.neuroshelf.camara

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object CameraUtils {
    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val yuvImage = YuvImage(bytes, ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val yuv = out.toByteArray()
        return BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
    }

    fun cropBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
