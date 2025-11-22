package com.example.neuroshelf.camara

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// 🔹 Asegurarse que DetectionResult coincide con el Detector
data class DetectionResult(
    val bbox: FloatArray, // [left, top, right, bottom]
    val score: Float,
    val classId: Int
)


// 🔹 Clase de seguimiento (Track)
data class Track(
    val id: Int,
    var bbox: FloatArray,        // Bounding box actualizada
    var classId: Int,            // Clase detectada (persona, producto, etc.)
    var score: Float,            // Confianza del objeto detectado
    var lastSeen: Long           // Última vez que fue detectado
)

class Tracker {

    private val tracks = mutableListOf<Track>()
    private var nextId = 1
    private val maxLostMs = 1500L   // ⏳ Tiempo máximo antes de eliminar un track (1.5s)
    private val maxDistance = 0.25  // 🎯 Umbral de distancia para asociar detecciones a tracks

    fun update(detections: List<DetectionResult>): List<Track> {
        val now = System.currentTimeMillis()

        // Si no hay tracks previos, asignar IDs a todas las detecciones nuevas
        if (tracks.isEmpty()) {
            detections.forEach { det ->
                tracks.add(
                    Track(
                        id = nextId++,
                        bbox = det.bbox,
                        classId = det.classId,
                        score = det.score,
                        lastSeen = now
                    )
                )
            }
            return tracks.toList()
        }

        // Asignar detecciones a tracks existentes
        val usedDetections = mutableSetOf<Int>() // Evitar asignar detecciones duplicadas

        for (track in tracks) {
            var bestDetIndex = -1
            var bestDist = Double.MAX_VALUE

            for ((index, det) in detections.withIndex()) {
                if (index in usedDetections) continue

                val distance = centroidDistance(track.bbox, det.bbox)
                if (distance < bestDist) {
                    bestDist = distance
                    bestDetIndex = index
                }
            }

            if (bestDetIndex != -1 && bestDist < maxDistance) {
                val bestDet = detections[bestDetIndex]
                track.bbox = bestDet.bbox
                track.score = bestDet.score
                track.lastSeen = now
                track.classId = bestDet.classId
                usedDetections.add(bestDetIndex)
            }
        }

        // Crear nuevos tracks para detecciones no asignadas
        for ((index, det) in detections.withIndex()) {
            if (index !in usedDetections) {
                tracks.add(
                    Track(
                        id = nextId++,
                        bbox = det.bbox,
                        classId = det.classId,
                        score = det.score,
                        lastSeen = now
                    )
                )
            }
        }

        // Eliminar tracks que han estado "perdidos" más allá del límite
        tracks.removeIf { now - it.lastSeen > maxLostMs }

        return tracks.toList()
    }

    // 📌 Distancia entre centroides (para asociar detecciones a tracks)
    private fun centroidDistance(box1: FloatArray, box2: FloatArray): Double {
        val cx1 = (box1[0] + box1[2]) / 2f
        val cy1 = (box1[1] + box1[3]) / 2f
        val cx2 = (box2[0] + box2[2]) / 2f
        val cy2 = (box2[1] + box2[3]) / 2f
        return hypot((cx1 - cx2), (cy1 - cy2)).toDouble()
    }
}

