package com.example.neuroshelf20.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import kotlin.math.sqrt

class FaceRecognition(context: Context) {

    private val repo = LocalEmbeddingsRepository(context)

    // Cargar embeddings desde JSON
    private val embeddings: Map<String, FloatArray> = repo.loadEmbeddings()

    // Cargar modelo TorchScript desde assets
    private val module: Module = Module.load(assetFilePath(context, "facenet_mobile.pt"))

    init {
        Log.d("FACE_RECOG", "Embeddings cargados: ${embeddings.size}")
    }

    // Preprocesamiento: 160x160, normalización [-1,1]
    private fun preprocess(bitmap: Bitmap): Tensor {
        val size = 160
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)

        val floatArray = FloatArray(1 * 3 * size * size)
        var idx = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = scaled.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF) - 127.5f) / 128.0f
                val g = ((pixel shr 8 and 0xFF) - 127.5f) / 128.0f
                val b = ((pixel and 0xFF) - 127.5f) / 128.0f
                floatArray[idx++] = r
                floatArray[idx++] = g
                floatArray[idx++] = b
            }
        }
        return Tensor.fromBlob(floatArray, longArrayOf(1L, 3L, size.toLong(), size.toLong()))
    }

    // Extraer embedding (512D)
    fun extractSignature(bitmap: Bitmap): FloatArray {
        val input = preprocess(bitmap)
        val output = module.forward(IValue.from(input)).toTensor()
        val emb = output.dataAsFloatArray
        Log.d("FACE_RECOG", "🧬 Signature generada (len=${emb.size}) sample: ${emb.take(8).joinToString(", ") { "%.4f".format(it) }}")
        return emb
    }

    // Distancia euclidiana
    private fun euclidean(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) sum += (a[i] - b[i]) * (a[i] - b[i])
        return sqrt(sum)
    }

    // Matching contra embeddings del JSON
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

        // Ajusta el umbral según tus pruebas (ej. 1.2f o 1.3f)
        return if (bestDist < 1.2f) bestId else null
    }
}
