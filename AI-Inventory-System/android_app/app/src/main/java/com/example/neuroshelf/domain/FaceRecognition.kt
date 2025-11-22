package com.example.neuroshelf.domain

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceRecognition(
    private val context: Context,
    modelPath: String = "facenet_int8.tflite"
) {

    private val interpreter: Interpreter
    private val inputSize = 112 // tamaño estándar para FaceNet / MobileFaceNet

    init {
        val model = loadModelFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    /** Carga el modelo *.tflite* desde assets */
    private fun loadModelFile(ctx: Context, filename: String): MappedByteBuffer {
        val assetFileDescriptor = ctx.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Convierte el rostro a embedding (vector de características) */
    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val buffer = convertBitmapToFloatBuffer(scaled)

        val output = Array(1) { FloatArray(128) } // embedding length según el modelo

        interpreter.run(buffer, output)

        return l2Normalize(output[0])
    }

    /** Convierte bitmap → FloatBuffer normalizado [-1..1] */
    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData =
            ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4) // float (4 bytes)
        imgData.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val v = intValues[pixel++]

                // Extraer canales RGB como enteros
                val r = (v shr 16) and 0xFF
                val g = (v shr 8) and 0xFF
                val b = v and 0xFF

                // Normalización [-1..1]
                imgData.putFloat((r - 127.5f) / 128f)
                imgData.putFloat((g - 127.5f) / 128f)
                imgData.putFloat((b - 127.5f) / 128f)
            }
        }

        imgData.rewind()
        return imgData
    }

    /** Normalización L2 del vector */
    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) sum += v * v
        val norm = sqrt(sum)

        val out = FloatArray(embedding.size)
        for (i in embedding.indices) out[i] = embedding[i] / norm

        return out
    }

    /** Compara dos embeddings usando similitud del coseno */
    fun compare(emb1: FloatArray, emb2: FloatArray): Float {
        var dot = 0f
        for (i in emb1.indices) {
            dot += emb1[i] * emb2[i]
        }
        return dot // entre -1 y 1; cuanto más alto, más similar
    }
}
