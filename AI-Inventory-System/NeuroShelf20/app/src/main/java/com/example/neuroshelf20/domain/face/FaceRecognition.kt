package com.example.neuroshelf20.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlin.math.sqrt

class FaceRecognition(context: Context) {

    private val repo = LocalEmbeddingsRepository(context)

    // Cargar embeddings SOLO UNA VEZ
    private val embeddings: Map<String, FloatArray> = repo.loadEmbeddings()

    init {
        Log.d("FACE_RECOG", "Embeddings cargados: ${embeddings.size}")
    }

    fun extractSignature(bmp: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bmp, 32, 32, true)
        val sig = FloatArray(64)

        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val p = resized.getPixel(x, y)
                val r = (p shr 16 and 0xFF) / 64
                val g = (p shr 8 and 0xFF) / 64
                val b = (p and 0xFF) / 64
                sig[r * 16 + g * 4 + b]++
            }
        }
        return sig
    }

    private fun euclidean(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += (a[i] - b[i]) * (a[i] - b[i])
        return sqrt(sum)
    }

    fun matchSignature(sig: FloatArray): String? {

        if (embeddings.isEmpty()) {
            Log.e("MATCH", "❌ Embeddings no cargados")
            return null
        }

        var bestId: String? = null
        var bestDist = Float.MAX_VALUE

        for ((id, ref) in embeddings) {
            val d = euclidean(sig, ref)
            Log.d("MATCH", "$id → distancia = $d")

            if (d < bestDist) {
                bestDist = d
                bestId = id
            }
        }

        Log.d("MATCH", "Mejor match: $bestId ($bestDist)")

        return if (bestDist < 25f) bestId else null
    }
}
