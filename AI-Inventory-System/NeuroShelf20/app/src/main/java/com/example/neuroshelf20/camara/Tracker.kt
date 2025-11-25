package com.example.neuroshelf20.camara

import kotlin.math.hypot
import kotlin.system.*

data class Track(
    val id: Int,
    var bbox: FloatArray,
    var classId: Int,
    var score: Float,
    var lastSeen: Long
)

class Tracker {
    private val tracks = mutableListOf<Track>()
    private var nextId = 1
    private val maxLostMs = 1500L
    private val maxDistance = 0.25

    fun update(detections: List<DetectionResult>): List<Track> {
        val now = System.currentTimeMillis()
        if (tracks.isEmpty()) {
            detections.forEach { d ->
                tracks.add(Track(nextId++, d.bbox, d.classId, d.score, now))
            }
            return tracks.toList()
        }

        val used = mutableSetOf<Int>()
        // Associate by centroid distance
        for (track in tracks) {
            var bestIdx = -1
            var bestDist = Double.MAX_VALUE
            for ((index, det) in detections.withIndex()) {
                if (index in used) continue
                val dist = centroidDistance(track.bbox, det.bbox)
                if (dist < bestDist) { bestDist = dist; bestIdx = index }
            }
            if (bestIdx != -1 && bestDist < maxDistance) {
                val det = detections[bestIdx]
                track.bbox = det.bbox
                track.score = det.score
                track.classId = det.classId
                track.lastSeen = now
                used.add(bestIdx)
            }
        }

        // create tracks for unused detections
        detections.forEachIndexed { idx, det ->
            if (idx !in used) {
                tracks.add(Track(nextId++, det.bbox, det.classId, det.score, now))
            }
        }

        tracks.removeIf { now - it.lastSeen > maxLostMs }
        return tracks.toList()
    }

    private fun centroidDistance(a: FloatArray, b: FloatArray): Double {
        val ax = (a[0] + a[2]) / 2f
        val ay = (a[1] + a[3]) / 2f
        val bx = (b[0] + b[2]) / 2f
        val by = (b[1] + b[3]) / 2f
        return hypot((ax - bx).toDouble(), (ay - by).toDouble())
    }
}
