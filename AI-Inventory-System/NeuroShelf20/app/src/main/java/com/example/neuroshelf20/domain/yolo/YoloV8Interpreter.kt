package com.example.neuroshelf20.domain.yolo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YoloV8Interpreter(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = 640

    init {
        val model = context.assets.open("yolov8n.tflite").readBytes()
        val buffer = ByteBuffer.allocateDirect(model.size)
        buffer.order(ByteOrder.nativeOrder())
        buffer.put(model)
        interpreter = Interpreter(buffer)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        input.order(ByteOrder.nativeOrder())

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val px = resized.getPixel(x, y)
                input.putFloat(((px shr 16 and 0xFF) / 255f))
                input.putFloat(((px shr 8 and 0xFF) / 255f))
                input.putFloat(((px and 0xFF) / 255f))
            }
        }

        val output = Array(1) { Array(8400) { FloatArray(6) } }
        interpreter.run(input, output)

        val detections = mutableListOf<DetectionResult>()

        for (i in 0 until 8400) {
            val conf = output[0][i][4]
            if (conf > 0.50f) {
                val x = output[0][i][0]
                val y = output[0][i][1]
                val w = output[0][i][2]
                val h = output[0][i][3]

                detections.add(
                    DetectionResult(
                        "obj",
                        conf,
                        RectF(
                            x - w / 2,
                            y - h / 2,
                            x + w / 2,
                            y + h / 2
                        )
                    )
                )
            }
        }

        return detections
    }
}

data class DetectionResult(
    val label: String,
    val confidence: Float,
    val box: RectF
)
