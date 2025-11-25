package com.example.neuroshelf20.camera.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.neuroshelf20.camera.utils.toBitmap
import com.example.neuroshelf20.domain.ObjectDetectionManager

class ObjectAnalyzer(
    private val manager: ObjectDetectionManager
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val bmp = image.toBitmap()
        if (bmp != null) manager.processFrame(bmp)
        image.close()
    }
}
