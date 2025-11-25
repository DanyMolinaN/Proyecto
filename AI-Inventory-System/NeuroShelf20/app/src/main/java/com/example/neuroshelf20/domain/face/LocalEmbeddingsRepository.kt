package com.example.neuroshelf20.domain.face

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONObject

class LocalEmbeddingsRepository(private val context: Context) {

    fun loadEmbeddings(): Map<String, FloatArray> {
        val map = mutableMapOf<String, FloatArray>()

        try {
            // Intento de leer el archivo
            val jsonText = context.assets.open("face_embeddings.json")
                .bufferedReader()
                .use { it.readText() }

            Toast.makeText(context, "JSON cargado correctamente ✔", Toast.LENGTH_SHORT).show()

            val json = JSONObject(jsonText)

            for (key in json.keys()) {
                val arr = json.getJSONArray(key)
                val floats = FloatArray(arr.length())

                for (i in 0 until arr.length()) {
                    floats[i] = arr.getDouble(i).toFloat()
                }

                map[key] = floats
            }

            Log.d("EMBEDS", "Embeddings cargados: ${map.size}")
            Toast.makeText(context, "Embeddings: ${map.size}", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("EMBEDS", "Error leyendo JSON", e)
            Toast.makeText(context, "ERROR cargando embeddings ❌", Toast.LENGTH_LONG).show()
        }

        return map
    }
}
