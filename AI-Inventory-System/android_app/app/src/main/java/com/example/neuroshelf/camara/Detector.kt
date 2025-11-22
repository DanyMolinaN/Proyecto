package com.example.neuroshelf.camara

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

data class DetectionResult(
    val bbox: FloatArray, // [left, top, right, bottom] normalizado (0..1)
    val score: Float,
    val classId: Int
)

class Detector(private val context: Context, modelPath: String = "yolov8n_int8.tflite") {

    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context, modelPath)
        interpreter = Interpreter(model, Interpreter.Options())
    }

    private fun loadModelFile(ctx: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = ctx.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    // Procesar detección
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val inputWidth = 640
        val inputHeight = 640

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        // 📌 Ajustar con la salida real de tu modelo (YOLOv8 típico = [1, 8400, 6])
        val outputBuffer = Array(1) { Array(8400) { FloatArray(6) } }

        interpreter.run(inputBuffer, outputBuffer)

        val rawDetections = mutableListOf<DetectionResult>()
        val confThreshold = 0.3f

        for (i in outputBuffer[0].indices) {
            val row = outputBuffer[0][i]
            val score = row[4]

            if (score > confThreshold) {
                val classId = row[5].toInt()

                // YOLOv8: [cx, cy, w, h, score, class]
                val cx = row[0]
                val cy = row[1]
                val w = row[2]
                val h = row[3]

                // Convertir a formato [left, top, right, bottom]
                val left = cx - w / 2
                val top = cy - h / 2
                val right = cx + w / 2
                val bottom = cy + h / 2

                rawDetections.add(
                    DetectionResult(
                        floatArrayOf(left, top, right, bottom),
                        score,
                        classId
                    )
                )
            }
        }

        return nonMaxSuppression(rawDetections, 0.45f)
    }

    // 🔹 Non-Maximum Suppression (NMS)
    private fun nonMaxSuppression(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.score }
        val selected = mutableListOf<DetectionResult>()

        val active = BooleanArray(sorted.size) { true }

        for (i in sorted.indices) {
            if (!active[i]) continue

            val current = sorted[i]
            selected.add(current)

            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue

                val next = sorted[j]
                if (iou(current.bbox, next.bbox) > iouThreshold) {
                    active[j] = false
                }
            }
        }
        return selected
    }

    // 🔹 Intersection over Union (IoU)
    private fun iou(box1: FloatArray, box2: FloatArray): Float {
        val x1 = max(box1[0], box2[0])
        val y1 = max(box1[1], box2[1])
        val x2 = min(box1[2], box2[2])
        val y2 = min(box1[3], box2[3])

        val intersectionArea = max(0f, x2 - x1) * max(0f, y2 - y1)
        val box1Area = (box1[2] - box1[0]) * (box1[3] - box1[1])
        val box2Area = (box2[2] - box2[0]) * (box2[3] - box2[1])
        val unionArea = box1Area + box2Area - intersectionArea

        return if (unionArea <= 0f) 0f else intersectionArea / unionArea
    }

    // 🔹 Conversión Bitmap → ByteBuffer (RGB)
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputWidth = 640
        val inputHeight = 640
        val bytePerChannel = 1 // Int8

        val imgData = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * bytePerChannel)
        imgData.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in intValues) {
            imgData.put(((pixel shr 16) and 0xFF).toByte()) // R
            imgData.put(((pixel shr 8) and 0xFF).toByte())  // G
            imgData.put((pixel and 0xFF).toByte())          // B
        }

        imgData.rewind()
        return imgData
    }
}
