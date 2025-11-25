package com.example.neuroshelf20.camera.analyzer

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.neuroshelf20.camera.utils.CameraUtils
import com.example.neuroshelf20.domain.DetectionManager

import android.graphics.Bitmap

class FrameAnalyzer(private val detectionManager: DetectionManager) : ImageAnalysis.Analyzer {

    @ExperimentalGetImage
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val bmp: Bitmap? = CameraUtils.imageProxyToBitmap(imageProxy)
            bmp?.let {
                detectionManager.processFrame(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
    }
}