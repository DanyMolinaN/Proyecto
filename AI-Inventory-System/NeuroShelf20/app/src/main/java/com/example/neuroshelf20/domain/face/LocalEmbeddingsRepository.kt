package com.example.neuroshelf20.domain.face

import android.content.Context
import android.util.Log
import org.json.JSONObject

class LocalEmbeddingsRepository(private val context: Context) {

    fun loadEmbeddings(): Map<String, FloatArray> {

        Log.d("EMBED", "🔍 Intentando cargar face_embeddings.json...")

        return try {

            val stream = context.assets.open("face_embeddings.json")

            Log.d("EMBED", "📁 Archivo encontrado en assets ✔️")

            val jsonText = stream.bufferedReader().use { it.readText() }

            Log.d("EMBED", "📄 Tamaño JSON: ${jsonText.length} caracteres")

            val obj = JSONObject(jsonText)
            val map = mutableMapOf<String, FloatArray>()

            obj.keys().forEach { key ->
                val arr = obj.getJSONArray(key)
                val floats = FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
                map[key] = floats
            }

            Log.d("EMBED", "✅ Embeddings cargados: ${map.size}")

            map

        } catch (e: Exception) {
            Log.e("EMBED", "❌ ERROR cargando embeddings: ${e.message}", e)
            emptyMap()
        }
    }
}
