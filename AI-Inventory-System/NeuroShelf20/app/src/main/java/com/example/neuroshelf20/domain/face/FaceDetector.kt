package com.example.neuroshelf20.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector(private val context: Context) {

    // Configuración REAL de ML Kit
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    /**
     * Detectar rostros en un bitmap
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image).await()
        } catch (e: Exception) {
            Log.e("FaceDetector", "Error detectando rostros", e)
            emptyList()
        }
    }

    /**
     * Recortar rostro desde un rectángulo detectado
     */
    fun cropFace(source: Bitmap, box: Rect): Bitmap? {
        return try {
            val safeRect = Rect(
                box.left.coerceAtLeast(0),
                box.top.coerceAtLeast(0),
                box.right.coerceAtMost(source.width),
                box.bottom.coerceAtMost(source.height)
            )

            if (safeRect.width() <= 0 || safeRect.height() <= 0) return null

            Bitmap.createBitmap(
                source,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
            )
        } catch (e: Exception) {
            Log.e("FaceDetector", "Error recortando rostro", e)
            null
        }
    }
}
