package com.example.neuroshelf.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.neuroshelf.camara.Detector
import com.example.neuroshelf.camara.DetectionResult
import com.example.neuroshelf.camara.Track
import com.example.neuroshelf.camara.Tracker
import com.example.neuroshelf.data.db.entities.Event
import com.example.neuroshelf.domain.event.EventRepository
import com.example.neuroshelf.domain.face.FaceDetector
import com.example.neuroshelf.domain.face.FaceRecognition
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
    private val faceDetector: FaceDetector by lazy { FaceDetector(context) }
    private val faceRecognition: FaceRecognition by lazy { FaceRecognition(context) }

    // Base de embeddings (DEMO)
    private val employeeDatabase = listOf(
        "EMP001" to FloatArray(128) { 0.1f },
        "EMP002" to FloatArray(128) { 0.2f }
    )

    fun processFrame(bitmap: Bitmap) {
        externalScope.launch(Dispatchers.Default) {

            val detections: List<DetectionResult> = detector.detect(bitmap)
            val tracks: List<Track> = tracker.update(detections)

            val personDetected = detections.any { it.classId == 0 } // clase 0 = persona

            val employeeId = if (personDetected) identifyEmployee(bitmap) else null

            if (employeeId != null) {
                saveEvents(tracks, employeeId)
            }
        }
    }

    private suspend fun identifyEmployee(bitmap: Bitmap): String? {
        val faces = faceDetector.detectFaces(bitmap)
        if (faces.isEmpty()) return null

        for (face in faces) {
            val emb = faceRecognition.getEmbedding(face)
            val match = faceRecognition.identifyPerson(emb, employeeDatabase)
            if (match != null) return match
        }
        return null
    }

    private suspend fun saveEvents(tracks: List<Track>, employeeId: String) {

        for (t in tracks) {
            if (t.classId == 1) { // 1 = producto
                val event = Event(
                    employeeId = employeeId,
                    productId = t.id.toString(),
                    action = "PICK_UP",
                    timestamp = System.currentTimeMillis(),
                    cameraId = "CAM01",
                    suspicionScore = 0.1f
                )
                eventRepository.saveEvent(event)
            }
        }
    }
}
