package com.example.neuroshelf20.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.neuroshelf20.domain.face.FaceDetector
import com.example.neuroshelf20.domain.face.FaceRecognition
import com.example.neuroshelf20.data.repository.EventRepositoryFirebaseImpl
import com.example.neuroshelf20.data.model.EventModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetectionManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val eventRepo: EventRepositoryFirebaseImpl
) {

    private val detector = FaceDetector(context)
    private val recognizer = FaceRecognition(context)
    private val alertManager = AlertManager(context)

    fun processFrame(bitmap: Bitmap) {
        externalScope.launch(Dispatchers.Default) {
            try {
                Log.d("NEURO_DETECT", "📸 Procesando frame...")

                // Detectar caras (FaceDetector.detectFaces() debe ser suspend si usa MLKit)
                val faces = try {
                    detector.detectFaces(bitmap)
                } catch (e: Exception) {
                    Log.e("NEURO_DETECT", "Error en detectFaces()", e)
                    emptyList()
                }

                Log.d("NEURO_DETECT", "🔍 Rostros detectados = ${faces.size}")
                if (faces.isEmpty()) return@launch

                for (face in faces) {
                    try {
                        val rect = face.boundingBox
                        Log.d("NEURO_DETECT", "🟥 Bounding box = $rect")

                        // Recorte (faceDetector.cropFace es segura y devuelve null si inválida)
                        val cropped = detector.cropFace(bitmap, rect)
                        if (cropped == null) {
                            Log.e("NEURO_DETECT", "❌ No se pudo recortar el rostro (rect inválido)")
                            continue
                        }
                        Log.d("NEURO_DETECT", "✂️ Rostro recortado: ${cropped.width}x${cropped.height}")

                        // Extraer firma / embedding
                        val signature = try {
                            recognizer.extractSignature(cropped)
                        } catch (e: Exception) {
                            Log.e("NEURO_DETECT", "Error al extraer signature", e)
                            continue
                        }

                        Log.d("NEURO_DETECT", "🧬 Signature generada (len=${signature.size}) sample: ${
                            signature.take(8).joinToString(separator = ", ") { "%.4f".format(it) }
                        }")

                        // Coincidencia contra DB local
                        val match = recognizer.matchSignature(signature)

                        if (match != null) {
                            Log.d("NEURO_DETECT", "🎉 MATCH ENCONTRADO → $match")
                            alertManager.sendLocalAlert("Identificado", "Empleado detectado: $match")

                            // Guardar evento (si saveEvent es suspend, asegúrate de usar await o implementarlo como susp)
                            try {
                                val event = EventModel(
                                    employeeId = match,
                                    productId = null,
                                    action = "FACE_DETECTED",
                                    timestamp = System.currentTimeMillis(),
                                    cameraId = "CAMERA01",
                                    suspicionScore = 0.0
                                )
                                // si saveEvent es suspend, eventRepo.saveEvent(event) puede ser llamado directamente aquí (estamos en coroutine)
                                eventRepo.saveEvent(event)
                                Log.d("NEURO_DETECT", "✅ Evento guardado para $match")
                            } catch (e: Exception) {
                                Log.e("NEURO_DETECT", "Error guardando evento", e)
                            }

                        } else {
                            Log.d("NEURO_DETECT", "⚠️ Persona desconocida")
                            alertManager.sendLocalAlert("Desconocido", "No coincide con la base")
                        }

                    } catch (inner: Exception) {
                        Log.e("NEURO_DETECT", "Error procesando un rostro", inner)
                    }
                }

            } catch (e: Exception) {
                Log.e("NEURO_DETECT", "Error general en processFrame()", e)
            }
        }
    }
}
