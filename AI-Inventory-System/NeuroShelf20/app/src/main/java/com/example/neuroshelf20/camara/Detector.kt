package com.example.neuroshelf20.camara

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DetectionResult(val bbox: FloatArray, val score: Float, val classId: Int)

class Detector(private val context: Context, private val modelName: String = "yolov8n_int8.tflite") {

    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(model)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        // Preprocess: resize to model input (example 640x640) - adjust as needed
        val inputSize = 640
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(scaled)

        // adjust any output shape according to your model
        // Example output structure: [1,8400,6]
        val output = Array(1) { Array(8400) { FloatArray(6) } }

        interpreter.run(inputBuffer, output)

        val results = mutableListOf<DetectionResult>()
        val confThreshold = 0.3f

        val rows = output[0].size
        for (i in 0 until rows) {
            val row = output[0][i]
            val score = row[4]
            if (score > confThreshold) {
                val classId = row[5].toInt()
                val cx = row[0]
                val cy = row[1]
                val w = row[2]
                val h = row[3]
                val left = cx - w / 2
                val top = cy - h / 2
                val right = cx + w / 2
                val bottom = cy + h / 2
                results.add(DetectionResult(floatArrayOf(left, top, right, bottom), score, classId))
            }
        }
        return nonMaxSuppression(results, 0.45f)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 640
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val v = intValues[pixel++]
                byteBuffer.put(((v shr 16) and 0xFF).toByte())
                byteBuffer.put(((v shr 8) and 0xFF).toByte())
                byteBuffer.put((v and 0xFF).toByte())
            }
        }
        byteBuffer.rewind()
        return byteBuffer
    }

    private fun nonMaxSuppression(boxes: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        if (boxes.isEmpty()) return emptyList()
        val sorted = boxes.sortedByDescending { it.score }.toMutableList()
        val selected = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            selected.add(current)
            val iter = sorted.iterator()
            while (iter.hasNext()) {
                val other = iter.next()
                if (iou(current.bbox, other.bbox) > iouThreshold) iter.remove()
            }
        }
        return selected
    }

    private fun iou(a: FloatArray, b: FloatArray): Float {
        val x1 = maxOf(a[0], b[0])
        val y1 = maxOf(a[1], b[1])
        val x2 = minOf(a[2], b[2])
        val y2 = minOf(a[3], b[3])
        val inter = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val areaA = maxOf(0f, a[2] - a[0]) * maxOf(0f, a[3] - a[1])
        val areaB = maxOf(0f, b[2] - b[0]) * maxOf(0f, b[3] - b[1])
        val union = areaA + areaB - inter
        return if (union <= 0f) 0f else inter / union
    }
}