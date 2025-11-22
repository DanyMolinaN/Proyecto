package com.example.neuroshelf.domain.face

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import kotlin.math.min

class FaceRecognition(
    context: Context,
    modelPath: String = "facenet_int8.tflite"
) {

    private val interpreter: Interpreter

    init {
        val model = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(model)
    }

    /**
     * Genera el embedding (vector de características faciales).
     * El modelo FaceNet/MobileFaceNet normalmente devuelve un vector de 128 dimensiones.
     */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        val tensorImage = TensorImage.fromBitmap(faceBitmap)

        val output = Array(1) { FloatArray(128) }
        interpreter.run(tensorImage.buffer, output)

        return l2Normalize(output[0]) // Normalizamos el embedding
    }

    /**
     * Identifica a la persona comparando embeddings con la base de datos local.
     * @return ID del empleado si hay coincidencia, o null si no existe.
     */
    fun identifyPerson(embedding: FloatArray, database: List<Pair<String, FloatArray>>): String? {
        var bestMatch: String? = null
        var lowestDistance = Float.MAX_VALUE

        for ((id, savedEmbedding) in database) {
            val distance = cosineDistance(embedding, savedEmbedding)
            if (distance < lowestDistance) {
                lowestDistance = distance
                bestMatch = id
            }
        }

        // Umbral recomendado entre 0.35 y 0.5 (depende del modelo y ambiente)
        return if (lowestDistance < 0.45f) bestMatch else null
    }

    /**
     * Distancia de similitud coseno, más precisa para embeddings L2 normalizados.
     */
    private fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return 1 - (dot / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble()))).toFloat()
    }

    /**
     * 🔹 Normalización L2: importante para comparar embeddings correctamente
     */
    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) {
            sum += v * v
        }
        val norm = Math.sqrt(sum.toDouble()).toFloat()

        for (i in embedding.indices) {
            embedding[i] /= norm
        }
        return embedding
    }
}
