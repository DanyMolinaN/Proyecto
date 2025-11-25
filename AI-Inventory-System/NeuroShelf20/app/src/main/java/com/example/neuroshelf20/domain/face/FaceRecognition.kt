package com.example.neuroshelf20.domain.face


import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import kotlin.math.sqrt

class FaceRecognition(context: Context, modelPath: String = "facenet_int8.tflite") {
    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val tensorImage = TensorImage.fromBitmap(bitmap)
        val output = Array(1) { FloatArray(128) }
        interpreter.run(tensorImage.buffer, output)
        return l2Normalize(output[0])
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        var sum = 0f
        for (v in vec) sum += v * v
        val norm = sqrt(sum.toDouble()).toFloat().coerceAtLeast(1e-6f)
        for (i in vec.indices) vec[i] = vec[i] / norm
        return vec
    }

    companion object {
        fun cosineDistance(a: FloatArray, b: FloatArray): Float {
            var dot = 0f; var na = 0f; var nb = 0f
            for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
            return 1f - (dot / (kotlin.math.sqrt(na.toDouble()) * kotlin.math.sqrt(nb.toDouble()))).toFloat()
        }
    }
}
