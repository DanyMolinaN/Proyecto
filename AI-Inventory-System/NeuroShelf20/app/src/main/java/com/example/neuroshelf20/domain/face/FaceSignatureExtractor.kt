package com.example.neuroshelf20.domain.face

import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.sqrt

object FaceSignatureExtractor {

    fun extract(face: Face): FloatArray {

        val landmarks = face.allLandmarks
        if (landmarks.isEmpty()) return FloatArray(0)

        fun get(id: Int) = landmarks.firstOrNull { it.landmarkType == id }

        val leftEye = get(FaceLandmark.LEFT_EYE)
        val rightEye = get(FaceLandmark.RIGHT_EYE)
        val nose = get(FaceLandmark.NOSE_BASE)
        val mouthLeft = get(FaceLandmark.MOUTH_LEFT)
        val mouthRight = get(FaceLandmark.MOUTH_RIGHT)

        if (listOf(leftEye, rightEye, nose, mouthLeft, mouthRight).any { it == null })
            return FloatArray(0)

        fun dist(a: FaceLandmark?, b: FaceLandmark?): Float {
            if (a == null || b == null) return 0f
            val dx = a.position.x - b.position.x
            val dy = a.position.y - b.position.y
            return sqrt(dx * dx + dy * dy)
        }

        val eyeDist = dist(leftEye, rightEye)
        val eyeNoseLeft = dist(leftEye, nose)
        val eyeNoseRight = dist(rightEye, nose)
        val mouthWidth = dist(mouthLeft, mouthRight)
        val noseToMouth = dist(nose, mouthLeft) + dist(nose, mouthRight)

        return floatArrayOf(
            eyeDist,
            eyeNoseLeft,
            eyeNoseRight,
            mouthWidth,
            noseToMouth
        )
    }
}
