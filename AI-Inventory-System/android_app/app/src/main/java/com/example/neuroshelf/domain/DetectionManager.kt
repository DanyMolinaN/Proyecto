package com.example.neuroshelf.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.neuroshelf.camara.Detector
import com.example.neuroshelf.camara.Tracker
import com.example.neuroshelf.data.db.entities.Event
import com.example.neuroshelf.domain.event.EventRepository
import com.example.neuroshelf.domain.face.FaceRecognition
import com.example.neuroshelf.domain.face.FaceDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetectionManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val eventRepository: EventRepository
) {

    private val detector: Detector by lazy { Detector(context) }
    private val tracker: Tracker by lazy { Tracker() }
    private val faceRecognition: FaceRecognition by lazy { FaceRecognition(context) }
    private val faceDetector: FaceDetector by lazy { FaceDetector(context) }

    fun processFrame(bitmap: Bitmap) {
        externalScope.launch(Dispatchers.Default) {

            // 1. Detecciones + Tracks
            val detections = detector.detect(bitmap)
            val tracks = tracker.update(detections)

            // 2. Reconocimiento facial (si hay personas)
            val employeeId = identifyEmployeeIfPersonDetected(bitmap, detections)

            // 3. Crear eventos
            createAndSaveEvents(tracks, employeeId)
        }
    }

    private fun identifyEmployeeIfPersonDetected(
        bitmap: Bitmap,
        detections: List<Detector.Detection>
    ): String? {

        val foundPerson = detections.any { it.classId == 0 } // YOLO class 0 = person
        if (!foundPerson) return null

        val faces = faceDetector.detectFaces(bitmap)
        if (faces.isEmpty()) return null

        for (faceBmp in faces) {
            val emb = faceRecognition.getEmbedding(faceBmp)
            val id = faceRecognition.identifyPerson(emb)
            if (id != null) return id
        }
        return null
    }

    private suspend fun createAndSaveEvents(
        tracks: List<Tracker.Track>,
        employeeId: String?
    ) {
        if (employeeId == null) return

        for (track in tracks) {

            // ❗ IMPORTANTE: aquí decides cuándo es un producto
            // Necesitas un classId en el Track, o un mapa de IDs → categorías guardado en detector.
            // Por ahora asumimos que track.id >= 100 son productos.
            if (track.id >= 100) {
                val event = Event(
                    employeeId = employeeId,
                    productId = track.id.toString(),
                    action = "PICK_UP",
                    timestamp = System.currentTimeMillis(),
                    cameraId = "CAM01",
                    suspicionScore = 0.1f
                )

                eventRepository.saveEvent(event) // ✔️ ahora NO da error
            }
        }
    }
}
