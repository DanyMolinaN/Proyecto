package com.example.neuroshelf20.data.model

data class EventModel(
    val id: String = "",
    val employeeId: String? = null,
    val productId: String? = null,
    val action: String = "",
    val timestamp: Long = 0L,
    val cameraId: String = "",
    val suspicionScore: Double = 0.0
)
