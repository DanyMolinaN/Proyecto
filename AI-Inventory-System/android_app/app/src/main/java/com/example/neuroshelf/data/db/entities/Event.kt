package com.example.neuroshelf.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String?,
    val productId: String?,
    val action: String, // "TAKE", "PUT"
    val timestamp: Long,
    val cameraId: String?,
    val suspicionScore: Float
)
