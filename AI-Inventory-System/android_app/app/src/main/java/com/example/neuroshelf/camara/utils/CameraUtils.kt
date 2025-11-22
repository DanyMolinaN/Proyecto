package com.example.neuroshelf.camara

import android.graphics.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object CameraUtils {

    @OptIn(ExperimentalGetImage::class)
    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // NV21 → Y + VU
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Convertimos NV21 → JPEG → Bitmap
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val jpegBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    fun cropBitmap(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): Bitmap {
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
