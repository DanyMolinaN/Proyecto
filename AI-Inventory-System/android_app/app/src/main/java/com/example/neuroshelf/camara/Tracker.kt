package com.example.neuroshelf.camara

import kotlin.math.abs
import kotlin.math.hypot

data class Track(val id: Int, var bbox: FloatArray, var lastSeen: Long)

class Tracker {
    private val tracks = mutableListOf<Track>()
    private var nextId = 1
    private val maxLostMs = 1500L // si no se ve en 1.5s, se considera perdido

    fun update(detections: List<DetectionResult>): List<Track> {
        val now = System.currentTimeMillis()
        if (tracks.isEmpty()) {
            detections.forEach {
                tracks.add(Track(nextId++, it.bbox, now))
            }
            return tracks.toList()
        }

        // Simple greedy assoc by centroid distance
        val assigned = mutableSetOf<Int>()
        for (det in detections) {
            val cx = (det.bbox[0] + det.bbox[2]) / 2
            val cy = (det.bbox[1] + det.bbox[3]) / 2
            // find nearest track
            var bestTrack: Track? = null
            var bestDist = Double.MAX_VALUE
            for (track in tracks) {
                val tcx = (track.bbox[0] + track.bbox[2]) / 2
                val tcy = (track.bbox[1] + track.bbox[3]) / 2
                val dist = hypot((cx - tcx).toDouble(), (cy - tcy).toDouble())
                if (dist < bestDist) {
                    bestDist = dist
                    bestTrack = track
                }
            }
            // threshold (normalized coords) -> adjust threshold depending on resolution
            if (bestTrack != null && bestDist < 0.2) {
                bestTrack.bbox = det.bbox
                bestTrack.lastSeen = now
                assigned.add(bestTrack.id)
            } else {
                tracks.add(Track(nextId++, det.bbox, now))
            }
        }

        // remove old tracks
        tracks.removeIf { now - it.lastSeen > maxLostMs }
        return tracks.toList()
    }
}
