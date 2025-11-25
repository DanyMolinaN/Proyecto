package com.example.neuroshelf20.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.neuroshelf20.camara.Detector
import com.example.neuroshelf20.camara.Tracker
import com.example.neuroshelf20.data.model.EventModel
import com.example.neuroshelf20.data.repository.EmployeeRepositoryFirebase
import com.example.neuroshelf20.domain.face.FaceDetector
import com.example.neuroshelf20.domain.face.FaceRecognition
import com.example.neuroshelf20.domain.event.EventRepository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class DetectionManager(
    private val context: Context,
    private val externalScope: CoroutineScope,
    private val eventRepository: EventRepository,
    private val employeeRepository: EmployeeRepositoryFirebase
) {

    private val detector by lazy { Detector(context) }
    private val tracker by lazy { Tracker() }
    private val faceDetector by lazy { FaceDetector(context) }
    private val faceRecognition by lazy { FaceRecognition(context) }
    private val alertManager by lazy { AlertManager(context) }
    private val PRODUCT_CLASSES = listOf(39, 41, 75, 76)
    fun processFrame(bitmap: Bitmap) {
        externalScope.launch {
            try {
                val detections = detector.detect(bitmap)
                val tracks = tracker.update(detections)

                // If there's a person class (classId 0 usualy), try find face
                val hasPerson = detections.any { it.classId == 0 } // adjust classId mapping as required
                var employeeId: String? = null
                if (hasPerson) {
                    val faces = faceDetector.detectFaces(bitmap)
                    val embeddingsDB = employeeRepository.getAllEmbeddings()
                    for (faceBmp in faces) {
                        val emb = faceRecognition.getEmbedding(faceBmp)
                        val match = embeddingsDB.minByOrNull { (_, dbVec) ->
                            FaceRecognition.cosineDistance(emb, dbVec)
                        }
                        if (match != null && FaceRecognition.cosineDistance(emb, match.second) < 0.45f) {
                            employeeId = match.first
                            break
                        }
                    }
                }

                handleEvents(tracks, employeeId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private suspend fun handleEvents(tracks: List<com.example.neuroshelf20.camara.Track>, employeeId: String?) {
        if (employeeId == null) {
            // optional: send alert only if suspicious removal occurs
            alertManager.sendLocalAlert("Persona no identificada", "Interacción detectada sin identificación")
        }
        for (t in tracks) {
            // if a product class - adapt classId mapping
            if (t.classId != 0) {
                val event = EventModel(
                    id = "",
                    employeeId = employeeId,
                    productId = t.id.toString(),
                    action = "PICK_UP",
                    timestamp = System.currentTimeMillis(),
                    cameraId = "CAM01",
                    suspicionScore = 0.1
                )
                eventRepository.saveEvent(event)
            }
        }
    }
}
