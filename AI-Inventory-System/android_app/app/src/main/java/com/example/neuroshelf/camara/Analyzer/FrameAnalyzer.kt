package com.example.neuroshelf.camara.Analyzer

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.neuroshelf.camara.CameraUtils   // ✅ CORREGIDO
import com.example.neuroshelf.domain.DetectionManager
import kotlin.system.measureTimeMillis

class FrameAnalyzer(
    private val detectionManager: DetectionManager
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = CameraUtils.imageProxyToBitmap(imageProxy)

            if (bitmap != null) {
                val time = measureTimeMillis {
                    detectionManager.processFrame(bitmap)
                }
                println("⏱ Frame processed in $time ms")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
}
