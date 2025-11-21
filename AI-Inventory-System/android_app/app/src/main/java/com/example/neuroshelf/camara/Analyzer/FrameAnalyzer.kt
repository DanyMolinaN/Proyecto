package com.example.neuroshelf.camara.Analyzer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.neuroshelf.domain.DetectionManager
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class FrameAnalyzer(private val detectionManager: DetectionManager) : ImageAnalysis.Analyzer {


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // Convert ImageProxy to Bitmap (helper)
        val bmp = CameraUtils.imageProxyToBitmap(imageProxy)
        if (bmp != null) {
            val t = measureTimeMillis {
                detectionManager.processFrame(bmp)
            }
        }
        imageProxy.close()
    }
}
