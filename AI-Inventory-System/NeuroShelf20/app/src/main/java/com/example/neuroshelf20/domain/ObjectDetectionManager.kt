package com.example.neuroshelf20.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.neuroshelf20.domain.yolo.YoloV8Interpreter

class ObjectDetectionManager(
    context: Context
) {
    private val yolo = YoloV8Interpreter(context)

    fun processFrame(bmp: Bitmap) {
        val res = yolo.detect(bmp)
        if (res.isEmpty()) {
            Log.d("YOLO", "Nada detectado")
        } else {
            res.forEach {
                Log.d("YOLO", "Detectado ${it.label} conf=${it.confidence}")
            }
        }
    }
}
