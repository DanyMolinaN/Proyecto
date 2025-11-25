package com.example.neuroshelf20.camera.analyzer

import com.example.neuroshelf20.utils.toBitmap

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.util.Log
import com.example.neuroshelf20.domain.DetectionManager

class FrameAnalyzer(
    private val detectionManager: DetectionManager
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {

        try {
            val bitmap = image.toBitmap()

            if (bitmap == null) {
                Log.e("NEURO_FRAME", "❌ No se pudo convertir el frame a Bitmap")
                image.close()
                return
            }

            Log.d("NEURO_FRAME", "📸 Frame recibido: ${bitmap.width}x${bitmap.height}")

            detectionManager.processFrame(bitmap)

        } catch (e: Exception) {
            Log.e("NEURO_FRAME", "🔥 Error en analyze()", e)
        } finally {
            image.close()
        }
    }
}
