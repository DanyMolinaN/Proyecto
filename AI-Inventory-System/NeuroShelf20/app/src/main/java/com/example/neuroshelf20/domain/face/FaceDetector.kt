package com.example.neuroshelf20.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector(context: Context) {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .enableTracking()
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        return try {
            detector.process(InputImage.fromBitmap(bitmap, 0)).await()
        } catch (e: Exception) {
            Log.e("FACE_DETECTOR", "❌ Error detectando rostros", e)
            emptyList()
        }
    }

    fun cropFace(src: Bitmap, box: Rect): Bitmap? {
        val rect = Rect(
            box.left.coerceAtLeast(0),
            box.top.coerceAtLeast(0),
            box.right.coerceAtMost(src.width),
            box.bottom.coerceAtMost(src.height)
        )

        return try {
            Bitmap.createBitmap(src, rect.left, rect.top, rect.width(), rect.height())
        } catch (e: Exception) {
            Log.e("FACE_DETECTOR", "❌ Error recortando rostro", e)
            null
        }
    }
}
