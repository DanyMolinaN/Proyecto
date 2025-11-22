package com.example.neuroshelf.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import java.nio.ByteBuffer

class FaceDetector(
    private val context: Context,
    modelPath: String = "ssd_face_detector.tflite" // Asegúrate del nombre correcto
) {

    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    /**
     * Detecta rostros y devuelve las áreas recortadas como bitmaps válidos.
     */
    fun detectFaces(bitmap: Bitmap): List<Bitmap> {
        val faceBitmaps = mutableListOf<Bitmap>()

        // Obtener bounding boxes (detecciones)
        val detectedBoxes = runDetection(bitmap)

        // Recortar cada rostro detectado
        for (rect in detectedBoxes) {
            cropFace(bitmap, rect)?.let { faceBitmaps.add(it) }
        }

        return faceBitmaps
    }

    /**
     * Procesa la imagen con el modelo SSD para obtener bounding boxes.
     * ⚠️ Aquí se debe adaptar según la salida real del modelo entrenado.
     */
    private fun runDetection(bitmap: Bitmap): List<RectF> {
        val inputImage = TensorImage.fromBitmap(bitmap)
        val inputBuffer: ByteBuffer = inputImage.buffer

        // Suponiendo que el modelo tiene salida [1, 10, 4] (10 boxes x 4 coords)
        val outputBoxes = Array(1) { Array(10) { FloatArray(4) } }

        interpreter.run(inputBuffer, outputBoxes)

        val results = mutableListOf<RectF>()

        for (box in outputBoxes[0]) {
            val left = (box[1] * bitmap.width).coerceIn(0f, bitmap.width.toFloat())
            val top = (box[0] * bitmap.height).coerceIn(0f, bitmap.height.toFloat())
            val right = (box[3] * bitmap.width).coerceIn(0f, bitmap.width.toFloat())
            val bottom = (box[2] * bitmap.height).coerceIn(0f, bitmap.height.toFloat())

            if (right > left && bottom > top) {
                results.add(RectF(left, top, right, bottom))
            }
        }

        return results
    }

    /**
     * Recorte seguro de rostros
     */
    private fun cropFace(bitmap: Bitmap, rect: RectF): Bitmap? {
        return try {
            Bitmap.createBitmap(
                bitmap,
                rect.left.toInt().coerceIn(0, bitmap.width),
                rect.top.toInt().coerceIn(0, bitmap.height),
                rect.width().toInt().coerceAtLeast(1).coerceAtMost(bitmap.width),
                rect.height().toInt().coerceAtLeast(1).coerceAtMost(bitmap.height)
            )
        } catch (e: Exception) {
            null
        }
    }
}
