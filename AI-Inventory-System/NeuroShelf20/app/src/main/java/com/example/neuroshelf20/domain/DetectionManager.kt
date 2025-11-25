package com.example.neuroshelf20.domain

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.neuroshelf20.domain.face.FaceDetector
import com.example.neuroshelf20.domain.face.FaceRecognition
import com.example.neuroshelf20.data.repository.EventRepositoryFirebaseImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import  com.example.neuroshelf20.data.model.EventModel

class DetectionManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val eventRepository: EventRepositoryFirebaseImpl
) {

    private val faceDetector = FaceDetector(context)
    private val faceRecognition = FaceRecognition(context)
    private val alertManager = AlertManager(context)

    fun processFrame(bitmap: Bitmap) {
        externalScope.launch(Dispatchers.Default) {

            val faces = faceDetector.detectFaces(bitmap)

            if (faces.isEmpty()) {
                Log.d("DETECT", "No se detectaron rostros")
                return@launch
            }

            for (face in faces) {

                val box = face.boundingBox
                val cropped = faceDetector.cropFace(bitmap, box)

                if (cropped == null) {
                    Log.e("DETECT", "No se pudo recortar rostro")
                    continue
                }

                // Convertir imagen en firma (embedding simple)
                val signature = faceRecognition.extractSignature(cropped)

                // Buscar persona en el JSON
                val personId = faceRecognition.matchSignature(signature)

                if (personId != null) {

                    alertManager.sendLocalAlert(
                        "Identificado",
                        "Empleado: $personId"
                    )

                    val event = EventModel(
                        employeeId = personId,
                        productId = null,
                        action = "face_recognized",
                        timestamp = System.currentTimeMillis(),
                        cameraId = "CAMERA01",
                        suspicionScore = 0.0
                    )

                    eventRepository.saveEvent(event)

                } else {
                    alertManager.sendLocalAlert(
                        "Alerta",
                        "Persona NO registrada"
                    )
                }
            }
        }
    }
}
