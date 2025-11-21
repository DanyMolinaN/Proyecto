package com.example.neuroshelf.domain

import android.content.Context
import android.graphics.Bitmap
import com.example.neuroshelf.camara.Detector
import com.example.neuroshelf.camara.Tracker
import com.example.neuroshelf.data.db.entities.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetectionManager(private val context: Context) {
    private val detector = Detector(context, "yolov8n_int8.tflite")
    private val tracker = Tracker()
    private val faceRecognition = FaceRecognition(context, "facenet_int8.tflite")

    // repository injection recommended
    fun processFrame(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.Default).launch {
            val detections = detector.detect(bitmap)
            val tracks = tracker.update(detections)

            // For each detection, if class corresponds to product, check person proximity etc.
            // For demo: detect faces in ROI and compute embedding
            // NOTE: implement face detection (SSD) to get face boxes -> crop -> embedding
            // This example assumes face detection returns a faceBitmap.

            // Pseudocode: detect faces
            // val faces = ssdFaceDetector.detect(bitmap)
            // for face in faces: emb = faceRecognition.getEmbedding(faceBitmap)
            // compare with DB embeddings -> get employeeId

            // Event creation example:
            // val event = Event(employeeId = foundEmployee, productId = productId, action="TAKE", timestamp=System.currentTimeMillis(), cameraId="CAM01", suspicionScore=0.1f)
            // save to DB via repository
        }
    }
}
