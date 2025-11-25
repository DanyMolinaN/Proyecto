package com.example.neuroshelf20.domain.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlin.math.sqrt

class FaceRecognition(context: Context) {

    private val repo = LocalEmbeddingsRepository(context)

    /**
     * Genera una “firma” (embedding simple) basada en histogramas de color.
     * 64 valores = 4×4×4 bins por canal RGB.
     */
    fun extractSignature(bmp: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(bmp, 32, 32, true)
        val hist = FloatArray(64)

        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val pixel = resized.getPixel(x, y)

                val r = (pixel shr 16 and 0xFF) / 64   // 4 bins → 0..3
                val g = (pixel shr 8 and 0xFF) / 64
                val b = (pixel and 0xFF) / 64

                val index = r * 16 + g * 4 + b
                hist[index] += 1f
            }
        }

        return hist
    }

    private fun euclidean(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sqrt(sum)
    }

    /**
     * Compara el embedding obtenido con el JSON ya cargado
     * Retorna el nombre de la persona si coincide
     */
    fun matchSignature(signature: FloatArray): String? {

        val db = repo.loadEmbeddings()
        if (db.isEmpty()) {
            Log.e("NEURO_MATCH", "⚠️ No hay embeddings cargados")
            return null
        }

        Log.d("NEURO_MATCH", "🧬 Buscando coincidencia en ${db.size} personas")

        var bestId: String? = null
        var bestDist = Float.MAX_VALUE

        for ((name, ref) in db) {
            if (ref.size != signature.size) {
                Log.e("NEURO_MATCH", "❌ Tamaño inválido en $name: ref=${ref.size}, sig=${signature.size}")
                continue
            }

            val dist = euclidean(signature, ref)

            Log.d("NEURO_MATCH", "➡️ $name distancia=$dist")

            if (dist < bestDist) {
                bestDist = dist
                bestId = name
            }
        }

        Log.d("NEURO_MATCH", "🏁 Mejor match: $bestId con distancia=$bestDist")

        return if (bestDist < 25f) bestId else null
    }
}
