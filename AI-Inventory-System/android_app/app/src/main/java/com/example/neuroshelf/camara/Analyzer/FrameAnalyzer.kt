package com.example.neuroshelf.camara.Analyzer

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.neuroshelf.camara.utils.CameraUtils
import com.example.neuroshelf.domain.DetectionManager
import kotlin.system.measureTimeMillis

class FrameAnalyzer(
    private val detectionManager: DetectionManager
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bitmap = CameraUtils.imageProxyToBitmap(imageProxy)

            bitmap?.let {
                val processingTime = measureTimeMillis {
                    detectionManager.processFrame(it)
                }
                println("⏱ Frame processed in $processingTime ms")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
}
