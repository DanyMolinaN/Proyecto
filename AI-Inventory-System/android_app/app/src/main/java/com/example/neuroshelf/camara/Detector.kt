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
    val bbox: FloatArray, // [x,y,w,h] normalized 0..1 (x center, y center, w, h) OR [left,top,right,bottom] based on model
    val score: Float,
    val classId: Int
)

class Detector(private val context: Context, modelPath: String = "yolov8n_int8.tflite") {
    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context, modelPath)
        val options = Interpreter.Options()
        // options.addDelegate(...) // GPU delegate optional
        interpreter = Interpreter(model, options)
    }

    private fun loadModelFile(ctx: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = ctx.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Example: model expects input 640x640 uint8/int8 - adapt to your exported model dims
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val inputSize = 640
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = convertBitmapToByteBuffer(scaled)

        // Outputs depend on exported model - if using YOLOv8 -> output may be Nx(6) or custom array.
        // For portability, assume output shape [1, 25200, 6] -> [x,y,w,h,score,class]
        val outputShape = arrayOf(Array(25200) { FloatArray(6) }) // adjust accordingly
        val outputMap = mutableMapOf<Int, Any>()
        outputMap[0] = outputShape

        interpreter.run(inputBuffer, outputShape)

        // Postprocess: NMS, thresholding
        val results = mutableListOf<DetectionResult>()
        val confThreshold = 0.3f
        for (i in outputShape[0].indices) {
            val row = outputShape[0][i]
            val score = row[4]
            if (score > confThreshold) {
                val classId = row[5].toInt()
                val cx = row[0]
                val cy = row[1]
                val w = row[2]
                val h = row[3]
                // convert center x,y,w,h to left,top,right,bottom normalized
                val left = cx - w / 2
                val top = cy - h / 2
                val right = cx + w / 2
                val bottom = cy + h / 2
                results.add(DetectionResult(floatArrayOf(left, top, right, bottom), score, classId))
            }
        }
        // TODO: apply NMS (implement simple IoU based NMS)
        return nonMaxSuppression(results, 0.45f)
    }

    private fun nonMaxSuppression(boxes: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        // Sort boxes by score in descending order
        val sortedBoxes = boxes.sortedByDescending { it.score }
        val out = mutableListOf<DetectionResult>()

        val selected = BooleanArray(boxes.size) { false }

        for (i in sortedBoxes.indices) {
            if (selected[i]) continue

            val currentBox = sortedBoxes[i]
            out.add(currentBox)
            selected[i] = true

            for (j in (i + 1) until sortedBoxes.size) {
                if (selected[j]) continue

                val nextBox = sortedBoxes[j]
                val iouValue = iou(currentBox.bbox, nextBox.bbox)
                if (iouValue > iouThreshold) {
                    selected[j] = true
                }
            }
        }
        return out
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 640
        val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3)
        imgData.order(ByteOrder.nativeOrder())
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val v = intValues[pixel++]
                // normalize to 0..255 or -128..127 depending on quantization. For int8 quantized models,
                // use appropriate dequantization if needed. This code writes raw bytes 0..255.
                imgData.put(((v shr 16) and 0xFF).toByte())
                imgData.put(((v shr 8) and 0xFF).toByte())
                imgData.put((v and 0xFF).toByte())
            }
        }
        imgData.rewind()
        return imgData
    }

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
}

